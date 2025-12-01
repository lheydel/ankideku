package com.ankideku.domain.model

import com.ankideku.data.remote.llm.LlmProvider

data class Settings(
    val llmProvider: LlmProvider = LlmProvider.MOCK,
    val theme: AppTheme = AppTheme.System,
)

enum class AppTheme {
    Light,
    Dark,
    System,
}
