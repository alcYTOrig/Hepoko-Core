package org.alc.hepokoCore.report

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent

class ReportGuiListener : Listener {
    @EventHandler
    fun onGuiClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val title = event.view.title()

        val plainTitle = PlainTextComponentSerializer.plainText().serialize(title)
        if (!plainTitle.contains("Список активных жалоб")) return

        event.isCancelled = true

        val clickedItem = event.currentItem ?: return
        if (clickedItem.type != Material.PLAYER_HEAD) return

        val meta = clickedItem.itemMeta ?: return
        val displayName = meta.displayName()?.let { PlainTextComponentSerializer.plainText().serialize(it) } ?: return

        val id = displayName.replace("Жалоба #", "").trim().toIntOrNull() ?: return

        val lore = meta.lore() ?: return
        var targetName = ""

        lore.forEach { line ->
            val lineText = PlainTextComponentSerializer.plainText().serialize(line)
            if (lineText.contains("Нарушитель:")) {
                targetName = lineText.replace("Нарушитель:", "").trim()
            }
        }

        val clickType = event.click

        if (clickType.isShiftClick) {
            val targetPlayer = Bukkit.getPlayer(targetName)
            if (targetPlayer != null && targetPlayer.isOnline) {
                player.teleport(targetPlayer.location)
                player.sendMessage(
                    Component.text()
                        .append(Component.text("Вы телепортировались к игроку ", NamedTextColor.GREEN))
                        .append(Component.text(targetName, NamedTextColor.WHITE))
                        .append(Component.text(" для проверки.", NamedTextColor.GREEN))
                        .build()
                )
                player.closeInventory()
            } else {
                player.sendMessage(
                    Component.text()
                        .append(Component.text("Игрок ", NamedTextColor.RED))
                        .append(Component.text(targetName, NamedTextColor.WHITE))
                        .append(Component.text(" сейчас оффлайн. Телепортация невозможна.", NamedTextColor.RED))
                        .build()
                )
            }
            return
        }

        if (clickType.isLeftClick) {
            if (ReportDatabase.acceptReport(id, player.name)) {
                player.sendMessage(
                    Component.text()
                        .append(Component.text("Вы взяли в работу жалобу #$id на игрока ", NamedTextColor.GREEN))
                        .append(Component.text(targetName, NamedTextColor.WHITE))
                        .append(Component.text(".", NamedTextColor.GREEN))
                        .build()
                )
                player.performCommand("reportlist")
            } else {
                player.sendMessage(Component.text("Не удалось взять жалобу. Возможно, она уже обрабатывается или закрыта.", NamedTextColor.RED))
            }
        } else if (clickType.isRightClick) {
            if (ReportDatabase.closeReport(id, player.name)) {
                player.sendMessage(
                    Component.text()
                        .append(Component.text("Жалоба #$id на игрока ", NamedTextColor.GREEN))
                        .append(Component.text(targetName, NamedTextColor.WHITE))
                        .append(Component.text(" успешно закрыта.", NamedTextColor.GREEN))
                        .build()
                )
                player.performCommand("reportlist")
            } else {
                player.sendMessage(Component.text("Не удалось закрыть жалобу.", NamedTextColor.RED))
            }
        }
    }
}