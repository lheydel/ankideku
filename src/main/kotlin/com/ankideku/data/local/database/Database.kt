package com.ankideku.data.local.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

/**
 * Result of database creation containing both the driver and database instance.
 */
data class DatabaseWithDriver(
    val driver: SqlDriver,
    val database: AnkiDekuDb,
)

/**
 * Database factory and management
 */
object DatabaseFactory {

    /**
     * Create or migrate the SQLDelight database with the given driver.
     * - New databases: creates full schema
     * - Existing databases: runs necessary migrations
     */
    fun create(driver: SqlDriver): AnkiDekuDb {
        val currentVersion = getVersion(driver)
        val schemaVersion = AnkiDekuDb.Schema.version

        if (currentVersion == 0L) {
            // New database - create full schema
            AnkiDekuDb.Schema.create(driver)
            setVersion(driver, schemaVersion)
        } else if (currentVersion < schemaVersion) {
            // Existing database - run migrations
            AnkiDekuDb.Schema.migrate(driver, currentVersion, schemaVersion)
            setVersion(driver, schemaVersion)
        }

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
     * Create the database with a file-based SQLite driver, returning both driver and database.
     * Use this when you need access to the raw SqlDriver for custom queries.
     */
    fun createWithFileAndDriver(dbPath: String): DatabaseWithDriver {
        // Ensure parent directory exists
        File(dbPath).parentFile?.mkdirs()

        val driver = JdbcSqliteDriver("jdbc:sqlite:$dbPath")
        val database = create(driver)
        return DatabaseWithDriver(driver, database)
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

    /**
     * Get current database schema version (0 if not set = new database)
     */
    private fun getVersion(driver: SqlDriver): Long {
        val result = driver.executeQuery<Long>(
            identifier = null,
            sql = "PRAGMA user_version;",
            mapper = { cursor ->
                val hasRow = cursor.next()
                if (hasRow.value) {
                    QueryResult.Value(cursor.getLong(0) ?: 0L)
                } else {
                    QueryResult.Value(0L)
                }
            },
            parameters = 0,
        )
        return result.value
    }

    /**
     * Set database schema version
     */
    private fun setVersion(driver: SqlDriver, version: Long) {
        driver.execute(null, "PRAGMA user_version = $version;", 0)
    }
}
