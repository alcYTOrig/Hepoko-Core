package org.alc.hepokoCore.ban

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import java.text.SimpleDateFormat

class BanListCommands : CommandExecutor {
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss")

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        when (command.name.lowercase()) {
            "banlist" -> showList(sender, isIp = false)
            "banlist-ip" -> showList(sender, isIp = true)
            "baninfo" -> showInfo(sender, args)
        }
        return true
    }

    private fun showList(sender: CommandSender, isIp: Boolean) {
        val bans = DatabaseManager.getActiveBans(isIp)
        val typeStr = if (isIp) "IP-адресов" else "игроков"

        if (bans.isEmpty()) {
            sender.sendMessage(Component.text("Список активных банов $typeStr пуст.", NamedTextColor.YELLOW))
            return
        }

        sender.sendMessage(Component.text("=== Список активных банов $typeStr (${bans.size}) ===", NamedTextColor.GOLD))
        bans.forEach { ban ->
            sender.sendMessage(
                Component.text()
                    .append(Component.text("• ", NamedTextColor.RED))
                    .append(Component.text(ban.target, NamedTextColor.WHITE))
                    .append(Component.text(" – Администратор: ", NamedTextColor.GRAY))
                    .append(Component.text(ban.banner, NamedTextColor.GOLD))
                    .append(Component.text(" – Причина: ", NamedTextColor.GRAY))
                    .append(Component.text(ban.reason, NamedTextColor.WHITE))
                    .append(Component.text(" (До: ", NamedTextColor.GRAY))
                    .append(Component.text(dateFormat.format(ban.expiryDate), NamedTextColor.YELLOW))
                    .append(Component.text(")", NamedTextColor.GRAY))
                    .build()
            )
        }
    }

    private fun showInfo(sender: CommandSender, args: Array<out String>) {
        if (args.isEmpty()) {
            sender.sendMessage(Component.text("Использование: /baninfo <никнейм или IP>", NamedTextColor.RED))
            return
        }
        val target = args[0]
        val banInfo = DatabaseManager.checkBan(target)

        if (banInfo == null) {
            sender.sendMessage(
                Component.text()
                    .append(Component.text("Объект ", NamedTextColor.GREEN))
                    .append(Component.text(target, NamedTextColor.WHITE))
                    .append(Component.text(" не имеет активных банов в СУБД.", NamedTextColor.GREEN))
                    .build()
            )
        } else {
            sender.sendMessage(Component.text("=== Информация о блокировке: $target ===", NamedTextColor.GOLD))
            sender.sendMessage(
                Component.text()
                    .append(Component.text("Забанил: ", NamedTextColor.GRAY))
                    .append(Component.text(banInfo.banner, NamedTextColor.RED))
                    .build()
            )
            sender.sendMessage(
                Component.text()
                    .append(Component.text("Причина: ", NamedTextColor.GRAY))
                    .append(Component.text(banInfo.reason, NamedTextColor.WHITE))
                    .build()
            )
            sender.sendMessage(
                Component.text()
                    .append(Component.text("Действует до: ", NamedTextColor.GRAY))
                    .append(Component.text(dateFormat.format(banInfo.expiryDate), NamedTextColor.YELLOW))
                    .build()
            )
        }
    }
}