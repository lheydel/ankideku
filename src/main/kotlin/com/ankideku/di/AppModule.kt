package com.ankideku.di

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.ankideku.data.local.database.AnkiDekuDb
import com.ankideku.data.local.repository.SqlDeckRepository
import com.ankideku.data.local.repository.SqlHistoryRepository
import com.ankideku.data.local.repository.SqlSessionRepository
import com.ankideku.data.local.repository.SqlSettingsRepository
import com.ankideku.data.local.repository.SqlSuggestionRepository
import com.ankideku.data.local.service.SqlTransactionService
import com.ankideku.data.remote.anki.AnkiConnectClient
import com.ankideku.data.remote.anki.AnkiConnectionMonitor
import com.ankideku.data.remote.llm.LlmConfig
import com.ankideku.data.remote.llm.LlmService
import com.ankideku.data.remote.llm.LlmServiceFactory
import com.ankideku.domain.repository.DeckRepository
import com.ankideku.domain.repository.HistoryRepository
import com.ankideku.domain.repository.SessionRepository
import com.ankideku.domain.repository.SettingsRepository
import com.ankideku.domain.repository.SuggestionRepository
import com.ankideku.domain.service.TransactionService
import com.ankideku.domain.usecase.ReviewSuggestionUseCase
import com.ankideku.domain.usecase.SearchHistoryUseCase
import com.ankideku.domain.usecase.SessionOrchestrator
import com.ankideku.domain.usecase.SyncDeckUseCase
import com.ankideku.util.json
import com.ankideku.util.onIO
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import org.koin.dsl.module
import java.io.File

/**
 * Main Koin dependency injection module.
 */
val appModule = module {
    // HTTP Client
    single {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(json)
            }
        }
    }

    // Database
    single {
        val dbPath = "${getDataPath()}/ankideku.db"
        val driver = JdbcSqliteDriver("jdbc:sqlite:$dbPath")

        // Create tables if they don't exist
        AnkiDekuDb.Schema.create(driver)

        AnkiDekuDb(driver)
    }

    // Remote services
    single { AnkiConnectClient(get()) }
    single { AnkiConnectionMonitor(get()) }

    // LLM Service - uses factory with settings
    // Use 'factory' so it reads current settings each time
    factory<LlmService> {
        val settingsRepo = get<SettingsRepository>()
        val provider = runBlocking { onIO { settingsRepo.getSettings().llmProvider } }
        LlmServiceFactory.getInstance(LlmConfig(provider))
    }

    // Repositories
    single<DeckRepository> { SqlDeckRepository(get()) }
    single<SessionRepository> { SqlSessionRepository(get()) }
    single<SuggestionRepository> { SqlSuggestionRepository(get()) }
    single<HistoryRepository> { SqlHistoryRepository(get()) }
    single<SettingsRepository> { SqlSettingsRepository(get()) }

    // Services
    single<TransactionService> { SqlTransactionService(get()) }

    // Use cases
    factory { SyncDeckUseCase(get(), get(), get()) }
    factory { SessionOrchestrator(get(), get(), get(), get()) }
    factory { ReviewSuggestionUseCase(get(), get(), get(), get(), get(), get()) }
    factory { SearchHistoryUseCase(get()) }

    // ViewModels - Added in Phase 4
}

/**
 * Get the platform-specific data directory for AnkiDeku.
 * Creates the directory if it doesn't exist.
 */
private fun getDataPath(): String {
    val os = System.getProperty("os.name").lowercase()
    val home = System.getProperty("user.home")

    return when {
        os.contains("win") -> "$home/AppData/Roaming/AnkiDeku"
        os.contains("mac") -> "$home/Library/Application Support/AnkiDeku"
        else -> "$home/.config/AnkiDeku"
    }.also { File(it).mkdirs() }
}
