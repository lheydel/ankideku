package com.ankideku.data.remote.llm

import com.ankideku.util.onIO
import kotlinx.coroutines.async
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Claude CLI Service
 * Implements LLM by spawning Claude Code CLI subprocess.
 */
class ClaudeCliService(
    private val timeoutMs: Long = 300_000,  // 5 minutes default
    maxRetries: Int = 2,
) : BaseLlmService(maxRetries = maxRetries) {

    private val activeProcesses = ConcurrentHashMap<String, Process>()

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
