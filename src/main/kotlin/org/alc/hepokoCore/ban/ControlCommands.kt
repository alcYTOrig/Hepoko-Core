package org.alc.hepokoCore.ban

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.alc.hepokoCore.utils.TimeParser
import org.alc.hepokoCore.perms.PermissionManager
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class TempMuteCommand : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        // ✅ ПРОВЕРКА ПРАВ ЧЕРЕЗ HEPOKOPERMS
        if (sender is Player) {
            if (!PermissionManager.hasPermission(sender, "hepokocore.tempmute")) {
                sender.sendMessage(Component.text("У вас нет прав!", NamedTextColor.RED))
                return true
            }
        }

        if (args.size < 2) {
            sender.sendMessage(Component.text("Использование: /tempmute <игрок> <время(1m/2h/3d)> [причина]", NamedTextColor.RED))
            return true
        }

        val targetName = args[0]
        val expiryDate = TimeParser.parseTimeString(args[1])
        if (expiryDate == null) {
            sender.sendMessage(Component.text("Неверный формат времени! Пример: 30m, 1h", NamedTextColor.RED))
            return true
        }

        val reason = if (args.size > 2) args.drop(2).joinToString(" ") else "Нарушение правил"
        val muterName = sender.name

        DatabaseManager.addMute(targetName, reason, expiryDate, muterName)

        val targetPlayer = Bukkit.getPlayer(targetName)
        if (targetPlayer != null && targetPlayer.isOnline) {
            targetPlayer.sendMessage(
                Component.text()
                    .append(Component.text("Вы были временно замучены админом ", NamedTextColor.RED))
                    .append(Component.text(muterName, NamedTextColor.GOLD))
                    .append(Component.text(" до ", NamedTextColor.RED))
                    .append(Component.text(expiryDate.toString(), NamedTextColor.YELLOW))
                    .append(Component.text(". Причина: ", NamedTextColor.RED))
                    .append(Component.text(reason, NamedTextColor.WHITE))
                    .build()
            )
        }

        Bukkit.broadcast(
            Component.text()
                .append(Component.text("Игрок ", NamedTextColor.RED))
                .append(Component.text(targetName, NamedTextColor.YELLOW))
                .append(Component.text(" замучен админом ", NamedTextColor.RED))
                .append(Component.text(muterName, NamedTextColor.GOLD))
                .append(Component.text(" до ", NamedTextColor.RED))
                .append(Component.text(expiryDate.toString(), NamedTextColor.YELLOW))
                .append(Component.text(". Причина: ", NamedTextColor.RED))
                .append(Component.text(reason, NamedTextColor.WHITE))
                .build()
        )
        return true
    }
}

class UnmuteCommand : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        // ✅ ПРОВЕРКА ПРАВ ЧЕРЕЗ HEPOKOPERMS
        if (sender is Player) {
            if (!PermissionManager.hasPermission(sender, "hepokocore.unmute")) {
                sender.sendMessage(Component.text("У вас нет прав!", NamedTextColor.RED))
                return true
            }
        }

        if (args.isEmpty()) {
            sender.sendMessage(Component.text("Использование: /unmute <игрок>", NamedTextColor.RED))
            return true
        }

        val targetName = args[0]
        val removed = DatabaseManager.removeMute(targetName)

        if (removed) {
            sender.sendMessage(
                Component.text()
                    .append(Component.text("Игрок ", NamedTextColor.GREEN))
                    .append(Component.text(targetName, NamedTextColor.WHITE))
                    .append(Component.text(" успешно размучен в СУБД.", NamedTextColor.GREEN))
                    .build()
            )
            val targetPlayer = Bukkit.getPlayer(targetName)
            targetPlayer?.sendMessage(Component.text("Вы были размучены администратором.", NamedTextColor.GREEN))
        } else {
            sender.sendMessage(
                Component.text()
                    .append(Component.text("Активных мутов для игрока ", NamedTextColor.RED))
                    .append(Component.text(targetName, NamedTextColor.WHITE))
                    .append(Component.text(" не найдено.", NamedTextColor.RED))
                    .build()
            )
        }
        return true
    }
}

class BroadcastCommand : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        // ✅ ПРОВЕРКА ПРАВ ЧЕРЕЗ HEPOKOPERMS
        if (sender is Player) {
            if (!PermissionManager.hasPermission(sender, "hepokocore.broadcast")) {
                sender.sendMessage(Component.text("У вас нет прав!", NamedTextColor.RED))
                return true
            }
        }

        if (args.isEmpty()) {
            sender.sendMessage(Component.text("Использование: /broadcast <сообщение>", NamedTextColor.RED))
            return true
        }

        val rawMessage = args.joinToString(" ")
        val formattedComponent = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(rawMessage)

        val announceComponent = Component.text()
            .append(Component.text("[ОБЪЯВЛЕНИЕ] ", NamedTextColor.DARK_RED).decorate(TextDecoration.BOLD))
            .append(formattedComponent)
            .build()

        Bukkit.broadcast(announceComponent)
        return true
    }
}