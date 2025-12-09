package com.ankideku.data.remote.llm

import com.ankideku.util.TokenEstimator
import com.ankideku.util.onIO
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedReader
import java.io.BufferedWriter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

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

    override suspend fun callLlm(prompt: String): String = onIO {
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

            // Read stdout and stderr concurrently
            val stdoutDeferred = async {
                process.inputStream.bufferedReader().use { it.readText() }
            }
            val stderrDeferred = async {
                process.errorStream.bufferedReader().use { it.readText() }
            }

            // Wait for process with timeout
            val completed = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)

            if (!completed) {
                process.destroyForcibly()
                throw Exception("Claude CLI timed out after ${timeoutMs}ms")
            }

            val stdout = stdoutDeferred.await()
            val stderr = stderrDeferred.await()
            val exitCode = process.exitValue()

            if (exitCode != 0) {
                throw Exception(stderr.ifBlank { "Claude CLI exited with code $exitCode" })
            }

            stdout
        } finally {
            activeProcesses.remove(processId)?.destroyForcibly()
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
     * Start a new conversation using Claude CLI in interactive mode.
     * The Claude process is kept alive for the duration of the conversation.
     */
    override suspend fun startConversation(systemPrompt: String): ConversationHandle = onIO {
        val conversationId = UUID.randomUUID().toString()

        val processBuilder = ProcessBuilder(
            "claude",
            "--model", "sonnet",
            "--system-prompt", systemPrompt,
        )
        processBuilder.redirectErrorStream(false)

        val process = processBuilder.start()
        activeProcesses[conversationId] = process

        val handle = ClaudeConversationHandle(
            id = conversationId,
            process = process,
            stdin = process.outputStream.bufferedWriter(),
            stdout = process.inputStream.bufferedReader(),
            stderr = process.errorStream.bufferedReader(),
            timeoutMs = conversationTimeoutMs,
            onClose = {
                activeProcesses.remove(conversationId)
                activeConversations.remove(conversationId)
            }
        )

        activeConversations[conversationId] = handle
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
 * Handle for an interactive conversation with Claude CLI.
 * Manages a persistent process and reads/writes via stdin/stdout.
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

    @Volatile
    private var closed = false

    override suspend fun sendMessage(content: String): ConversationResponse = onIO {
        if (closed) {
            throw IllegalStateException("Conversation is closed")
        }

        if (!process.isAlive) {
            throw IllegalStateException("Claude process has terminated")
        }

        val inputTokens = TokenEstimator.estimate(content)

        // Write message to stdin
        stdin.write(content)
        stdin.newLine()
        stdin.flush()

        // Read response until we get a complete message
        // Claude CLI outputs a response and then waits for the next input
        val response = readResponse()

        // Parse for action calls
        val parseResult = ActionParser.parse(response)
        val outputTokens = TokenEstimator.estimate(response)

        ConversationResponse(
            content = parseResult.textContent,
            actionCalls = parseResult.actionCalls,
            usage = TokenUsage(
                inputTokens = inputTokens,
                outputTokens = outputTokens,
            )
        )
    }

    /**
     * Read response from Claude.
     * Claude CLI in interactive mode outputs the response and then waits.
     * We detect end of response by checking for a pause in output.
     */
    private suspend fun readResponse(): String = onIO {
        val buffer = StringBuilder()
        val readTimeoutMs = 100L // Short timeout to check for more data
        var consecutiveEmptyReads = 0
        val maxEmptyReads = 20 // ~2 seconds of silence means response complete

        val overallTimeout = withTimeoutOrNull(timeoutMs) {
            while (coroutineContext.isActive) {
                // Check if there's data available
                if (stdout.ready()) {
                    val char = stdout.read()
                    if (char == -1) break // EOF
                    buffer.append(char.toChar())
                    consecutiveEmptyReads = 0
                } else {
                    consecutiveEmptyReads++
                    if (consecutiveEmptyReads >= maxEmptyReads && buffer.isNotEmpty()) {
                        // No more data coming, response complete
                        break
                    }
                    delay(readTimeoutMs)
                }

                // Also check stderr for errors
                if (stderr.ready()) {
                    val errorBuffer = StringBuilder()
                    while (stderr.ready()) {
                        val char = stderr.read()
                        if (char == -1) break
                        errorBuffer.append(char.toChar())
                    }
                    if (errorBuffer.isNotEmpty()) {
                        println("[ClaudeConversation] stderr: $errorBuffer")
                    }
                }
            }
            buffer.toString()
        }

        overallTimeout ?: throw Exception("Claude conversation timed out after ${timeoutMs}ms")
    }

    override suspend fun close() = onIO {
        if (closed) return@onIO
        closed = true

        try {
            // Send exit/quit to gracefully close
            stdin.write("/exit")
            stdin.newLine()
            stdin.flush()

            // Give it a moment to exit
            process.waitFor(2, TimeUnit.SECONDS)
        } catch (e: Exception) {
            // Ignore errors during close
        } finally {
            forceClose()
        }
    }

    /**
     * Force close without graceful shutdown.
     */
    internal fun forceClose() {
        closed = true
        try {
            stdin.close()
            stdout.close()
            stderr.close()
            process.destroyForcibly()
        } catch (e: Exception) {
            // Ignore
        }
        onClose()
    }
}
