package org.alc.hepokoCore.report

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.alc.hepokoCore.perms.PermissionManager

class ReportCommand : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(Component.text("Команда только для игроков!", NamedTextColor.RED))
            return true
        }

        // ✅ ПРОВЕРКА ПРАВ ЧЕРЕЗ HEPOKOPERMS
        if (!PermissionManager.hasPermission(sender, "hepokocore.report")) {
            sender.sendMessage(Component.text("У вас нет прав!", NamedTextColor.RED))
            return true
        }

        if (args.size < 2) {
            sender.sendMessage(Component.text("Использование: /report <ник_нарушителя> <причина>", NamedTextColor.RED))
            return true
        }

        val targetName = args[0]
        val reason = args.drop(1).joinToString(" ")

        if (sender.name.equals(targetName, ignoreCase = true)) {
            sender.sendMessage(Component.text("Вы не можете подать жалобу на самого себя!", NamedTextColor.RED))
            return true
        }

        if (ReportDatabase.hasActiveReport(sender.name, targetName)) {
            sender.sendMessage(
                Component.text()
                    .append(Component.text("Вы уже отправили жалобу на игрока ", NamedTextColor.RED))
                    .append(Component.text(targetName, NamedTextColor.WHITE))
                    .append(Component.text(". Дождитесь, пока администрация рассмотрит её!", NamedTextColor.RED))
                    .build()
            )
            return true
        }

        ReportDatabase.createReport(sender.name, targetName, reason)
        sender.sendMessage(
            Component.text()
                .append(Component.text("Ваша жалоба на игрока ", NamedTextColor.GREEN))
                .append(Component.text(targetName, NamedTextColor.WHITE))
                .append(Component.text(" успешно отправлена администрации!", NamedTextColor.GREEN))
                .build()
        )

        Bukkit.getOnlinePlayers().forEach { staff ->
            if (staff.hasPermission("hepokocore.admin") || PermissionManager.hasPermission(staff, "hepokocore.reportlist")) {
                staff.sendMessage(
                    Component.text()
                        .append(Component.text("[ЖАЛОБА] ", NamedTextColor.DARK_RED))
                        .append(Component.text(sender.name, NamedTextColor.YELLOW))
                        .append(Component.text(" пожаловался на ", NamedTextColor.GRAY))
                        .append(Component.text(targetName, NamedTextColor.RED))
                        .append(Component.text(": $reason", NamedTextColor.GRAY))
                        .build()
                )
            }
        }
        return true
    }
}

class ReportListCommand : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(Component.text("Эту команду можно использовать только в игре!", NamedTextColor.RED))
            return true
        }

        // ✅ ПРОВЕРКА ПРАВ ЧЕРЕЗ HEPOKOPERMS
        if (!PermissionManager.hasPermission(sender, "hepokocore.reportlist")) {
            sender.sendMessage(Component.text("У вас нет прав!", NamedTextColor.RED))
            return true
        }

        openReportGui(sender)
        return true
    }

    private fun openReportGui(player: Player) {
        val reports = ReportDatabase.getActiveReports()
        val inv = Bukkit.createInventory(
            null,
            54,
            Component.text()
                .append(Component.text("Список активных жалоб", NamedTextColor.DARK_RED))
                .build()
        )

        val grayPanel = ItemStack(Material.GRAY_STAINED_GLASS_PANE)
        val panelMeta = grayPanel.itemMeta
        panelMeta?.displayName(Component.text(" "))
        grayPanel.itemMeta = panelMeta

        for (i in 45..53) {
            inv.setItem(i, grayPanel)
        }

        reports.take(45).forEachIndexed { index, report ->
            val skull = ItemStack(Material.PLAYER_HEAD)
            val meta = skull.itemMeta as? SkullMeta
            if (meta != null) {
                meta.displayName(
                    Component.text()
                        .append(Component.text("Жалоба #${report.id}", NamedTextColor.RED))
                        .build()
                )
                meta.owningPlayer = Bukkit.getOfflinePlayer(report.target)

                val statusColor = if (report.status == "OPEN") NamedTextColor.GREEN else NamedTextColor.YELLOW

                meta.lore(
                    listOf(
                        Component.text()
                            .append(Component.text("Отправитель: ", NamedTextColor.GRAY))
                            .append(Component.text(report.reporter, NamedTextColor.WHITE))
                            .build(),
                        Component.text()
                            .append(Component.text("Нарушитель: ", NamedTextColor.GRAY))
                            .append(Component.text(report.target, NamedTextColor.GOLD))
                            .build(),
                        Component.text()
                            .append(Component.text("Причина: ", NamedTextColor.GRAY))
                            .append(Component.text(report.reason, NamedTextColor.YELLOW))
                            .build(),
                        Component.text()
                            .append(Component.text("Статус: ", NamedTextColor.GRAY))
                            .append(Component.text(report.status, statusColor))
                            .build(),
                        Component.text()
                            .append(Component.text("Взял: ", NamedTextColor.GRAY))
                            .append(Component.text(if (report.moderator.isEmpty()) "Никто" else report.moderator, NamedTextColor.AQUA))
                            .build(),
                        Component.text(" "),
                        Component.text()
                            .append(Component.text("• ЛКМ ", NamedTextColor.GREEN))
                            .append(Component.text("— Взять в работу", NamedTextColor.GRAY))
                            .build(),
                        Component.text()
                            .append(Component.text("• ПКМ ", NamedTextColor.RED))
                            .append(Component.text("— Закрыть жалобу", NamedTextColor.GRAY))
                            .build(),
                        Component.text()
                            .append(Component.text("• Shift + Клик ", NamedTextColor.GOLD))
                            .append(Component.text("— Телепорт к нарушителю", NamedTextColor.GRAY))
                            .build()
                    )
                )
                skull.itemMeta = meta
            }
            inv.setItem(index, skull)
        }
        player.openInventory(inv)
    }
}