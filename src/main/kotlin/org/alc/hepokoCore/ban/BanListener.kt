package org.alc.hepokoCore.ban

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerLoginEvent

class BanListener : Listener {
    @EventHandler
    @Suppress("DEPRECATION")
    fun onPlayerLogin(event: PlayerLoginEvent) {
        val player = event.player
        val nickname = player.name
        val ipAddress = event.address.hostAddress

        var banInfo = DatabaseManager.checkBan(nickname)
        if (banInfo == null && ipAddress != null) {
            banInfo = DatabaseManager.checkBan(ipAddress)
        }

        if (banInfo != null) {
            val kickMessage = Component.text()
                .append(Component.text("Вы заблокированы на этом сервере!\n", NamedTextColor.RED))
                .append(Component.text("Администратор: ", NamedTextColor.GRAY))
                .append(Component.text(banInfo.banner, NamedTextColor.GOLD))
                .append(Component.text("\nПричина: ", NamedTextColor.GRAY))
                .append(Component.text(banInfo.reason, NamedTextColor.WHITE))
                .append(Component.text("\nДействует до: ", NamedTextColor.GRAY))
                .append(Component.text(banInfo.expiryDate.toString(), NamedTextColor.YELLOW))
                .build()
            @Suppress("DEPRECATION")
            event.disallow(PlayerLoginEvent.Result.KICK_BANNED, kickMessage)
        }
    }
}