package org.alc.hepokoCore.report

import java.io.File
import java.sql.Connection
import java.sql.DriverManager

object ReportDatabase {
    private var connection: Connection? = null

    fun setup(dataFolder: File) {
        val dataDirectory = File(dataFolder, "data")
        if (!dataDirectory.exists()) dataDirectory.mkdirs()

        val dbFile = File(dataDirectory, "reports_prison.db")
        Class.forName("org.sqlite.JDBC")
        connection = DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}")

        connection?.createStatement()?.use { statement ->
            statement.execute(
                """
                CREATE TABLE IF NOT EXISTS reports (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    reporter TEXT NOT NULL,
                    target TEXT NOT NULL,
                    reason TEXT NOT NULL,
                    status TEXT NOT NULL DEFAULT 'OPEN',
                    moderator TEXT DEFAULT '',
                    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
                """.trimIndent()
            )
        }
    }

    fun createReport(reporter: String, target: String, reason: String) {
        val query = "INSERT INTO reports (reporter, target, reason, status) VALUES (?, ?, ?, 'OPEN')"
        connection?.prepareStatement(query)?.use { ps ->
            ps.setString(1, reporter.lowercase())
            ps.setString(2, target.lowercase())
            ps.setString(3, reason)
            ps.executeUpdate()
        }
    }

    fun acceptReport(reportId: Int, moderator: String): Boolean {
        val query = "UPDATE reports SET status = 'IN_PROGRESS', moderator = ? WHERE id = ? AND status = 'OPEN'"
        return connection?.prepareStatement(query)?.use { ps ->
            ps.setString(1, moderator.lowercase())
            ps.setInt(2, reportId)
            ps.executeUpdate() > 0
        } ?: false
    }

    fun closeReport(reportId: Int, moderator: String): Boolean {
        val query = "UPDATE reports SET status = 'CLOSED', moderator = ? WHERE id = ?"
        return connection?.prepareStatement(query)?.use { ps ->
            ps.setString(1, moderator.lowercase())
            ps.setInt(2, reportId)
            ps.executeUpdate() > 0
        } ?: false
    }

    fun getActiveReports(): List<ReportData> {
        val list = mutableListOf<ReportData>()
        val query = "SELECT id, reporter, target, reason, status, moderator FROM reports WHERE status != 'CLOSED' ORDER BY id ASC"
        connection?.prepareStatement(query)?.use { ps ->
            ps.executeQuery().use { rs ->
                while (rs.next()) {
                    list.add(
                        ReportData(
                            rs.getInt("id"),
                            rs.getString("reporter"),
                            rs.getString("target"),
                            rs.getString("reason"),
                            rs.getString("status"),
                            rs.getString("moderator") ?: ""
                        )
                    )
                }
            }
        }
        return list
    }

    fun hasActiveReport(reporter: String, target: String): Boolean {
        val query = "SELECT 1 FROM reports WHERE reporter = ? AND target = ? AND status != 'CLOSED' LIMIT 1"
        return connection?.prepareStatement(query)?.use { ps ->
            ps.setString(1, reporter.lowercase())
            ps.setString(2, target.lowercase())
            ps.executeQuery().use { rs ->
                rs.next()
            }
        } ?: false
    }

    fun close() {
        runCatching { connection?.close() }
    }
}

data class ReportData(
    val id: Int,
    val reporter: String,
    val target: String,
    val reason: String,
    val status: String,
    val moderator: String
)