package com.ankideku.data.remote.llm

import com.ankideku.util.TokenEstimator
import com.ankideku.util.onIO
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.json.*
import java.io.BufferedReader
import java.io.BufferedWriter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Claude CLI Service
 * Implements LLM by spawning Claude Code CLI subprocess.
 */
class ClaudeCliService(
    private val timeoutMs: Long = 300_000,  // 5 minutes default
    private val conversationTimeoutMs: Long = 120_000,  // 2 minutes for conversation messages
    maxRetries: Int = 2,
) : BaseLlmService(maxRetries = maxRetries) {

    private val activeProcesses = ConcurrentHashMap<String, Process>()
    private val activeConversations = ConcurrentHashMap<String, ClaudeConversationHandle>()

    override suspend fun getHealth(): LlmHealthStatus = onIO {
        try {
            val isInstalled = isClaudeInstalled()
            if (!isInstalled) {
                return@onIO LlmHealthStatus(
                    available = false,
                    error = "Claude Code CLI is not installed or not in PATH"
                )
            }

            val version = getClaudeVersion()
            LlmHealthStatus(
                available = true,
                info = version?.let { "Claude Code $it installed" } ?: "Claude Code installed"
            )
        } catch (e: Exception) {
            LlmHealthStatus(
                available = false,
                error = "Failed to check Claude CLI: ${e.message}"
            )
        }
    }

    override suspend fun callLlm(prompt: String): String {
        val processId = "${System.currentTimeMillis()}-${(Math.random() * 1000000).toInt()}"

        try {
            val processBuilder = ProcessBuilder(
                "claude",
                "--print",
                "--model", "sonnet",
                "--output-format", "text"
            )
            processBuilder.redirectErrorStream(false)

            val process = processBuilder.start()
            activeProcesses[processId] = process

            // Write prompt to stdin
            process.outputStream.bufferedWriter().use { writer ->
                writer.write(prompt)
            }

            // Wait for process in a cancellation-aware manner
            val (stdout, stderr) = awaitProcess(process, processId)
            val exitCode = process.exitValue()

            if (exitCode != 0) {
                throw Exception(stderr.ifBlank { "Claude CLI exited with code $exitCode" })
            }

            return stdout
        } finally {
            activeProcesses.remove(processId)?.destroyForcibly()
        }
    }

    /**
     * Await process completion in a cancellation-aware manner.
     * If the coroutine is cancelled, the process will be destroyed.
     */
    private suspend fun awaitProcess(process: Process, processId: String): Pair<String, String> = suspendCancellableCoroutine { cont ->
        cont.invokeOnCancellation {
            process.destroyForcibly()
            activeProcesses.remove(processId)
        }

        // Use a background thread to wait for the process
        val executor = Executors.newSingleThreadExecutor()
        executor.submit {
            try {
                val completed = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
                if (!completed) {
                    process.destroyForcibly()
                    cont.resumeWithException(Exception("Claude CLI timed out after ${timeoutMs}ms"))
                    return@submit
                }

                val stdout = process.inputStream.bufferedReader().readText()
                val stderr = process.errorStream.bufferedReader().readText()
                cont.resume(Pair(stdout, stderr))
            } catch (e: InterruptedException) {
                process.destroyForcibly()
                cont.resumeWithException(CancellationException("Process interrupted"))
            } catch (e: Exception) {
                process.destroyForcibly()
                cont.resumeWithException(e)
            } finally {
                executor.shutdown()
            }
        }
    }

    /**
     * Kill a specific process by ID
     */
    fun killProcess(processId: String) {
        activeProcesses.remove(processId)?.destroyForcibly()
    }

    /**
     * Kill all active processes
     */
    fun killAll() {
        activeProcesses.values.forEach { it.destroyForcibly() }
        activeProcesses.clear()
        activeConversations.values.forEach { runCatching { it.forceClose() } }
        activeConversations.clear()
    }

    /**
     * Start a new conversation using Claude CLI with streaming JSON.
     * Uses --print mode with stream-json format for pipe-compatible bidirectional streaming.
     */
    override suspend fun startConversation(systemPrompt: String): ConversationHandle = onIO {
        val sessionId = UUID.randomUUID().toString()

        val processBuilder = ProcessBuilder(
            "claude",
            "--print",
            "--verbose",
            "--model", "sonnet",
            "--output-format", "stream-json",
            "--input-format", "stream-json",
            "--session-id", sessionId,
            "--system-prompt", systemPrompt,
        )
        processBuilder.redirectErrorStream(false)

        val process = processBuilder.start()
        activeProcesses[sessionId] = process

        val handle = ClaudeConversationHandle(
            id = sessionId,
            process = process,
            stdin = process.outputStream.bufferedWriter(),
            stdout = process.inputStream.bufferedReader(),
            stderr = process.errorStream.bufferedReader(),
            timeoutMs = conversationTimeoutMs,
            onClose = {
                activeProcesses.remove(sessionId)
                activeConversations.remove(sessionId)
            }
        )

        activeConversations[sessionId] = handle
        handle
    }

    companion object {
        /**
         * Check if Claude CLI is installed
         */
        private fun isClaudeInstalled(): Boolean {
            return try {
                val command = if (System.getProperty("os.name").lowercase().contains("win")) {
                    arrayOf("where", "claude")
                } else {
                    arrayOf("which", "claude")
                }
                val process = ProcessBuilder(*command).start()
                process.waitFor(5, TimeUnit.SECONDS) && process.exitValue() == 0
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Get Claude CLI version
         */
        private fun getClaudeVersion(): String? {
            return try {
                val process = ProcessBuilder("claude", "--version").start()
                if (process.waitFor(5, TimeUnit.SECONDS) && process.exitValue() == 0) {
                    process.inputStream.bufferedReader().readText().trim()
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * Handle for a conversation with Claude CLI using stream-json format.
 * Uses --print mode with bidirectional JSON streaming for pipe compatibility.
 */
internal class ClaudeConversationHandle(
    override val id: String,
    private val process: Process,
    private val stdin: BufferedWriter,
    private val stdout: BufferedReader,
    private val stderr: BufferedReader,
    private val timeoutMs: Long,
    private val onClose: () -> Unit,
) : ConversationHandle {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lineChannel = Channel<String>(Channel.UNLIMITED)
    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

    @Volatile
    private var closed = false

    init {
        // Background reader for stdout - reads JSON lines
        scope.launch {
            try {
                while (isActive && process.isAlive) {
                    val line = stdout.readLine() ?: break
                    if (line.isNotBlank()) {
                        lineChannel.send(line)
                    }
                }
            } catch (e: Exception) {
                if (!closed) {
                    println("[ClaudeConversation] stdout reader error: ${e.message}")
                }
            } finally {
                lineChannel.close()
            }
        }

        // Background reader for stderr
        scope.launch {
            try {
                while (isActive && process.isAlive) {
                    val line = stderr.readLine() ?: break
                    if (line.isNotBlank()) {
                        println("[ClaudeConversation] stderr: $line")
                    }
                }
            } catch (e: Exception) {
                if (!closed) {
                    println("[ClaudeConversation] stderr reader error: ${e.message}")
                }
            }
        }
    }

    override suspend fun sendMessage(content: String): ConversationResponse {
        if (closed) {
            throw IllegalStateException("Conversation is closed")
        }

        if (!process.isAlive) {
            throw IllegalStateException("Claude process has terminated")
        }

        println("[ClaudeConversation] sendMessage: sending ${content.length} chars...")
        val inputTokens = TokenEstimator.estimate(content)

        // Send message as JSON - format: {"type":"user","message":{"role":"user","content":[{"type":"text","text":"..."}]}}
        val inputJson = buildJsonObject {
            put("type", "user")
            putJsonObject("message") {
                put("role", "user")
                putJsonArray("content") {
                    addJsonObject {
                        put("type", "text")
                        put("text", content)
                    }
                }
            }
        }
        onIO {
            stdin.write(inputJson.toString())
            stdin.newLine()
            stdin.flush()
        }
        println("[ClaudeConversation] sendMessage: sent JSON, waiting for response...")

        // Read streaming response
        val response = readStreamingResponse()
        println("[ClaudeConversation] sendMessage: got response of ${response.length} chars")

        // Parse for action calls
        val parseResult = ActionParser.parse(response)
        val outputTokens = TokenEstimator.estimate(response)

        return ConversationResponse(
            content = parseResult.textContent,
            actionCalls = parseResult.actionCalls,
            usage = TokenUsage(
                inputTokens = inputTokens,
                outputTokens = outputTokens,
            )
        )
    }

    /**
     * Read streaming JSON response until we get a system result message.
     * Format: assistant messages have type="message", role="assistant", content=[{type:"text",text:"..."}]
     *         final result has role="system" with result field
     */
    private suspend fun readStreamingResponse(): String {
        val contentBuilder = StringBuilder()

        println("[ClaudeConversation] readStreamingResponse: starting...")

        withTimeout(timeoutMs) {
            while (true) {
                val line = withTimeoutOrNull(100) {
                    lineChannel.receiveCatching().getOrNull()
                }

                if (line != null) {
                    println("[ClaudeConversation] received: $line")
                    try {
                        val jsonObj = json.parseToJsonElement(line).jsonObject
                        val type = jsonObj["type"]?.jsonPrimitive?.contentOrNull

                        when (type) {
                            // Final result message - contains complete response
                            "result" -> {
                                val result = jsonObj["result"]?.jsonPrimitive?.contentOrNull ?: ""
                                contentBuilder.append(result)
                                break
                            }
                            // Ignore intermediary messages
                            "system", "assistant", "user" -> { }
                            else -> {
                                println("[ClaudeConversation] Unknown message type: $type")
                            }
                        }
                    } catch (e: Exception) {
                        println("[ClaudeConversation] Failed to parse JSON: $line - ${e.message}")
                    }
                } else {
                    // Check if process ended
                    if (!process.isAlive || lineChannel.isClosedForReceive) {
                        println("[ClaudeConversation] readStreamingResponse: process/channel closed")
                        break
                    }
                }
            }
        }

        return contentBuilder.toString()
    }

    override suspend fun reset() {
        // For stream-json mode, we can't send /clear
        // Instead, we'd need to restart the process or use a new session
        // For now, just log a warning
        println("[ClaudeConversation] reset() not supported in stream-json mode")
    }

    override suspend fun close() {
        if (closed) return
        forceClose()
    }

    /**
     * Force close - fire and forget.
     */
    internal fun forceClose() {
        if (closed) return
        closed = true
        scope.cancel()
        onClose()

        // Fire and forget cleanup
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch(Dispatchers.IO) {
            runCatching {
                process.destroyForcibly()
                stdin.close()
                stdout.close()
                stderr.close()
            }
        }
    }
}
