package org.alc.hepokoCore.perms

import org.bukkit.permissions.Permissible
import org.bukkit.plugin.java.JavaPlugin

class HepokoPermissionsProvider(private val plugin: JavaPlugin) {
    fun register() {
        plugin.server.pluginManager.registerEvents(
            object : org.bukkit.event.Listener {
                @org.bukkit.event.EventHandler
                fun onCommand(event: org.bukkit.event.player.PlayerCommandPreprocessEvent) {
                    val player = event.player
                    val command = event.message.split(" ")[0].removePrefix("/")

                    val permissionNode = "hepokocore.$command"
                    if (!PermissionManager.hasPermission(player, permissionNode) &&
                        !player.hasPermission(permissionNode)) {
                    }
                }
            },
            plugin
        )
    }
}