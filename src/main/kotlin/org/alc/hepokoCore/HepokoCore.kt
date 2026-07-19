package org.alc.hepokoCore

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.java.JavaPlugin
import org.alc.hepokoCore.auth.AuthEveryDay
import org.alc.hepokoCore.custom.CustomChat
import org.alc.hepokoCore.ban.DatabaseManager
import org.alc.hepokoCore.ban.BanListener
import org.alc.hepokoCore.ban.UnbanCommand
import org.alc.hepokoCore.ban.UnbanIpCommand
import org.alc.hepokoCore.ban.BanListCommands
import org.alc.hepokoCore.ban.TempBanCommands
import org.alc.hepokoCore.ban.TempMuteCommand
import org.alc.hepokoCore.ban.UnmuteCommand
import org.alc.hepokoCore.ban.BroadcastCommand
import org.alc.hepokoCore.custom.tab.AnimationManager
import org.alc.hepokoCore.custom.tab.ReloadCommand
import org.alc.hepokoCore.custom.tab.TabManager
import org.alc.hepokoCore.custom.tab.ReloadCommandTAB
import org.alc.hepokoCore.perms.PermissionDatabase
import org.alc.hepokoCore.perms.PermissionManager
import org.alc.hepokoCore.perms.HepokoPermsCommand
import org.alc.hepokoCore.perms.HepokoVaultPermissions
import net.milkbowl.vault.permission.Permission
import org.bukkit.Bukkit

object Configs {
    lateinit var authConfig: FileConfiguration
        private set
    lateinit var globalConfig: FileConfiguration
        private set
    lateinit var mimicConfig: FileConfiguration
        private set
    lateinit var controlConfig: FileConfiguration
        private set
    lateinit var customConfig: FileConfiguration
        private set
    lateinit var animationsTabConfig: FileConfiguration
        private set

    fun loadAll(manager: ConfigManager) {
        authConfig = manager.loadConfig("AuthConfig.yml")
        globalConfig = manager.loadConfig("GlobalConfig.yml")
        mimicConfig = manager.loadConfig("MimicConfig.yml")
        controlConfig = manager.loadConfig("ControlConfig.yml")
        customConfig = manager.loadConfig("CustomConfig.yml")
        animationsTabConfig = manager.loadConfig("AnimationsTabConfig.yml")
    }
}

class HepokoCore : JavaPlugin() {
    val configManager = ConfigManager(this)
    lateinit var authEveryDay: AuthEveryDay
    private var vaultPermission: Permission? = null

    private lateinit var animationManager: AnimationManager
    lateinit var tabManager: TabManager
        private set

    override fun onEnable() {
        preLoadConfig()

        org.alc.hepokoCore.report.ReportDatabase.setup(dataFolder)

        // Инициализация системы прав
        PermissionDatabase.setup(dataFolder)
        PermissionManager.loadCache()
        PermissionManager.loadAllUsers()
        getCommand("hperm")?.setExecutor(HepokoPermsCommand())

        // ✅ РЕГИСТРАЦИЯ VAULT-ПРОВАЙДЕРА
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            vaultPermission = HepokoVaultPermissions()
            if (vaultPermission != null) {
                logger.info("[HepokoPerms] Vault найден! Регистрируем провайдер прав...")
                // Vault автоматически зарегистрирует наш провайдер
            }
        } else {
            logger.warning("[HepokoPerms] Vault не найден! Права будут работать только внутри плагина.")
        }

        initControl()
        initAuthEveryDay()
        if (!this.isEnabled) return

        initMimic()
        initTab()

