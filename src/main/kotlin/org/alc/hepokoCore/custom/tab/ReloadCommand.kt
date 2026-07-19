package org.alc.hepokoCore.custom.tab

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.alc.hepokoCore.HepokoCore
import org.alc.hepokoCore.perms.PermissionManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

class ReloadCommand(private val plugin: HepokoCore) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        // ✅ ПРОВЕРКА ПРАВ ЧЕРЕЗ HEPOKOPERMS
        if (sender is Player) {
            if (!PermissionManager.hasPermission(sender, "hepokocore.admin")) {
                sender.sendMessage(Component.text("У вас нет прав!", NamedTextColor.RED))
                return true
            }
        }

        sender.sendMessage(Component.text("[HepokoCore] Запуск процесса полной перезагрузки модулей...", NamedTextColor.GRAY))
        try {
            plugin.preLoadConfig()
            sender.sendMessage(Component.text("✔ Файлы модулей (GlobalConfig, AuthConfig, ControlConfig и т.д.) успешно перечитаны с диска.", NamedTextColor.GREEN))
        } catch (e: Exception) {
            sender.sendMessage(Component.text("✖ Произошла критическая ошибка при перезагрузке: ${e.message}", NamedTextColor.RED))
            e.printStackTrace()
        }
        return true
    }
}

class ReloadCommandTAB(private val tabManager: TabManager) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        // ✅ ПРОВЕРКА ПРАВ ЧЕРЕЗ HEPOKOPERMS
        if (sender is Player) {
            if (!PermissionManager.hasPermission(sender, "hepokocore.admin")) {
                sender.sendMessage(Component.text("У вас нет прав!", NamedTextColor.RED))
                return true
            }
        }

        sender.sendMessage(Component.text("[HepokoCore] Запуск процесса полной перезагрузки модулей...", NamedTextColor.GRAY))
        try {
            tabManager.reload()
            sender.sendMessage(Component.text("✔ Конфигурация TAB (CustomConfig.yml) и движок (AnimationsTabConfig.yml) успешно перезапущены.", NamedTextColor.GREEN))
            sender.sendMessage(Component.text("[HepokoCore] Все элементы и кастомный TAB были успешно обновлены в рантайме!", NamedTextColor.GOLD))
        } catch (e: Exception) {
            sender.sendMessage(Component.text("✖ Произошла критическая ошибка при перезагрузке: ${e.message}", NamedTextColor.RED))
            e.printStackTrace()
        }
        return true
    }
}