package org.alc.hepokoCore.perms

import net.milkbowl.vault.permission.Permission
import org.bukkit.OfflinePlayer
import org.bukkit.World
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@Suppress("OVERRIDE_DEPRECATION")
class HepokoVaultPermissions : Permission() {

    override fun getName(): String = "HepokoPerms"
    override fun isEnabled(): Boolean = true
    override fun hasSuperPermsCompat(): Boolean = true
    override fun hasGroupSupport(): Boolean = true

    // --- CommandSender / Player Checks ---
    @Deprecated("")
    override fun has(sender: CommandSender, permission: String): Boolean {
        return if (sender is Player) PermissionManager.hasPermission(sender, permission) else false
    }

    @Deprecated("")
    override fun has(player: Player, permission: String): Boolean {
        return PermissionManager.hasPermission(player, permission)
    }

    // Required abstract methods from Permission with String parameters
    @Deprecated("")
    override fun getPlayerGroups(world: String, player: String): Array<String> = arrayOf("default")
    @Deprecated("")
    override fun getPrimaryGroup(world: String, player: String): String = "default"
    @Deprecated("")
    override fun playerHas(world: String, player: String, permission: String): Boolean = false
    @Deprecated("")
    override fun groupHas(world: String, group: String, permission: String): Boolean = false
    @Deprecated("")
    override fun playerAdd(world: String, player: String, permission: String): Boolean = false
    @Deprecated("")
    override fun playerRemove(world: String, player: String, permission: String): Boolean = false
    @Deprecated("")
    override fun groupAdd(world: String, group: String, permission: String): Boolean = false
    @Deprecated("")
    override fun groupRemove(world: String, group: String, permission: String): Boolean = false
    @Deprecated("")
    override fun playerInGroup(world: String, player: String, group: String): Boolean = false
    @Deprecated("")
    override fun playerAddGroup(world: String, player: String, group: String): Boolean = false
    @Deprecated("")
    override fun playerRemoveGroup(world: String, player: String, group: String): Boolean = false

    // Required abstract methods from Permission with World/OfflinePlayer parameters
    @Deprecated("")
    override fun groupHas(world: World, group: String, permission: String): Boolean = false
    @Deprecated("")
    override fun groupAdd(world: World, group: String, permission: String): Boolean = false

    // --- Player Groups (with Player) ---
    override fun getPlayerGroups(player: Player): Array<String> {
        return PermissionManager.getUserData(player.uniqueId)?.groups?.toTypedArray() ?: arrayOf("default")
    }

    override fun getPrimaryGroup(player: Player): String {
        return PermissionManager.getUserData(player.uniqueId)?.groups?.firstOrNull() ?: "default"
    }

    // --- Group Management ---
    override fun getGroups(): Array<String> {
        return PermissionManager.getAllGroups().map { it.name }.toTypedArray()
    }

    // --- Player Permissions ---
    override fun playerAdd(player: Player, permission: String): Boolean {
        PermissionDatabase.addUserPermission(player.uniqueId.toString(), permission, true)
        return true
    }

    override fun playerRemove(player: Player, permission: String): Boolean = false
    override fun playerAddGroup(player: Player, group: String): Boolean = false
    override fun playerRemoveGroup(player: Player, group: String): Boolean = false
}
