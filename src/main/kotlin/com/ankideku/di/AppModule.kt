package com.ankideku.di

import app.cash.sqldelight.db.SqlDriver
import com.ankideku.data.local.database.AnkiDekuDb
import com.ankideku.data.local.database.DatabaseFactory
import com.ankideku.data.local.database.DatabaseWithDriver
import com.ankideku.data.local.repository.SqlDeckRepository
import com.ankideku.data.local.repository.SqlHistoryRepository
import com.ankideku.data.local.repository.SqlSelPresetRepository
import com.ankideku.data.local.repository.SqlSelService
import com.ankideku.data.local.repository.SqlNoteTypeConfigRepository
import com.ankideku.data.local.repository.SqlSessionRepository
import com.ankideku.data.local.repository.SqlSettingsRepository
import com.ankideku.data.local.repository.SqlSuggestionRepository
import com.ankideku.data.local.service.SqlTransactionService
import com.ankideku.data.remote.anki.AnkiConnectClient
import com.ankideku.data.remote.anki.AnkiConnectionMonitor
import com.ankideku.domain.repository.DeckRepository
import com.ankideku.domain.repository.HistoryRepository
import com.ankideku.domain.repository.NoteTypeConfigRepository
import com.ankideku.domain.repository.SessionRepository
import com.ankideku.domain.repository.SelPresetRepository
import com.ankideku.domain.repository.SettingsRepository
import com.ankideku.domain.repository.SuggestionRepository
import com.ankideku.domain.sel.SelService
import com.ankideku.domain.service.TransactionService
import com.ankideku.domain.usecase.deck.DeckFinder
import com.ankideku.domain.usecase.history.HistoryFinder
import com.ankideku.domain.usecase.notetype.NoteTypeConfigFinder
import com.ankideku.domain.usecase.suggestion.BatchReviewFeature
import com.ankideku.domain.usecase.suggestion.ConflictChecker
import com.ankideku.domain.usecase.suggestion.ReviewSuggestionFeature
import com.ankideku.domain.usecase.session.SessionFinder
import com.ankideku.domain.usecase.suggestion.SessionOrchestrator
import com.ankideku.domain.usecase.settings.SettingsManager
import com.ankideku.domain.usecase.suggestion.SuggestionFinder
import com.ankideku.domain.usecase.deck.SyncDeckFeature
import com.ankideku.util.getAppDataDir
import com.ankideku.util.json
import io.ktor.client.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import com.ankideku.ui.screens.main.MainViewModel
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

    // Database - create with driver exposed for raw SQL access
    single {
        val dbPath = "${getAppDataDir().absolutePath}/ankideku.db"
        DatabaseFactory.createWithFileAndDriver(dbPath)
    }
    single<AnkiDekuDb> { get<DatabaseWithDriver>().database }
    single<SqlDriver> { get<DatabaseWithDriver>().driver }

    // Remote services
    single { AnkiConnectClient(get()) }
    single {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        AnkiConnectionMonitor(client = get(), scope = scope)
    }

    // Repositories
    single<DeckRepository> { SqlDeckRepository(get()) }
    single<SessionRepository> { SqlSessionRepository(get()) }
    single<SuggestionRepository> { SqlSuggestionRepository(get()) }
    single<HistoryRepository> { SqlHistoryRepository(get()) }
    single<SettingsRepository> { SqlSettingsRepository(get()) }
    single<NoteTypeConfigRepository> { SqlNoteTypeConfigRepository(get()) }
    single<SelPresetRepository> { SqlSelPresetRepository(get()) }

    // Services
    single<TransactionService> { SqlTransactionService(get()) }
    single<SelService> { SqlSelService(get()) }

    // Finders (query services)
    single { DeckFinder(get(), get()) }
    single { SessionFinder(get()) }
    single { SuggestionFinder(get()) }
    single { HistoryFinder(get()) }
    single { SettingsManager(get()) }
    single { NoteTypeConfigFinder(get()) }

    // Use cases (complex operations)
    factory { SyncDeckFeature(get(), get(), get()) }
    factory { SessionOrchestrator(get(), get(), get(), get()) }
    single { ConflictChecker(get()) }
    factory { ReviewSuggestionFeature(get(), get(), get(), get(), get(), get(), get()) }
    factory { BatchReviewFeature(get(), get(), get(), get(), get(), get(), get()) }

    // ViewModels
    single {
        MainViewModel(
            syncDeckFeature = get(),
            sessionOrchestrator = get(),
            reviewSuggestionFeature = get(),
            batchReviewFeature = get(),
            selService = get(),
            suggestionRepository = get(),
            deckFinder = get(),
            sessionFinder = get(),
            suggestionFinder = get(),
            historyFinder = get(),
            settingsManager = get(),
            connectionMonitor = get(),
            noteTypeConfigFinder = get(),
        )
    }
}
