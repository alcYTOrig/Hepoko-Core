package org.alc.hepokoCore.protect

import java.sql.*
import java.time.Instant

/**
 * Database manager for CoreProtect functionality.
 * Stores block changes, player actions, and chat logs in SQLite.
 */
object ProtectDatabase {
    private var connection: Connection? = null

    /**
     * Initialize the database and create tables if they don't exist.
     */
    fun setup(dataFolder: java.io.File) {
        val dbFile = java.io.File(dataFolder, "coreprotect.db")
        val url = "jdbc:sqlite:${dbFile.absolutePath}"

        connection = DriverManager.getConnection(url)
        createTables()
    }

    /**
     * Create necessary tables for CoreProtect.
     */
    private fun createTables() {
        connection?.createStatement()?.use { stmt ->
            // Table for block changes (place, break, etc.)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS block_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_name TEXT NOT NULL,
                    player_uuid TEXT NOT NULL,
                    action_type TEXT NOT NULL,  -- 'PLACE', 'BREAK', 'PHYSICS', etc.
                    world_name TEXT NOT NULL,
                    x INTEGER NOT NULL,
                    y INTEGER NOT NULL,
                    z INTEGER NOT NULL,
                    block_type TEXT NOT NULL,
                    block_data INTEGER DEFAULT 0,
                    old_block_type TEXT,
                    old_block_data INTEGER,
                    timestamp INTEGER NOT NULL,
                    rolled_back BOOLEAN DEFAULT false
                )
            """)

            // Table for chest/container access
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS container_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_name TEXT NOT NULL,
                    player_uuid TEXT NOT NULL,
                    action_type TEXT NOT NULL,  -- 'OPEN', 'TAKE', 'PLACE'
                    world_name TEXT NOT NULL,
                    x INTEGER NOT NULL,
                    y INTEGER NOT NULL,
                    z INTEGER NOT NULL,
                    container_type TEXT NOT NULL,
                    item_type TEXT,
                    item_amount INTEGER DEFAULT 1,
                    timestamp INTEGER NOT NULL
                )
            """)

