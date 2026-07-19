package org.alc.hepokoCore.mimicChest

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.Chest
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import kotlin.math.roundToInt
import org.alc.hepokoCore.Configs
import org.alc.hepokoCore.HepokoCore
import java.time.Duration

class MimicListener(private val module: MimicChestModule, private val plugin: HepokoCore) : Listener {

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        val block = event.blockPlaced
        if (block.type == Material.CHEST || block.type == Material.TRAPPED_CHEST) {
            val loc = block.location
            MimicDatabase.markChestAsPlayerPlaced(loc.world.name, loc.blockX, loc.blockY, loc.blockZ)
        }
    }

    @EventHandler
    fun onChestOpen(event: PlayerInteractEvent) {
        val block = event.clickedBlock ?: return
        if (block.type != Material.CHEST && block.type != Material.TRAPPED_CHEST) return
        if (!event.action.name.contains("RIGHT_CLICK")) return

        val loc = block.location
        val player = event.player

        if (module.ignorePlayerPlaced) {
            if (MimicDatabase.isPlayerPlacedChest(loc.world.name, loc.blockX, loc.blockY, loc.blockZ)) return
        }

        if (module.activeSessions.containsKey(player.uniqueId)) {
            event.isCancelled = true
            return
        }

        if (kotlin.random.Random.nextDouble() > module.chance) return

        event.isCancelled = true
        startScreenMimic(player, loc)
    }

    private fun startScreenMimic(player: Player, location: org.bukkit.Location) {
        val actions = module.generateActionChain().toMutableList()
        val maxTicks = module.timeLimitSeconds * 20
        val session = ScreenMimicSession(
            player = player.uniqueId,
            chestLocation = location,
            actions = actions,
            timeLeftTicks = maxTicks,
            maxTimeTicks = maxTicks
        )

        module.activeSessions[player.uniqueId] = session
        updateScreenInterface(player, session)

        session.taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(module.plugin, {
            val currentSession = module.activeSessions[player.uniqueId]
            if (currentSession == null) {
                return@scheduleSyncRepeatingTask
            }

            currentSession.timeLeftTicks -= 2
            if (currentSession.timeLeftTicks <= 0) {
                failGame(player, currentSession, "Вы не успели отбиться от мимика!")
                return@scheduleSyncRepeatingTask
            }

            sendAirshipProgressBar(
                player,
                currentSession.timeLeftTicks.toDouble() / currentSession.maxTimeTicks.toDouble()
            )
        }, 0L, 2L)
    }

    private fun sendAirshipProgressBar(player: Player, ratio: Double) {
        val totalBars = 20
        val activeBarsCount = (ratio * totalBars).roundToInt().coerceIn(0, totalBars)
        val sideBars = activeBarsCount / 2

        val sb = StringBuilder()
        sb.append("§c§lАТАКУЕТ МИМИК: ")

        for (i in 0 until 10) {
            if (i < sideBars) sb.append("§c█") else sb.append("§8▒")
        }

        sb.append("§f ⚠️ ")

        for (i in 9 downTo 0) {
            if (i < sideBars) sb.append("§c█") else sb.append("§8▒")
        }

        val component = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
            .legacySection()
            .deserialize(sb.toString())
        player.sendActionBar(component)
    }

    private fun updateScreenInterface(player: Player, session: ScreenMimicSession) {
        val sb = StringBuilder()
        session.actions.forEachIndexed { index, displayStr ->
            if (index == session.currentChainIndex) {
                sb.append("§6§l[$displayStr] ")
            } else if (index > session.currentChainIndex) {
                sb.append("§7$displayStr ")
            } else {
                sb.append("§a✔ ")
            }
        }

        val titleComponent = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
            .legacySection()
            .deserialize("§c§lБЫСТРО НАЖИМАЙ КЛАВИШИ!")
        val subtitleComponent = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
            .legacySection()
            .deserialize(sb.toString().trim())

        val title = Title.title(
            titleComponent,
            subtitleComponent,
            Title.Times.times(
                Duration.ofMillis(0),
                Duration.ofMillis(1500),
                Duration.ofMillis(0)
            )
        )
        player.showTitle(title)
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        val session = module.activeSessions[player.uniqueId] ?: return

        event.isCancelled = true

        val isLeftClick = event.action.name.contains("LEFT_CLICK")
        val isRightClick = event.action.name.contains("RIGHT_CLICK")

        if (!isLeftClick && !isRightClick) return

        val inputType = if (isLeftClick) "ЛКМ" else "ПКМ"
        handleQteInput(player, session, inputType)
    }

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        val session = module.activeSessions[player.uniqueId] ?: return

        val from = event.from
        val to = event.to

        if (from.x != to.x || from.y != to.y || from.z != to.z) {
            event.isCancelled = true
        }

        if (to.y > from.y) {
            handleQteInput(player, session, "ПРОБЕЛ")
            return
        }

        val deltaX = to.x - from.x
        val deltaZ = to.z - from.z

        if (deltaX == 0.0 && deltaZ == 0.0) return

        val yaw = Math.toRadians(from.yaw.toDouble())
        val sin = Math.sin(yaw)
        val cos = Math.cos(yaw)

        val forwardMovement = -deltaX * sin + deltaZ * cos
        val strafeMovement = -deltaX * cos - deltaZ * sin

        var detectedKey = ""
        val threshold = 0.005

        if (forwardMovement > threshold) detectedKey = "W"
        else if (forwardMovement < -threshold) detectedKey = "S"
        else if (strafeMovement > threshold) detectedKey = "D"
        else if (strafeMovement < -threshold) detectedKey = "A"

        if (detectedKey.isNotEmpty()) {
            handleQteInput(player, session, detectedKey)
        }
    }

    private fun handleQteInput(player: Player, session: ScreenMimicSession, input: String) {
        val currentTime = System.currentTimeMillis()
        val lastInputTime = session.inputCooldowns[input] ?: 0L

        if (currentTime < lastInputTime) {
            return
        }

        session.inputCooldowns[input] = currentTime + 250L

        val expected = session.actions[session.currentChainIndex]

        if (input == expected) {
            session.currentProgress++
            session.currentChainIndex++

            if (Configs.globalConfig.getBoolean("debug", false)) {
                plugin.logger.info("Чистый клик: $input. Progress: ${session.currentProgress}/${module.clicksRequired}")
            }

            if (session.currentChainIndex >= session.actions.size) {
                if (session.currentProgress >= module.clicksRequired) {
                    winGame(player, session)
                    return
                } else {
                    val newActions = module.generateActionChain()
                    session.actions.clear()
                    session.actions.addAll(newActions)
                    session.currentChainIndex = 0
                }
            }

            player.playSound(player.location, org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.5f)
            session.timeLeftTicks = (session.timeLeftTicks + (session.maxTimeTicks / 8)).coerceAtMost(session.maxTimeTicks)
            updateScreenInterface(player, session)
        } else {
            session.timeLeftTicks = (session.timeLeftTicks - (session.maxTimeTicks / 7)).coerceAtLeast(0)
            player.playSound(player.location, org.bukkit.Sound.ITEM_SHIELD_BREAK, 0.5f, 1.2f)
        }
    }

    private fun winGame(player: Player, session: ScreenMimicSession) {
        Bukkit.getScheduler().cancelTask(session.taskId)
        module.activeSessions.remove(player.uniqueId)

        val titleComponent = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
            .legacySection()
            .deserialize("§a§lУСПЕХ")
        val subtitleComponent = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
            .legacySection()
            .deserialize("§7Мимик побежден!")

        val title = Title.title(
            titleComponent,
            subtitleComponent,
            Title.Times.times(
                Duration.ofMillis(250),
                Duration.ofMillis(1000),
                Duration.ofMillis(250)
            )
        )
        player.showTitle(title)

        player.sendActionBar(Component.text("✔ Доступ к сундуку получен", NamedTextColor.GREEN))
        player.playSound(player.location, org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f)

        val block = session.chestLocation.block
        if (block.type == Material.CHEST || block.type == Material.TRAPPED_CHEST) {
            val chest = block.state as Chest
            player.openInventory(chest.inventory)
        }
    }

    private fun failGame(player: Player, session: ScreenMimicSession, message: String) {
        Bukkit.getScheduler().cancelTask(session.taskId)
        module.activeSessions.remove(player.uniqueId)

        val titleComponent = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
            .legacySection()
            .deserialize("§c§lПОРАЖЕНИЕ")
        val subtitleComponent = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
            .legacySection()
            .deserialize("§4Мимик укусил вас!")

        val title = Title.title(
            titleComponent,
            subtitleComponent,
            Title.Times.times(
                Duration.ofMillis(250),
                Duration.ofMillis(1250),
                Duration.ofMillis(250)
            )
        )
        player.showTitle(title)

        player.sendActionBar(Component.text("☠ Нанесено урон в обход брони", NamedTextColor.RED))
        player.playSound(player.location, org.bukkit.Sound.ENTITY_WITHER_HURT, 0.8f, 0.8f)
        player.damage(module.damageAmount)

        player.sendMessage(
            Component.text()
                .append(Component.text(message, NamedTextColor.RED))
                .append(Component.text(" Снято ${module.damageAmount} HP!", NamedTextColor.RED))
                .build()
        )
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        if (module.activeSessions.containsKey(event.player.uniqueId)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val session = module.activeSessions[event.player.uniqueId] ?: return
        Bukkit.getScheduler().cancelTask(session.taskId)
        module.activeSessions.remove(event.player.uniqueId)
    }
}