package org.alc.hepokoCore.ban

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class UnbanCommand : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage(Component.text("Использование: /unban <никнейм>", NamedTextColor.RED))
            return true
        }
        val target = args[0]
        val removed = DatabaseManager.removeBan(target)
        if (removed) {
            sender.sendMessage(
                Component.text()
                    .append(Component.text("Игрок ", NamedTextColor.GREEN))
                    .append(Component.text(target, NamedTextColor.WHITE))
                    .append(Component.text(" успешно разбанен в СУБД.", NamedTextColor.GREEN))
                    .build()
            )
        } else {
            sender.sendMessage(
                Component.text()
                    .append(Component.text("Активных банов для ", NamedTextColor.RED))
                    .append(Component.text(target, NamedTextColor.WHITE))
                    .append(Component.text(" не найдено.", NamedTextColor.RED))
                    .build()
            )
        }
        return true
    }
}

class UnbanIpCommand : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage(Component.text("Использование: /unban-ip <IP-адрес>", NamedTextColor.RED))
            return true
        }
        val target = args[0]
        val removed = DatabaseManager.removeBan(target)
        if (removed) {
            sender.sendMessage(
                Component.text()
                    .append(Component.text("IP-адрес ", NamedTextColor.GREEN))
                    .append(Component.text(target, NamedTextColor.WHITE))
                    .append(Component.text(" успешно разбанен в СУБД.", NamedTextColor.GREEN))
                    .build()
            )
        } else {
            sender.sendMessage(
                Component.text()
                    .append(Component.text("Активных банов для IP ", NamedTextColor.RED))
                    .append(Component.text(target, NamedTextColor.WHITE))
                    .append(Component.text(" не найдено.", NamedTextColor.RED))
                    .build()
            )
        }
        return true
    }
}