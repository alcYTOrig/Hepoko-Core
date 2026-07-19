package org.alc.hepokoCore.protect

import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.block.BlockPhysicsEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.block.Container

/**
 * Listener for CoreProtect functionality.
 * Logs all player actions to the database.
 */
class ProtectListener(private val database: ProtectDatabase) : Listener {

    // ========== BLOCK EVENTS ==========

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val block = event.block
        
        database.logBlockAction(
            playerName = player.name,
            playerUuid = player.uniqueId.toString(),
            actionType = "BREAK",
            worldName = block.world.name,
            x = block.x,
            y = block.y,
            z = block.z,
            blockType = block.type.name,
            blockData = block.blockData.hashCode(),
            oldBlockType = null,
            oldBlockData = null
        )
    }

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        val player = event.player
        val block = event.block
        val oldBlock = event.blockReplacedState
        
        database.logBlockAction(
            playerName = player.name,
            playerUuid = player.uniqueId.toString(),
            actionType = "PLACE",
            worldName = block.world.name,
            x = block.x,
            y = block.y,
            z = block.z,
            blockType = block.type.name,
            blockData = block.blockData.hashCode(),
            oldBlockType = oldBlock.type.name,
            oldBlockData = oldBlock.blockData.hashCode()
        )
    }

    @EventHandler
    fun onBlockPhysics(event: BlockPhysicsEvent) {
        // Log physics events (like pistons, gravity blocks)
        val block = event.block
        
        database.logBlockAction(
            playerName = "[PHYSICS]",
            playerUuid = "",
            actionType = "PHYSICS",
            worldName = block.world.name,
            x = block.x,
            y = block.y,
            z = block.z,
            blockType = block.type.name,
            blockData = block.blockData.hashCode()
        )
    }

    // ========== PLAYER EVENTS ==========

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        
        database.logSessionAction(
            playerName = player.name,
            playerUuid = player.uniqueId.toString(),
            actionType = "JOIN",
            ipAddress = player.address?.hostString
        )
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        
        database.logSessionAction(
            playerName = player.name,
            playerUuid = player.uniqueId.toString(),
            actionType = "QUIT"
        )
    }

    @EventHandler
    fun onPlayerChat(event: AsyncPlayerChatEvent) {
        val player = event.player
        
        database.logChatMessage(
            playerName = player.name,
            playerUuid = player.uniqueId.toString(),
            message = event.message,
            channel = "global"
        )
    }

    // ========== CONTAINER EVENTS ==========

    @EventHandler
    fun onInventoryOpen(event: InventoryOpenEvent) {
        val holder = event.inventory.holder
        
        // Only log player-opened block containers (chests, furnaces, etc.)
        if (holder is Container && event.player is Player) {
            val player = event.player as Player
            val block = holder.block
            
            val containerType = when (holder) {
                is org.bukkit.block.Chest -> "CHEST"
                is org.bukkit.block.Furnace -> "FURNACE"
                is org.bukkit.block.Dispenser -> "DISPENSER"
                is org.bukkit.block.Dropper -> "DROPPER"
                is org.bukkit.block.Hopper -> "HOPPER"
                is org.bukkit.block.ShulkerBox -> "SHULKER_BOX"
                is org.bukkit.block.Barrel -> "BARREL"
                else -> "CONTAINER"
            }
            
            database.logContainerAction(
                playerName = player.name,
                playerUuid = player.uniqueId.toString(),
                actionType = "OPEN",
                worldName = block.world.name,
                x = block.x,
                y = block.y,
                z = block.z,
                containerType = containerType
            )
        }
    }

    // ========== ENTITY EVENTS ==========

    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val victim = event.entity
        val attacker = event.damager
        
        // Only log player-related damage
        if (victim is Player || attacker is Player) {
            val victimName = if (victim is Player) victim.name else victim.type.name
            val victimUuid = if (victim is Player) victim.uniqueId.toString() else null
            val victimType = if (victim is Player) "PLAYER" else victim.type.name
            
            val attackerName = if (attacker is Player) attacker.name else if (attacker != null) attacker.type.name else null
            val attackerUuid = if (attacker is Player) attacker.uniqueId.toString() else null
            val attackerType = if (attacker is Player) "PLAYER" else if (attacker != null) attacker.type.name else "UNKNOWN"
            
            val location = victim.location
            
            database.logEntityAction(
                attackerName = attackerName,
                attackerUuid = attackerUuid,
                attackerType = attackerType,
                victimName = victimName,
                victimUuid = victimUuid,
                victimType = victimType,
                actionType = "DAMAGE",
                damageAmount = event.damage,
                damageCause = event.cause.name,
                weapon = if (attacker is Player) attacker.inventory.itemInMainHand.type.name else null,
                worldName = location.world.name,
                x = location.blockX,
                y = location.blockY,
                z = location.blockZ
            )
        }
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity
        val killer = player.killer
        
        val killerName = killer?.name
        val killerUuid = killer?.uniqueId?.toString()
        val killerType = if (killer != null) "PLAYER" else "MOB"
        
        val location = player.location
        
        database.logEntityAction(
            attackerName = killerName,
            attackerUuid = killerUuid,
            attackerType = killerType,
            victimName = player.name,
            victimUuid = player.uniqueId.toString(),
            victimType = "PLAYER",
            actionType = "KILL",
            damageCause = event.entity.lastDamageCause?.cause?.name,
            worldName = location.world.name,
            x = location.blockX,
            y = location.blockY,
            z = location.blockZ
        )
    }
}
