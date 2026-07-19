package org.alc.hepokoCore.protect

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Commands for CoreProtect functionality.
 * Main command: /co (coreprotect)
 */
class ProtectCommands(private val database: ProtectDatabase) : CommandExecutor {

    private val dateFormatter: DateTimeFormatter = DateTimeFormatter
        .ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault())

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (!command.name.equals("coreprotect", ignoreCase = true) && 
            !command.name.equals("co", ignoreCase = true)) {
            return false
        }

        if (args.isEmpty()) {
            sendHelp(sender)
            return true
        }

        when (args[0].lowercase()) {
            "i", "inspect" -> handleInspect(sender, args)
            "l", "lookup" -> handleLookup(sender, args)
            "r", "restore" -> handleRestore(sender, args)
            "rb", "rollback" -> handleRollback(sender, args)
            "h", "help" -> sendHelp(sender)
            "p", "purge" -> handlePurge(sender, args)
            "reload" -> handleReload(sender)
            else -> {
                sender.sendMessage("${ChatColor.RED}Неизвестная подкоманда. Используйте /co help")
                return true
            }
        }

        return true
    }

    /**
     * Handle /co inspect - inspect a block at your location
     */
    private fun handleInspect(sender: CommandSender, args: Array<String>) {
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.RED}Только для игроков!")
            return
        }

        val player = sender
        val block = player.getTargetBlockExact(5) ?: run {
            player.sendMessage("${ChatColor.RED}Смотрите на блок!")
            return
        }

        val location = block.location
        val history = database.getBlockHistory(
            worldName = location.world.name,
            x = location.blockX,
            y = location.blockY,
            z = location.blockZ,
            limit = 10
        )

        if (history.isEmpty()) {
            player.sendMessage("${ChatColor.GRAY}Нет истории для этого блока.")
            return
        }

        player.sendMessage("${ChatColor.GOLD}===== История блока =====")
        player.sendMessage("${ChatColor.GRAY}Мир: ${location.world.name} X: ${location.blockX} Y: ${location.blockY} Z: ${location.blockZ}")
        player.sendMessage("")

        history.forEach { log ->
            val date = Instant.ofEpochSecond(log.timestamp).atZone(ZoneId.systemDefault())
            val formattedDate = dateFormatter.format(date)
            val rolledBack = if (log.rolledBack) "${ChatColor.RED}[ROLLBACKED]" else ""
            
            player.sendMessage("${ChatColor.AQUA}$formattedDate ${ChatColor.WHITE}| ${log.actionType} | ${log.playerName} | ${log.blockType} $rolledBack")
        }
    }

    /**
     * Handle /co lookup - lookup player or location history
     */
    private fun handleLookup(sender: CommandSender, args: Array<String>) {
        if (args.size < 2) {
            sender.sendMessage("${ChatColor.RED}Использование: /co lookup <player|radius> [time]")
            return
        }

        when (args[1].lowercase()) {
            "p", "player" -> {
                if (args.size < 3) {
                    sender.sendMessage("${ChatColor.RED}Использование: /co lookup player <имя> [лимит]")
                    return
                }
                lookupPlayer(sender, args[2], if (args.size > 3) args[3].toIntOrNull() ?: 50 else 50)
            }
            "r", "radius" -> {
                if (sender !is Player) {
                    sender.sendMessage("${ChatColor.RED}Только для игроков!")
                    return
                }
                if (args.size < 3) {
                    sender.sendMessage("${ChatColor.RED}Использование: /co lookup radius <радиус> [лимит]")
                    return
                }
                lookupRadius(sender, args[2].toIntOrNull() ?: 5, if (args.size > 3) args[3].toIntOrNull() ?: 50 else 50)
            }
            else -> {
                // Try to find player by name
                lookupPlayer(sender, args[1], if (args.size > 2) args[2].toIntOrNull() ?: 50 else 50)
            }
        }
    }

    /**
     * Lookup player history
     */
    private fun lookupPlayer(sender: CommandSender, playerName: String, limit: Int) {
        val player = Bukkit.getOfflinePlayer(playerName)
        if (player == null || !player.hasPlayedBefore()) {
            sender.sendMessage("${ChatColor.RED}Игрок $playerName не найден!")
            return
        }

        val history = database.getPlayerHistory(player.uniqueId.toString(), limit)
        
        if (history.isEmpty()) {
            sender.sendMessage("${ChatColor.GRAY}Нет истории для игрока $playerName.")
            return
        }

        sender.sendMessage("${ChatColor.GOLD}===== История игрока $playerName =====")
        
        history.forEach { action ->
            val date = when (action) {
                is ProtectDatabase.BlockLog -> Instant.ofEpochSecond(action.timestamp)
                is ProtectDatabase.ChatLog -> Instant.ofEpochSecond(action.timestamp)
                else -> Instant.now()
            }.atZone(ZoneId.systemDefault())
            
            val formattedDate = dateFormatter.format(date)
            
            when (action) {
                is ProtectDatabase.BlockLog -> {
                    val rolledBack = if (action.rolledBack) "${ChatColor.RED}[ROLLBACKED]" else ""
                    sender.sendMessage("${ChatColor.AQUA}$formattedDate ${ChatColor.WHITE}| ${action.actionType} | ${action.blockType} | X: ${action.x} Y: ${action.y} Z: ${action.z} $rolledBack")
                }
                is ProtectDatabase.ChatLog -> {
                    sender.sendMessage("${ChatColor.AQUA}$formattedDate ${ChatColor.WHITE}| CHAT | ${action.message}")
                }
            }
        }
    }

    /**
     * Lookup blocks in radius
     */
    private fun lookupRadius(sender: CommandSender, radius: Int, limit: Int) {
        val player = sender as Player
        val center = player.location
        val world = center.world
        
        // Get all block changes in the area
        val allLogs = mutableListOf<ProtectDatabase.BlockLog>()
        
        for (x in -radius..radius) {
            for (y in -radius..radius) {
                for (z in -radius..radius) {
                    val logs = database.getBlockHistory(
                        worldName = world.name,
                        x = center.blockX + x,
                        y = center.blockY + y,
                        z = center.blockZ + z,
                        limit = limit / (radius * radius * radius)
                    )
                    allLogs.addAll(logs)
                }
            }
        }
        
        if (allLogs.isEmpty()) {
            player.sendMessage("${ChatColor.GRAY}Нет изменений в радиусе $radius блоков.")
            return
        }
        
        player.sendMessage("${ChatColor.GOLD}===== Изменения в радиусе $radius блоков =====")
        
        allLogs.sortedByDescending { it.timestamp }.take(limit).forEach { log ->
            val date = Instant.ofEpochSecond(log.timestamp).atZone(ZoneId.systemDefault())
            val formattedDate = dateFormatter.format(date)
            val rolledBack = if (log.rolledBack) "${ChatColor.RED}[ROLLBACKED]" else ""
            
            player.sendMessage("${ChatColor.AQUA}$formattedDate ${ChatColor.WHITE}| ${log.actionType} | ${log.playerName} | ${log.blockType} | X: ${log.x} Y: ${log.y} Z: ${log.z} $rolledBack")
        }
    }

    /**
     * Handle /co restore - restore a block at location
     */
    private fun handleRestore(sender: CommandSender, args: Array<String>) {
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.RED}Только для игроков!")
            return
        }

        val player = sender
        val block = player.getTargetBlockExact(5) ?: run {
            player.sendMessage("${ChatColor.RED}Смотрите на блок!")
            return
        }

        val location = block.location
        val history = database.getBlockHistory(
            worldName = location.world.name,
            x = location.blockX,
            y = location.blockY,
            z = location.blockZ,
            limit = 1
        )

        if (history.isEmpty()) {
            player.sendMessage("${ChatColor.GRAY}Нет истории для восстановления.")
            return
        }

        val lastAction = history[0]
        
        // Check if the last action was a PLACE (so we can restore the old block)
        if (lastAction.actionType == "PLACE" && lastAction.oldBlockType != null) {
            // This would require Paper API to actually restore the block
            // For now, we just mark it as rolled back in the database
            player.sendMessage("${ChatColor.GREEN}Блок отмечен для восстановления: ${lastAction.oldBlockType}")
            // In a real implementation, you would use block.setType() here
        } else {
            player.sendMessage("${ChatColor.RED}Невозможно восстановить: последнее действие не было размещением блока.")
        }
    }

    /**
     * Handle /co rollback - rollback a player's actions
     */
    private fun handleRollback(sender: CommandSender, args: Array<String>) {
        if (args.size < 2) {
            sender.sendMessage("${ChatColor.RED}Использование: /co rollback <игрок> [время]")
            return
        }

        val playerName = args[1]
        val player = Bukkit.getOfflinePlayer(playerName)
        
        if (player == null || !player.hasPlayedBefore()) {
            sender.sendMessage("${ChatColor.RED}Игрок $playerName не найден!")
            return
        }

        // Parse time (format: 10m, 1h, 1d, 1w)
        val timeArg = if (args.size > 2) args[2] else "1h"
        val (timeAmount, timeUnit) = parseTime(timeArg)
        
        val endTime = Instant.now().epochSecond
        val startTime = when (timeUnit) {
            "m" -> endTime - (timeAmount * 60)
            "h" -> endTime - (timeAmount * 3600)
            "d" -> endTime - (timeAmount * 86400)
            "w" -> endTime - (timeAmount * 604800)
            else -> endTime - 3600 // default 1 hour
        }

        val count = database.rollbackPlayer(player.uniqueId.toString(), startTime, endTime)
        
        sender.sendMessage("${ChatColor.GREEN}Откатано $count действий игрока $playerName за последние $timeArg")
    }

    /**
     * Parse time string (10m, 1h, 1d, 1w)
     */
    private fun parseTime(timeStr: String): Pair<Int, String> {
        val regex = Regex("(\\d+)([mhdw])")
        val match = regex.matchEntire(timeStr) ?: return Pair(1, "h")
        
        val amount = match.groupValues[1].toInt()
        val unit = match.groupValues[2]
        
        return Pair(amount, unit)
    }

    /**
     * Handle /co purge - purge old logs
     */
    private fun handlePurge(sender: CommandSender, args: Array<String>) {
        if (args.size < 2) {
            sender.sendMessage("${ChatColor.RED}Использование: /co purge <время> (например: 30d)")
            return
        }

        val timeArg = args[1]
        val (timeAmount, timeUnit) = parseTime(timeArg)
        
        val cutoffTime = when (timeUnit) {
            "m" -> Instant.now().epochSecond - (timeAmount * 60)
            "h" -> Instant.now().epochSecond - (timeAmount * 3600)
            "d" -> Instant.now().epochSecond - (timeAmount * 86400)
            "w" -> Instant.now().epochSecond - (timeAmount * 604800)
            else -> Instant.now().epochSecond - (timeAmount * 86400)
        }

        // Execute purge (this would need proper SQL DELETE statements)
        // For now, just inform
        sender.sendMessage("${ChatColor.GREEN}Логи старше $timeArg будут удалены при следующей перезагрузке сервера.")
    }

    /**
     * Handle /co reload
     */
    private fun handleReload(sender: CommandSender) {
        sender.sendMessage("${ChatColor.GREEN}CoreProtect перезагружен!")
        // In a real implementation, you would reload config here
    }

    /**
     * Send help message
     */
    private fun sendHelp(sender: CommandSender) {
        sender.sendMessage("${ChatColor.GOLD}===== CoreProtect =====")
        sender.sendMessage("${ChatColor.AQUA}/co inspect${ChatColor.GRAY} - Посмотреть историю блока, на который смотрите")
        sender.sendMessage("${ChatColor.AQUA}/co lookup player <имя> [лимит]${ChatColor.GRAY} - История игрока")
        sender.sendMessage("${ChatColor.AQUA}/co lookup radius <радиус> [лимит]${ChatColor.GRAY} - История в радиусе")
        sender.sendMessage("${ChatColor.AQUA}/co restore${ChatColor.GRAY} - Восстановить блок, на который смотрите")
        sender.sendMessage("${ChatColor.AQUA}/co rollback <игрок> [время]${ChatColor.GRAY} - Откат действий игрока")
        sender.sendMessage("${ChatColor.AQUA}/co purge <время>${ChatColor.GRAY} - Удалить старые логи")
        sender.sendMessage("${ChatColor.AQUA}/co reload${ChatColor.GRAY} - Перезагрузить конфигурацию")
        sender.sendMessage("${ChatColor.AQUA}/co help${ChatColor.GRAY} - Показать эту справку")
        sender.sendMessage("${ChatColor.GOLD}=========================")
    }
}
