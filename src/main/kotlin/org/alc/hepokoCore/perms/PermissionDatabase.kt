package org.alc.hepokoCore.perms

import java.io.File
import java.sql.Connection
import java.sql.DriverManager

object PermissionDatabase {
    private var connection: Connection? = null

    fun setup(dataFolder: File) {
        val dataDirectory = File(dataFolder, "data")
        if (!dataDirectory.exists()) dataDirectory.mkdirs()

        val dbFile = File(dataDirectory, "permissions.db")
        Class.forName("org.sqlite.JDBC")
        connection = DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}")

        connection?.createStatement()?.use { stmt ->
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS groups (
                    name TEXT PRIMARY KEY,
                    weight INTEGER DEFAULT 0
                )
            """.trimIndent())

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS group_permissions (
                    group_name TEXT,
                    node TEXT,
                    value INTEGER,
                    PRIMARY KEY (group_name, node)
                )
            """.trimIndent())

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    uuid TEXT PRIMARY KEY,
                    name TEXT,
                    primary_group TEXT DEFAULT 'default'
                )
            """.trimIndent())

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS user_permissions (
                    uuid TEXT,
                    node TEXT,
                    value INTEGER,
                    PRIMARY KEY (uuid, node)
                )
            """.trimIndent())
        }

        addGroup("default", 0)
    }

    fun addGroup(name: String, weight: Int) {
        connection?.prepareStatement("INSERT OR IGNORE INTO groups (name, weight) VALUES (?, ?)")?.use {
            it.setString(1, name.lowercase())
            it.setInt(2, weight)
            it.executeUpdate()
        }
    }

    // ✅ НОВЫЙ МЕТОД: Удаление группы
    fun removeGroup(name: String): Boolean {
        val groupName = name.lowercase()
        var removed = false

        // Сначала удаляем все права группы
        connection?.prepareStatement("DELETE FROM group_permissions WHERE group_name = ?")?.use {
            it.setString(1, groupName)
            it.executeUpdate()
        }

        // Затем удаляем саму группу
        connection?.prepareStatement("DELETE FROM groups WHERE name = ?")?.use {
            it.setString(1, groupName)
            removed = it.executeUpdate() > 0
        }

        return removed
    }

    fun addGroupPermission(groupName: String, node: String, value: Boolean) {
        connection?.prepareStatement("INSERT OR REPLACE INTO group_permissions (group_name, node, value) VALUES (?, ?, ?)")?.use {
            it.setString(1, groupName.lowercase())
            it.setString(2, node)
            it.setInt(3, if (value) 1 else 0)
            it.executeUpdate()
        }
    }

    // ✅ НОВЫЙ МЕТОД: Удаление права у группы
    fun removeGroupPermission(groupName: String, node: String): Boolean {
        var removed = false
        connection?.prepareStatement("DELETE FROM group_permissions WHERE group_name = ? AND node = ?")?.use {
            it.setString(1, groupName.lowercase())
            it.setString(2, node)
            removed = it.executeUpdate() > 0
        }
        return removed
    }

    fun addUserPermission(uuid: String, node: String, value: Boolean) {
        connection?.prepareStatement("INSERT OR REPLACE INTO user_permissions (uuid, node, value) VALUES (?, ?, ?)")?.use {
            it.setString(1, uuid)
            it.setString(2, node)
            it.setInt(3, if (value) 1 else 0)
            it.executeUpdate()
        }
    }

    // ✅ НОВЫЙ МЕТОД: Удаление права у пользователя
    fun removeUserPermission(uuid: String, node: String): Boolean {
        var removed = false
        connection?.prepareStatement("DELETE FROM user_permissions WHERE uuid = ? AND node = ?")?.use {
            it.setString(1, uuid)
            it.setString(2, node)
            removed = it.executeUpdate() > 0
        }
        return removed
    }

    fun getAllGroups(): List<GroupData> {
        val groups = mutableListOf<GroupData>()
        connection?.prepareStatement("SELECT name, weight FROM groups")?.use { stmt ->
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    groups.add(GroupData(rs.getString("name"), rs.getInt("weight")))
                }
            }
        }

        groups.forEach { group ->
            connection?.prepareStatement("SELECT node, value FROM group_permissions WHERE group_name = ?")?.use { stmt ->
                stmt.setString(1, group.name)
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        group.permissions.add(PermissionNode(rs.getString("node"), rs.getInt("value") == 1))
                    }
                }
            }
        }
        return groups
    }

    // ========== USER GROUP MANAGEMENT ==========

    fun getUserGroups(uuid: String): List<String> {
        val groups = mutableListOf<String>()
        connection?.prepareStatement("SELECT primary_group FROM users WHERE uuid = ?")?.use { stmt ->
            stmt.setString(1, uuid)
            stmt.executeQuery().use { rs ->
                if (rs.next()) {
                    groups.add(rs.getString("primary_group"))
                }
            }
        }
        return groups
    }

    fun setUserPrimaryGroup(uuid: String, groupName: String): Boolean {
        var updated = false
        connection?.prepareStatement("UPDATE users SET primary_group = ? WHERE uuid = ?")?.use { stmt ->
            stmt.setString(1, groupName.lowercase())
            stmt.setString(2, uuid)
            updated = stmt.executeUpdate() > 0
        }
        
        // If user doesn't exist, create them
        if (!updated) {
            connection?.prepareStatement("INSERT OR IGNORE INTO users (uuid, name, primary_group) VALUES (?, ?, ?)")?.use { stmt ->
                stmt.setString(1, uuid)
                stmt.setString(2, "unknown")
                stmt.setString(3, groupName.lowercase())
                stmt.executeUpdate()
            }
        }
        
        PermissionManager.loadCache()
        return true
    }

    fun addUserToGroup(uuid: String, groupName: String): Boolean {
        // For now, just set as primary group
        return setUserPrimaryGroup(uuid, groupName)
    }

    fun removeUserFromGroup(uuid: String, groupName: String): Boolean {
        var removed = false
        // Check if this is the user's primary group
        connection?.prepareStatement("SELECT primary_group FROM users WHERE uuid = ?")?.use { stmt ->
            stmt.setString(1, uuid)
            stmt.executeQuery().use { rs ->
                if (rs.next() && rs.getString("primary_group") == groupName.lowercase()) {
                    // Reset to default if removing primary group
                    connection?.prepareStatement("UPDATE users SET primary_group = 'default' WHERE uuid = ?")?.use { updateStmt ->
                        updateStmt.setString(1, uuid)
                        removed = updateStmt.executeUpdate() > 0
                    }
                }
            }
        }
        PermissionManager.loadCache()
        return removed
    }

    fun getAllUsers(): List<UserData> {
        val users = mutableListOf<UserData>()
        connection?.prepareStatement("SELECT uuid, name, primary_group FROM users")?.use { stmt ->
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    val uuid = try { java.util.UUID.fromString(rs.getString("uuid")) } catch (e: Exception) { continue }
                    val userData = UserData(uuid, rs.getString("name"))
                    userData.groups.add(rs.getString("primary_group"))
                    users.add(userData)
                }
            }
        }

        users.forEach { user ->
            connection?.prepareStatement("SELECT node, value FROM user_permissions WHERE uuid = ?")?.use { stmt ->
                stmt.setString(1, user.uuid.toString())
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        user.permissions.add(PermissionNode(rs.getString("node"), rs.getInt("value") == 1))
                    }
                }
            }
        }
        return users
    }

    fun close() {
        runCatching { connection?.close() }
    }
}