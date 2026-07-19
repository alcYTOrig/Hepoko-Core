package org.alc.hepokoCore.auth

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.requests.GatewayIntent
import org.bukkit.command.CommandExecutor
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.util.*
import org.bukkit.command.Command
import org.bukkit.command.CommandSender

import org.alc.hepokoCore.HepokoCore
import org.alc.hepokoCore.Configs

class AuthEveryDay(private val plugin: HepokoCore) : CommandExecutor {
    var dbConnection: Connection? = null
        private set
    var jda: JDA? = null
        private set

    val authenticatedPlayers: MutableSet<UUID> = Collections.synchronizedSet(HashSet())
    val pending2FA: MutableMap<UUID, String> = Collections.synchronizedMap(HashMap())
    val pendingTOTPSetup: MutableMap<UUID, String> = Collections.synchronizedMap(HashMap())
    val pendingTOTPAuth: MutableSet<UUID> = Collections.synchronizedSet(HashSet())
    val lastDisconnectTimes: MutableMap<UUID, Long> = Collections.synchronizedMap(HashMap())

    fun debugLog(message: String) {
        if (Configs.globalConfig.getBoolean("debug", false)) {
            plugin.logger.info("§d[Auth:DEBUG] $message")
        }
    }

    fun start() {
        if (!initDatabase()) {
            plugin.logger.severe("Выключение плагина из-за критической ошибки SQLite!")
            plugin.server.pluginManager.disablePlugin(plugin)
            return
        }
        if (Configs.authConfig.getBoolean("2fa_only", false) && !Configs.authConfig.getBoolean(
                "2fa_discord",
                true
            ) && !Configs.authConfig.getBoolean("2fa_app", true)
        ) {
            plugin.logger.severe("Недопустимые значения!")
        }
        if (Configs.authConfig.getBoolean("2fa_enabled", true) && Configs.authConfig.getBoolean("2fa_discord", true)) {
            initDiscordBot()
        }
        plugin.server.pluginManager.registerEvents(PlayerListener(this), plugin)
        val commands = AuthCommands(plugin, this)
        plugin.getCommand("register")?.setExecutor(commands)
        plugin.getCommand("login")?.setExecutor(commands)
        plugin.getCommand("2fa_discord")?.setExecutor(commands)
        plugin.getCommand("2fa_app")?.setExecutor(commands)
        plugin.logger.info("Hepoko:AuthEveryDay успешно запущен!")
    }

    fun stop() {
        runCatching {
            dbConnection?.close()
            jda?.shutdown()
        }
    }

    private fun initDatabase(): Boolean {
        return runCatching {
            val dataDirectory = File(plugin.dataFolder, "data")
            if (!dataDirectory.exists()) {
                dataDirectory.mkdirs() // Создаем папку data, если её нет
            }
            val dbFile = File(dataDirectory, "auth.db")
            dbConnection = DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}")
            dbConnection?.createStatement().use { statement ->
                statement?.execute(
                    """
                CREATE TABLE IF NOT EXISTS users (
                    username TEXT PRIMARY KEY,
                    password_hash TEXT NOT NULL,
                    discord_id TEXT NOT NULL,
                    totp_secret TEXT DEFAULT '',
                    is_enabled INTEGER DEFAULT 1
                );
            """.trimIndent()
                )
                statement?.execute(
                    """
                CREATE TABLE IF NOT EXISTS blocked_ips (
                    ip TEXT PRIMARY KEY,
                    blocked_by TEXT NOT NULL,
                    timestamp TEXT NOT NULL
                );
            """.trimIndent()
                )
            }
            true
        }.getOrElse {
            plugin.logger.severe("Ошибка БД: ${it.message}")
            false
        }
    }

    private fun initDiscordBot() {
        val token = Configs.authConfig.getString("discord-token") ?: return
        runCatching {
            jda = JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.DIRECT_MESSAGES)
                .addEventListeners(DiscordButtonListener(plugin, this))
                .build().awaitReady()
        }.onFailure {
            plugin.logger.severe("Ошибка запуска Дискорд бота: ${it.message}")
        }
    }

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        TODO("Not yet implemented")
    }
}
