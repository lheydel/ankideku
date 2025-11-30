package com.ankideku.data.local.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

/**
 * Database factory and management
 */
object DatabaseFactory {

    /**
     * Create the SQLDelight database with the given driver
     */
    fun create(driver: SqlDriver): AnkiDekuDb {
        AnkiDekuDb.Schema.create(driver)
        return AnkiDekuDb(driver)
    }

    /**
     * Create the database with a file-based SQLite driver
     */
    fun createWithFile(dbPath: String): AnkiDekuDb {
        // Ensure parent directory exists
        File(dbPath).parentFile?.mkdirs()

        val driver = JdbcSqliteDriver("jdbc:sqlite:$dbPath")
        return create(driver)
    }

    /**
     * Create an in-memory database (for testing)
     */
    fun createInMemory(): AnkiDekuDb {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        return create(driver)
    }

    /**
     * Get the default database path for the application
     */
    fun getDefaultDbPath(): String {
        val userHome = System.getProperty("user.home")
        val appDir = File(userHome, ".ankideku")
        appDir.mkdirs()
        return File(appDir, "ankideku.db").absolutePath
    }
}
