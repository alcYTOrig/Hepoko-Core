package org.alc.hepokoCore.protect

import org.alc.hepokoCore.HepokoCore
import org.bukkit.plugin.java.JavaPlugin

/**
 * Main manager class for CoreProtect module.
 * Handles initialization, registration of listeners and commands.
 */
class ProtectManager(private val plugin: JavaPlugin) {
    private val database = ProtectDatabase
    private val listener = ProtectListener(database)
    private val commands = ProtectCommands(database)

    /**
     * Initialize the CoreProtect module
     */
    fun init() {
        // Setup database
        database.setup(plugin.dataFolder)

        // Register listener
        plugin.server.pluginManager.registerEvents(listener, plugin)

        // Register commands
        plugin.getCommand("coreprotect")?.setExecutor(commands)
        plugin.getCommand("co")?.setExecutor(commands)

        plugin.logger.info("[CoreProtect] Модуль защи 당연히 загружен!")
    }

    /**
     * Shutdown the CoreProtect module
     */
    fun shutdown() {
        database.close()
        plugin.logger.info("[CoreProtect] Модуль выключен. База данных закрыта.")
    }

    /**
     * Get the ProtectDatabase instance
     */
    fun getDatabase(): ProtectDatabase {
        return database
    }
}
