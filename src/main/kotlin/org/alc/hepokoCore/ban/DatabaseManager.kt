package org.alc.hepokoCore.ban

import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Timestamp
import java.util.Date

object DatabaseManager {
    private var connection: Connection? = null

    fun setup(dataFolder: File) {
        val dataDirectory = File(dataFolder, "data")
        if (!dataDirectory.exists()) dataDirectory.mkdirs()

        val dbFile = File(dataDirectory, "bans.db")
        Class.forName("org.sqlite.JDBC")
        connection = DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}")

        connection?.createStatement()?.use { statement ->
            statement.execute("""
                CREATE TABLE IF NOT EXISTS ip_bans (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    target TEXT NOT NULL,
                    reason TEXT NOT NULL,
                    expiry_time TIMESTAMP NOT NULL,
                    banner TEXT NOT NULL
                )
            """.trimIndent())
            statement.execute("""
                CREATE TABLE IF NOT EXISTS mutes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL,
                    reason TEXT NOT NULL,
                    expiry_time TIMESTAMP NOT NULL,
                    muter TEXT NOT NULL
                )
            """.trimIndent())
        }
    }

    fun addBan(target: String, reason: String, expiryDate: Date, banner: String) {
        val query = "INSERT INTO ip_bans (target, reason, expiry_time, banner) VALUES (?, ?, ?, ?)"
        connection?.prepareStatement(query)?.use { ps ->
            ps.setString(1, target.lowercase())
            ps.setString(2, reason)
            ps.setTimestamp(3, Timestamp(expiryDate.time))
            ps.setString(4, banner)
            ps.executeUpdate()
        }
    }

    fun checkBan(target: String): BanInfo? {
        val query = "SELECT reason, expiry_time, banner FROM ip_bans WHERE target = ? AND expiry_time > ? ORDER BY id DESC LIMIT 1"
        return connection?.prepareStatement(query)?.use { ps ->
            ps.setString(1, target.lowercase())
            ps.setTimestamp(2, Timestamp(System.currentTimeMillis()))
            ps.executeQuery().use { rs ->
                if (rs.next()) {
                    BanInfo(
                        reason = rs.getString("reason"),
                        expiryDate = Date(rs.getTimestamp("expiry_time").time),
                        banner = rs.getString("banner")
                    )
                } else null
            }
        }
    }

    fun removeBan(target: String): Boolean {
        val query = "DELETE FROM ip_bans WHERE target = ?"
        return connection?.prepareStatement(query)?.use { ps ->
            ps.setString(1, target.lowercase())
            ps.executeUpdate() > 0
        } ?: false
    }

    fun getActiveBans(isIp: Boolean): List<BanRecord> {
        val list = mutableListOf<BanRecord>()
        val query = "SELECT target, reason, expiry_time, banner FROM ip_bans WHERE expiry_time > ?"
        connection?.prepareStatement(query)?.use { ps ->
            ps.setTimestamp(1, Timestamp(System.currentTimeMillis()))
            ps.executeQuery().use { rs ->
                while (rs.next()) {
                    val target = rs.getString("target")
                    val isTargetIp = target.contains(".")
                    if (isIp == isTargetIp) {
                        list.add(
                            BanRecord(
                                target = target,
                                reason = rs.getString("reason"),
                                expiryDate = Date(rs.getTimestamp("expiry_time").time),
                                banner = rs.getString("banner")
                            )
                        )
                    }
                }
            }
        }
        return list
    }

    fun addMute(username: String, reason: String, expiryDate: Date, muter: String) {
        val query = "INSERT INTO mutes (username, reason, expiry_time, muter) VALUES (?, ?, ?, ?)"
        connection?.prepareStatement(query)?.use { ps ->
            ps.setString(1, username.lowercase())
            ps.setString(2, reason)
            ps.setTimestamp(3, Timestamp(expiryDate.time))
            ps.setString(4, muter)
            ps.executeUpdate()
        }
    }

    fun checkMute(username: String): MuteInfo? {
        val query = "SELECT reason, expiry_time, muter FROM mutes WHERE username = ? AND expiry_time > ? ORDER BY id DESC LIMIT 1"
        return connection?.prepareStatement(query)?.use { ps ->
            ps.setString(1, username.lowercase())
            ps.setTimestamp(2, Timestamp(System.currentTimeMillis()))
            ps.executeQuery().use { rs ->
                if (rs.next()) {
                    MuteInfo(
                        reason = rs.getString("reason"),
                        expiryDate = Date(rs.getTimestamp("expiry_time").time),
                        muter = rs.getString("muter")
                    )
                } else null
            }
        }
    }

    fun removeMute(username: String): Boolean {
        val query = "DELETE FROM mutes WHERE username = ?"
        return connection?.prepareStatement(query)?.use { ps ->
            ps.setString(1, username.lowercase())
            ps.executeUpdate() > 0
        } ?: false
    }

    fun close() {
        runCatching { connection?.close() }
    }
}

data class BanInfo(val reason: String, val expiryDate: Date, val banner: String)
data class BanRecord(val target: String, val reason: String, val expiryDate: Date, val banner: String)
data class MuteInfo(val reason: String, val expiryDate: Date, val muter: String)