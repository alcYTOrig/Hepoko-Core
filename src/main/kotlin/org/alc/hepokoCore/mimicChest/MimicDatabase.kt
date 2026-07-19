package org.alc.hepokoCore.mimicChest

import java.io.File
import java.sql.Connection
import java.sql.DriverManager

object MimicDatabase {
    private var connection: Connection? = null

    fun setup(dataFolder: File) {
        val dataDirectory = File(dataFolder, "data")
        if (!dataDirectory.exists()) dataDirectory.mkdirs()

        val dbFile = File(dataDirectory, "mimic_chests.db")
        Class.forName("org.sqlite.JDBC")
        connection = DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}")

        connection?.createStatement()?.use { statement ->
            statement.execute(
                """
                CREATE TABLE IF NOT EXISTS player_placed_chests (
                    world TEXT NOT NULL,
                    x INTEGER NOT NULL,
                    y INTEGER NOT NULL,
                    z INTEGER NOT NULL,
                    PRIMARY KEY (world, x, y, z)
                );
                """.trimIndent()
            )
        }
    }

    fun markChestAsPlayerPlaced(world: String, x: Int, y: Int, z: Int) {
        val query = "INSERT OR IGNORE INTO player_placed_chests (world, x, y, z) VALUES (?, ?, ?, ?)"
        connection?.prepareStatement(query)?.use { preparedStatement ->
            preparedStatement.setString(1, world)
            preparedStatement.setInt(2, x)
            preparedStatement.setInt(3, y)
            preparedStatement.setInt(4, z)
            preparedStatement.executeUpdate()
        }
    }

    fun isPlayerPlacedChest(world: String, x: Int, y: Int, z: Int): Boolean {
        val query = "SELECT 1 FROM player_placed_chests WHERE world = ? AND x = ? AND y = ? AND z = ?"
        return connection?.prepareStatement(query)?.use { preparedStatement ->
            preparedStatement.setString(1, world)
            preparedStatement.setInt(2, x)
            preparedStatement.setInt(3, y)
            preparedStatement.setInt(4, z)
            preparedStatement.executeQuery().use { resultSet ->
                resultSet.next()
            }
        } ?: false
    }

    fun close() {
        runCatching { connection?.close() }
    }
}