            // Table for chat messages
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS chat_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_name TEXT NOT NULL,
                    player_uuid TEXT NOT NULL,
                    message TEXT NOT NULL,
                    channel TEXT DEFAULT 'global',
                    timestamp INTEGER NOT NULL
                )
            """)

            // Table for player sessions (join/quit)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS session_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_name TEXT NOT NULL,
                    player_uuid TEXT NOT NULL,
                    action_type TEXT NOT NULL,  -- 'JOIN', 'QUIT', 'KICK', 'BAN'
                    ip_address TEXT,
                    timestamp INTEGER NOT NULL
                )
            """)

            // Table for entity damage/kills
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS entity_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    attacker_name TEXT,
                    attacker_uuid TEXT,
                    attacker_type TEXT,
                    victim_name TEXT NOT NULL,
                    victim_uuid TEXT,
                    victim_type TEXT NOT NULL,
                    action_type TEXT NOT NULL,  -- 'DAMAGE', 'KILL'
                    damage_amount REAL,
                    damage_cause TEXT,
                    weapon TEXT,
                    world_name TEXT NOT NULL,
                    x INTEGER,
                    y INTEGER,
                    z INTEGER,
                    timestamp INTEGER NOT NULL
                )
            """)

            // Indexes for faster queries
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_block_logs_player ON block_logs(player_uuid)")
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_block_logs_location ON block_logs(world_name, x, y, z)")
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_block_logs_timestamp ON block_logs(timestamp)")
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_chat_logs_player ON chat_logs(player_uuid)")
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_chat_logs_timestamp ON chat_logs(timestamp)")
        }
    }

    /**
     * Log a block action (place, break, etc.)
     */
    fun logBlockAction(
        playerName: String,
        playerUuid: String,
        actionType: String,
        worldName: String,
        x: Int,
        y: Int,
        z: Int,
        blockType: String,
        blockData: Int = 0,
        oldBlockType: String? = null,
        oldBlockData: Int? = null
    ) {
        connection?.prepareStatement("""
            INSERT INTO block_logs 
            (player_name, player_uuid, action_type, world_name, x, y, z, block_type, block_data, old_block_type, old_block_data, timestamp)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """)?.use { pstmt ->
            pstmt.setString(1, playerName)
            pstmt.setString(2, playerUuid)
            pstmt.setString(3, actionType)
            pstmt.setString(4, worldName)
            pstmt.setInt(5, x)
            pstmt.setInt(6, y)
            pstmt.setInt(7, z)
            pstmt.setString(8, blockType)
            pstmt.setInt(9, blockData)
            pstmt.setString(10, oldBlockType)
            pstmt.setInt(11, oldBlockData ?: 0)
            pstmt.setLong(12, Instant.now().epochSecond)
            pstmt.executeUpdate()
        }
    }

    /**
     * Log a container action (chest open, take item, etc.)
     */
    fun logContainerAction(
        playerName: String,
        playerUuid: String,
        actionType: String,
        worldName: String,
        x: Int,
        y: Int,
        z: Int,
        containerType: String,
        itemType: String? = null,
        itemAmount: Int = 1
    ) {
        connection?.prepareStatement("""
            INSERT INTO container_logs 
            (player_name, player_uuid, action_type, world_name, x, y, z, container_type, item_type, item_amount, timestamp)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """)?.use { pstmt ->
            pstmt.setString(1, playerName)
            pstmt.setString(2, playerUuid)
            pstmt.setString(3, actionType)
            pstmt.setString(4, worldName)
            pstmt.setInt(5, x)
            pstmt.setInt(6, y)
            pstmt.setInt(7, z)
            pstmt.setString(8, containerType)
            pstmt.setString(9, itemType)
            pstmt.setInt(10, itemAmount)
            pstmt.setLong(11, Instant.now().epochSecond)
            pstmt.executeUpdate()
        }
    }

    /**
     * Log a chat message
     */
    fun logChatMessage(
        playerName: String,
        playerUuid: String,
        message: String,
        channel: String = "global"
    ) {
        connection?.prepareStatement("""
            INSERT INTO chat_logs 
            (player_name, player_uuid, message, channel, timestamp)
            VALUES (?, ?, ?, ?, ?)
        """)?.use { pstmt ->
            pstmt.setString(1, playerName)
            pstmt.setString(2, playerUuid)
            pstmt.setString(3, message)
            pstmt.setString(4, channel)
            pstmt.setLong(5, Instant.now().epochSecond)
            pstmt.executeUpdate()
        }
    }

    /**
     * Log a session action (join, quit, kick, ban)
     */
    fun logSessionAction(
        playerName: String,
        playerUuid: String,
        actionType: String,
        ipAddress: String? = null
    ) {
        connection?.prepareStatement("""
            INSERT INTO session_logs 
            (player_name, player_uuid, action_type, ip_address, timestamp)
            VALUES (?, ?, ?, ?, ?)
        """)?.use { pstmt ->
            pstmt.setString(1, playerName)
            pstmt.setString(2, playerUuid)
            pstmt.setString(3, actionType)
            pstmt.setString(4, ipAddress)
            pstmt.setLong(5, Instant.now().epochSecond)
            pstmt.executeUpdate()
        }
    }

    /**
     * Log an entity action (damage, kill)
     */
    fun logEntityAction(
        attackerName: String?,
        attackerUuid: String?,
        attackerType: String,
        victimName: String,
        victimUuid: String?,
        victimType: String,
        actionType: String,
        damageAmount: Double? = null,
        damageCause: String? = null,
        weapon: String? = null,
        worldName: String,
        x: Int? = null,
        y: Int? = null,
        z: Int? = null
    ) {
        connection?.prepareStatement("""
            INSERT INTO entity_logs 
            (attacker_name, attacker_uuid, attacker_type, victim_name, victim_uuid, victim_type, 
             action_type, damage_amount, damage_cause, weapon, world_name, x, y, z, timestamp)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """)?.use { pstmt ->
            pstmt.setString(1, attackerName)
            pstmt.setString(2, attackerUuid)
            pstmt.setString(3, attackerType)
            pstmt.setString(4, victimName)
            pstmt.setString(5, victimUuid)
            pstmt.setString(6, victimType)
            pstmt.setString(7, actionType)
            pstmt.setDouble(8, damageAmount ?: 0.0)
            pstmt.setString(9, damageCause)
            pstmt.setString(10, weapon)
            pstmt.setString(11, worldName)
            pstmt.setInt(12, x ?: 0)
            pstmt.setInt(13, y ?: 0)
            pstmt.setInt(14, z ?: 0)
            pstmt.setLong(15, Instant.now().epochSecond)
            pstmt.executeUpdate()
        }
    }

    /**
     * Get block changes at a specific location
     */
    fun getBlockHistory(
        worldName: String,
        x: Int,
        y: Int,
        z: Int,
        limit: Int = 10
    ): List<BlockLog> {
        val logs = mutableListOf<BlockLog>()
        connection?.prepareStatement("""
            SELECT * FROM block_logs 
            WHERE world_name = ? AND x = ? AND y = ? AND z = ?
            ORDER BY timestamp DESC LIMIT ?
        """)?.use { pstmt ->
            pstmt.setString(1, worldName)
            pstmt.setInt(2, x)
            pstmt.setInt(3, y)
            pstmt.setInt(4, z)
            pstmt.setInt(5, limit)
            pstmt.executeQuery().use { rs ->
                while (rs.next()) {
                    logs.add(BlockLog.fromResultSet(rs))
                }
            }
        }
        return logs
    }

    /**
     * Get all actions by a specific player
     */
    fun getPlayerHistory(
        playerUuid: String,
        limit: Int = 50
    ): List<Any> {
        val history = mutableListOf<Any>()
        
        // Get block actions
        connection?.prepareStatement("""
            SELECT * FROM block_logs 
            WHERE player_uuid = ?
            ORDER BY timestamp DESC LIMIT ?
        """)?.use { pstmt ->
            pstmt.setString(1, playerUuid)
            pstmt.setInt(2, limit / 2)
            pstmt.executeQuery().use { rs ->
                while (rs.next()) {
                    history.add(BlockLog.fromResultSet(rs))
                }
            }
        }
        
        // Get chat messages
        connection?.prepareStatement("""
            SELECT * FROM chat_logs 
            WHERE player_uuid = ?
            ORDER BY timestamp DESC LIMIT ?
        """)?.use { pstmt ->
            pstmt.setString(1, playerUuid)
            pstmt.setInt(2, limit / 2)
            pstmt.executeQuery().use { rs ->
                while (rs.next()) {
                    history.add(ChatLog.fromResultSet(rs))
                }
            }
        }
        
        return history.sortedByDescending {
            when (it) {
                is BlockLog -> it.timestamp
                is ChatLog -> it.timestamp
                else -> 0L
            }
        }
    }

    /**
     * Rollback all actions by a player in a time range
     */
    fun rollbackPlayer(
        playerUuid: String,
        startTime: Long,
        endTime: Long
    ): Int {
        var count = 0
        connection?.prepareStatement("""
            SELECT * FROM block_logs 
            WHERE player_uuid = ? AND timestamp BETWEEN ? AND ?
            ORDER BY timestamp DESC
        """)?.use { pstmt ->
            pstmt.setString(1, playerUuid)
            pstmt.setLong(2, startTime)
            pstmt.setLong(3, endTime)
            pstmt.executeQuery().use { rs ->
                while (rs.next()) {
                    val log = BlockLog.fromResultSet(rs)
                    if (log.actionType == "PLACE" && !log.rolledBack) {
                        // Here you would restore the old block
                        // This is handled by the ProtectManager
                        count++
                        markAsRolledBack(log.id)
                    }
                }
            }
        }
        return count
    }

    /**
     * Mark a log entry as rolled back
     */
    private fun markAsRolledBack(id: Long) {
        connection?.prepareStatement("""
            UPDATE block_logs SET rolled_back = true WHERE id = ?
        """)?.use { pstmt ->
            pstmt.setLong(1, id)
            pstmt.executeUpdate()
        }
    }

    /**
     * Get recent chat messages
     */
    fun getRecentChat(limit: Int = 20): List<ChatLog> {
        val logs = mutableListOf<ChatLog>()
        connection?.prepareStatement("""
            SELECT * FROM chat_logs 
            ORDER BY timestamp DESC LIMIT ?
        """)?.use { pstmt ->
            pstmt.setInt(1, limit)
            pstmt.executeQuery().use { rs ->
                while (rs.next()) {
                    logs.add(ChatLog.fromResultSet(rs))
                }
            }
        }
        return logs
    }

    /**
     * Close the database connection
     */
    fun close() {
        connection?.close()
        connection = null
    }

    /**
     * Data class for block logs
     */
    data class BlockLog(
        val id: Long,
        val playerName: String,
        val playerUuid: String,
        val actionType: String,
        val worldName: String,
        val x: Int,
        val y: Int,
        val z: Int,
        val blockType: String,
        val blockData: Int,
        val oldBlockType: String?,
        val oldBlockData: Int?,
        val timestamp: Long,
        val rolledBack: Boolean
    ) {
        companion object {
            fun fromResultSet(rs: ResultSet): BlockLog {
                return BlockLog(
                    id = rs.getLong("id"),
                    playerName = rs.getString("player_name"),
                    playerUuid = rs.getString("player_uuid"),
                    actionType = rs.getString("action_type"),
                    worldName = rs.getString("world_name"),
                    x = rs.getInt("x"),
                    y = rs.getInt("y"),
                    z = rs.getInt("z"),
                    blockType = rs.getString("block_type"),
                    blockData = rs.getInt("block_data"),
                    oldBlockType = rs.getString("old_block_type"),
                    oldBlockData = rs.getInt("old_block_data"),
                    timestamp = rs.getLong("timestamp"),
                    rolledBack = rs.getBoolean("rolled_back")
                )
            }
        }
    }

    /**
     * Data class for chat logs
     */
    data class ChatLog(
        val id: Long,
        val playerName: String,
        val playerUuid: String,
        val message: String,
        val channel: String,
        val timestamp: Long
    ) {
        companion object {
            fun fromResultSet(rs: ResultSet): ChatLog {
                return ChatLog(
                    id = rs.getLong("id"),
                    playerName = rs.getString("player_name"),
                    playerUuid = rs.getString("player_uuid"),
                    message = rs.getString("message"),
                    channel = rs.getString("channel"),
                    timestamp = rs.getLong("timestamp")
                )
            }
        }
    }
}
