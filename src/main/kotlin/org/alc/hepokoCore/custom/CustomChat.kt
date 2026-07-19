package org.alc.hepokoCore.custom

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.alc.hepokoCore.Configs

class CustomChat : Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    @Suppress("DEPRECATION")
    fun onChat(event: AsyncPlayerChatEvent) {
        if (!Configs.globalConfig.getBoolean("CustomChat", true) ||
            !Configs.customConfig.getBoolean("custom-chat.enabled", true)) {
            return
        }

        event.isCancelled = true

        val formatPattern = Configs.customConfig.getString("custom-chat.pattern", "<nick>: <msg>") ?: "<nick>: <msg>"

        val formattedMessage = formatPattern
            .replace("<nick>", event.player.name)
            .replace("<msg>", event.message)

        val component = LegacyComponentSerializer.legacyAmpersand().deserialize(formattedMessage)

        event.recipients.forEach { it.sendMessage(component) }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onJoin(event: PlayerJoinEvent) {
        if (!Configs.globalConfig.getBoolean("CustomChat", true) ||
            !Configs.customConfig.getBoolean("custom-join.enabled", false)) {
            return
        }

        val formatPattern = Configs.customConfig.getString("custom-join.pattern", "[+] <nick>") ?: "[+] <nick>"
        val formattedMessage = formatPattern.replace("<nick>", event.player.name)

        val component = LegacyComponentSerializer.legacyAmpersand().deserialize(formattedMessage)
        event.joinMessage(component)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onQuit(event: PlayerQuitEvent) {
        if (!Configs.globalConfig.getBoolean("CustomChat", true) ||
            !Configs.customConfig.getBoolean("custom-quit.enabled", false)) {
            return
        }

        val formatPattern = Configs.customConfig.getString("custom-quit.pattern", "[-] <nick>") ?: "[-] <nick>"
        val formattedMessage = formatPattern.replace("<nick>", event.player.name)

        val component = LegacyComponentSerializer.legacyAmpersand().deserialize(formattedMessage)
        event.quitMessage(component)
    }
}