        server.pluginManager.registerEvents(CustomChat(), this)
        DatabaseManager.setup(dataFolder)
        server.pluginManager.registerEvents(BanListener(), this)
        getCommand("heporeload")?.setExecutor(ReloadCommand(this))
    }

    override fun onDisable() {
        if (::authEveryDay.isInitialized) authEveryDay.stop()
        org.alc.hepokoCore.report.ReportDatabase.close()
        DatabaseManager.close()
        org.alc.hepokoCore.mimicChest.MimicDatabase.close()

        if (::tabManager.isInitialized) tabManager.stop()
    }

    fun preLoadConfig() {
        Configs.loadAll(configManager)
    }

    fun initTab() {
        if (Configs.globalConfig.getBoolean("TabManager", true)) {
            animationManager = AnimationManager(this)
            tabManager = TabManager(this, animationManager)
            tabManager.start()
            getCommand("tabreload")?.setExecutor(ReloadCommandTAB(tabManager))
        }
    }

    fun initAuthEveryDay() {
        if (!Configs.globalConfig.getBoolean("AuthEveryDay", true)) {
            logger.info("AuthEveryDay disabled in GlobalConfig")
            return
        }
        authEveryDay = AuthEveryDay(this)
        authEveryDay.start()
    }

    fun initControl() {
        if (!Configs.globalConfig.getBoolean("Control", true)) {
            logger.info("Control disabled in GlobalConfig")
            return
        }
        if (Configs.controlConfig.getBoolean("enabled-ban", true)) {
            val tempBanExecutor = TempBanCommands()
            if (Configs.controlConfig.getBoolean("ban.tempban", true)) {
                getCommand("tempban")?.setExecutor(tempBanExecutor)
            }
            if (Configs.controlConfig.getBoolean("ban.tempban-ip", true)) {
                getCommand("tempban-ip")?.setExecutor(tempBanExecutor)
            }
            if (Configs.controlConfig.getBoolean("ban.unban", true)) {
                getCommand("unban")?.setExecutor(UnbanCommand())
            }
            if (Configs.controlConfig.getBoolean("ban.unban-ip", true)) {
                getCommand("unban-ip")?.setExecutor(UnbanIpCommand())
            }
            val banListExecutor = BanListCommands()
            if (Configs.controlConfig.getBoolean("ban.banlist", true)) {
                getCommand("banlist")?.setExecutor(banListExecutor)
            }
            if (Configs.controlConfig.getBoolean("ban.banlist-ip", true)) {
                getCommand("banlist-ip")?.setExecutor(banListExecutor)
            }
            if (Configs.controlConfig.getBoolean("ban.baninfo", true)) {
                getCommand("baninfo")?.setExecutor(banListExecutor)
            }
        }
        if (Configs.controlConfig.getBoolean("enabled-mute", true)) {
            if (Configs.controlConfig.getBoolean("mute.tempmute", true)) {
                getCommand("tempmute")?.setExecutor(TempMuteCommand())
            }
            if (Configs.controlConfig.getBoolean("mute.unmute", true)) {
                getCommand("unmute")?.setExecutor(UnmuteCommand())
            }
            if (Configs.controlConfig.getBoolean("mute.tempmute", true)) {
                server.pluginManager.registerEvents(org.alc.hepokoCore.ban.MuteListener(), this)
            }
        }
        if (Configs.controlConfig.getBoolean("broadcast", true)) {
            getCommand("broadcast")?.setExecutor(BroadcastCommand())
        }
        if (Configs.controlConfig.getBoolean("enabled-report", true)) {
            if (Configs.controlConfig.getBoolean("report.report", true)) {
                getCommand("report")?.setExecutor(org.alc.hepokoCore.report.ReportCommand())
            }
            if (Configs.controlConfig.getBoolean("report.reportlist", true)) {
                getCommand("reportlist")?.setExecutor(org.alc.hepokoCore.report.ReportListCommand())
                server.pluginManager.registerEvents(org.alc.hepokoCore.report.ReportGuiListener(), this)
            }
        }
    }

    fun initMimic() {
        if (!Configs.globalConfig.getBoolean("MimicChest", false)) {
            logger.info("MimicChest disabled in GlobalConfig")
            return
        }
        org.alc.hepokoCore.mimicChest.MimicDatabase.setup(dataFolder)
        val mimicModule = org.alc.hepokoCore.mimicChest.MimicChestModule(this)
        mimicModule.loadConfig()
        server.pluginManager.registerEvents(org.alc.hepokoCore.mimicChest.MimicListener(mimicModule, this), this)
    }
}