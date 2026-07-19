package org.alc.hepokoCore.ban

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import java.text.SimpleDateFormat

class MuteListener : Listener {
    private val dateFormat: SimpleDateFormat by lazy {
        SimpleDateFormat("dd.MM.yyyy HH:mm:ss")
    }

    @EventHandler(priority = EventPriority.LOWEST)
    @Suppress("DEPRECATION")
    fun onPlayerChat(event: AsyncPlayerChatEvent) {
        val player = event.player
        val muteInfo = DatabaseManager.checkMute(player.name) ?: return

        event.isCancelled = true

        val kickMessage = Component.text()
            .append(Component.text("Ваш чат заблокирован!\n", NamedTextColor.RED))
            .append(Component.text("Замутил: ", NamedTextColor.GRAY))
            .append(Component.text(muteInfo.muter, NamedTextColor.WHITE))
            .append(Component.text("\nПричина: ", NamedTextColor.GRAY))
            .append(Component.text(muteInfo.reason, NamedTextColor.WHITE))
            .append(Component.text("\nДействует до: ", NamedTextColor.GRAY))
            .append(Component.text(dateFormat.format(muteInfo.expiryDate), NamedTextColor.YELLOW))
            .build()

        player.sendMessage(kickMessage)
    }
}