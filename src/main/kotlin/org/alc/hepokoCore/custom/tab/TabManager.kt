package org.alc.hepokoCore.custom.tab

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.alc.hepokoCore.HepokoCore
import org.alc.hepokoCore.Configs
import java.util.regex.Pattern

class TabManager(private val plugin: HepokoCore, private val animationManager: AnimationManager) {
    private var tabTask: BukkitRunnable? = null
    private var scoreObjective: Objective? = null

    private val colorPattern: Pattern = Pattern.compile("§[0-9a-fk-orx]|§x(§[0-9a-f]){6}")

    fun start() {
        animationManager.loadAnimations()
        setupRightSidePanel()

        tabTask = object : BukkitRunnable() {
            override fun run() {
                if (!Configs.customConfig.getBoolean("tab.enabled", true)) return
                animationManager.updateTicks()
                for (player in Bukkit.getOnlinePlayers()) {
                    updateTabForPlayer(player)
                }
            }
        }
        tabTask?.runTaskTimer(plugin, 0L, Configs.customConfig.getLong("tab.update-interval", 1L))
    }

    fun stop() {
        tabTask?.cancel()
        tabTask = null
        try {
            scoreObjective?.unregister()
            val scoreboard = Bukkit.getScoreboardManager().mainScoreboard
            scoreboard.getObjective("tab_custom")?.unregister()
        } catch (ignored: Exception) {
        }
    }

    fun reload() {
        stop()
        start()
        forceInstantUpdate()
    }

    private fun forceInstantUpdate() {
        if (!Configs.customConfig.getBoolean("tab.enabled", true)) return
        for (player in Bukkit.getOnlinePlayers()) {
            updateTabForPlayer(player)
        }
    }

    private fun setupRightSidePanel() {
        val scoreboard = Bukkit.getScoreboardManager().mainScoreboard
        try {
            scoreboard.getObjective("tab_custom")?.unregister()
            scoreObjective = scoreboard.getObjective("tab_custom") ?: scoreboard.registerNewObjective(
                "tab_custom",
                Criteria.DUMMY,
                Component.text("tab_custom")
            )
            val type = Configs.customConfig.getString("tab.right-side-panel", "NONE")
            if (type.equals("CUSTOM_TEXT", ignoreCase = true)) {
                scoreObjective?.displaySlot = DisplaySlot.PLAYER_LIST
            } else {
                scoreObjective?.displaySlot = null
            }
        } catch (e: Exception) {
            plugin.logger.warning("Не удалось инициализировать правую панель Scoreboard: ${e.message}")
        }
    }

    private fun updateTabForPlayer(player: Player) {
        val rawHeader = Configs.customConfig.getStringList("tab.header")
        val rawFooter = Configs.customConfig.getStringList("tab.footer")
        var headerLines = rawHeader.map { parsePlaceholders(it, player) }
        var footerLines = rawFooter.map { parsePlaceholders(it, player) }

        if (Configs.customConfig.getBoolean("tab.auto-center", true)) {
            val allLines = headerLines + footerLines
            val maxLength = allLines.maxOfOrNull { getVisibleLength(it) } ?: 0
            headerLines = headerLines.map { centerText(it, maxLength) }
            footerLines = footerLines.map { centerText(it, maxLength) }
        }

        val headerComponent = LegacyComponentSerializer.legacySection()
            .deserialize(headerLines.joinToString("\n"))
        val footerComponent = LegacyComponentSerializer.legacySection()
            .deserialize(footerLines.joinToString("\n"))

        player.sendPlayerListHeaderAndFooter(headerComponent, footerComponent)

        val type = Configs.customConfig.getString("tab.right-side-panel", "NONE")
        if (type.equals("CUSTOM_TEXT", ignoreCase = true) && scoreObjective != null) {
            val rightText = Configs.customConfig.getString("tab.right-side-panel.text", "") ?: ""
            val parsedRightText = parsePlaceholders(rightText, player)

            val rightComponent = LegacyComponentSerializer.legacySection()
                .deserialize(parsedRightText)

            scoreObjective?.getScore(player.name)?.customName(rightComponent)
            scoreObjective?.getScore(player.name)?.score = 0
        }
    }

    private fun centerText(text: String, maxLength: Int): String {
        val visibleLength = getVisibleLength(text)
        if (visibleLength >= maxLength) return text
        val neededSpaces = (maxLength - visibleLength) / 2
        val spaces = " ".repeat(neededSpaces)
        return spaces + text + spaces
    }

    private fun getVisibleLength(text: String): Int {
        return colorPattern.matcher(text).replaceAll("").length
    }

    private fun parsePlaceholders(text: String, player: Player): String {
        var result = text
            .replace("%player%", player.name)
            .replace("%online%", Bukkit.getOnlinePlayers().size.toString())
            .replace("%max_online%", Bukkit.getMaxPlayers().toString())
            .replace("%ping%", player.ping.toString())
            .replace("&", "§")

        if (result.contains("%animation:")) {
            val section = Configs.animationsTabConfig.getConfigurationSection("animations")
                ?: Configs.animationsTabConfig.getConfigurationSection("Animations")
            section?.getKeys(false)?.forEach { animName ->
                val placeholder = "%animation:$animName%"
                if (result.contains(placeholder)) {
                    val frame = animationManager.getFrame(animName)
                    if (frame.isNotEmpty()) {
                        result = result.replace(placeholder, frame.replace("&", "§"))
                    }
                }
            }
        }
        return result
    }
}