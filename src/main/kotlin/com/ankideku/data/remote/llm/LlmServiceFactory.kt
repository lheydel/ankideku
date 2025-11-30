package com.ankideku.data.remote.llm

/**
 * Factory for creating LLM service instances.
 * Creates the appropriate service based on configuration.
 */
object LlmServiceFactory {
    private var instance: LlmService? = null
    private var currentProvider: LlmProvider? = null

    /**
     * Create an LLM service based on the provided configuration.
     */
    fun create(config: LlmConfig = DEFAULT_LLM_CONFIG): LlmService {
        return when (config.provider) {
            LlmProvider.CLAUDE_CODE -> ClaudeCliService()
            LlmProvider.MOCK -> MockLlmService()
        }
    }

    /**
     * Get or create a singleton LLM service instance.
     * Recreates if provider changes.
     */
    fun getInstance(config: LlmConfig = DEFAULT_LLM_CONFIG): LlmService {
        // Recreate if provider changed
        if (currentProvider != config.provider) {
            instance = null
        }

        if (instance == null) {
            instance = create(config)
            currentProvider = config.provider
        }

        return instance!!
    }

    /**
     * Clear the singleton instance.
     * Useful for testing or when config changes.
     */
    fun clearInstance() {
        instance = null
        currentProvider = null
    }

    /**
     * Get list of available providers.
     */
    fun getAvailableProviders(): List<LlmProvider> {
        return LlmProvider.entries
    }

    /**
     * Check if a provider is available (implemented).
     */
    fun isProviderAvailable(provider: LlmProvider): Boolean {
        return provider in getAvailableProviders()
    }
}
