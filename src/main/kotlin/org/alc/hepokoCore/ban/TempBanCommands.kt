package org.alc.hepokoCore.ban

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.alc.hepokoCore.utils.TimeParser

class TempBanCommands : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val isIpBanCommand = command.name.equals("tempban-ip", ignoreCase = true)
        val usageText = if (isIpBanCommand) "/tempban-ip <игрок/IP> <время> [причина]" else "/tempban <игрок> <время> [причина]"

        if (args.size < 2) {
            sender.sendMessage(Component.text("Использование: $usageText", NamedTextColor.RED))
            return true
        }

        val targetInput = args[0]
        val expiryDate = TimeParser.parseTimeString(args[1])
        if (expiryDate == null) {
            sender.sendMessage(Component.text("Неверный формат времени! Пример: 10m, 2h", NamedTextColor.RED))
            return true
        }

        val reason = if (args.size > 2) args.drop(2).joinToString(" ") else "Нарушение правил"
        val bannerName = sender.name

        if (isIpBanCommand) {
            val ipAddress = if (targetInput.contains(".")) {
                targetInput
            } else {
                Bukkit.getPlayer(targetInput)?.address?.address?.hostAddress
            }

            if (ipAddress == null) {
                sender.sendMessage(Component.text("Не удалось определить IP-адрес!", NamedTextColor.RED))
                return true
            }

            DatabaseManager.addBan(ipAddress, reason, expiryDate, bannerName)

            Bukkit.getOnlinePlayers().forEach { player ->
                val playerIp = player.address?.address?.hostAddress
                if (playerIp == ipAddress) {
                    player.kick(
                        Component.text()
                            .append(Component.text("Ваш IP-адрес был временно забанен!\n", NamedTextColor.RED))
                            .append(Component.text("Причина: ", NamedTextColor.GRAY))
                            .append(Component.text(reason, NamedTextColor.WHITE))
                            .append(Component.text("\nДо: ", NamedTextColor.GRAY))
                            .append(Component.text(expiryDate.toString(), NamedTextColor.YELLOW))
                            .build()
                    )
                }
            }

            Bukkit.broadcast(
                Component.text()
                    .append(Component.text("IP-адрес ", NamedTextColor.RED))
                    .append(Component.text(ipAddress, NamedTextColor.YELLOW))
                    .append(Component.text(" забанен админом ", NamedTextColor.RED))
                    .append(Component.text(bannerName, NamedTextColor.GOLD))
                    .append(Component.text(" до $expiryDate. Причина: $reason", NamedTextColor.RED))
                    .build()
            )
        } else {
            DatabaseManager.addBan(targetInput, reason, expiryDate, bannerName)

            val targetPlayer = Bukkit.getPlayer(targetInput)
            if (targetPlayer != null && targetPlayer.isOnline) {
                targetPlayer.kick(
                    Component.text()
                        .append(Component.text("Вы были временно забанены!\n", NamedTextColor.RED))
                        .append(Component.text("Причина: ", NamedTextColor.GRAY))
                        .append(Component.text(reason, NamedTextColor.WHITE))
                        .append(Component.text("\nДо: ", NamedTextColor.GRAY))
                        .append(Component.text(expiryDate.toString(), NamedTextColor.YELLOW))
                        .build()
                )
            }

            Bukkit.broadcast(
                Component.text()
                    .append(Component.text("Игрок ", NamedTextColor.RED))
                    .append(Component.text(targetInput, NamedTextColor.YELLOW))
                    .append(Component.text(" забанен админом ", NamedTextColor.RED))
                    .append(Component.text(bannerName, NamedTextColor.GOLD))
                    .append(Component.text(" до $expiryDate. Причина: $reason", NamedTextColor.RED))
                    .build()
            )
        }
        return true
    }
}