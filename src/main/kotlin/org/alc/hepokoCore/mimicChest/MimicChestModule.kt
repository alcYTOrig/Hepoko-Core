package org.alc.hepokoCore.mimicChest

import java.util.UUID
import org.alc.hepokoCore.HepokoCore
import org.alc.hepokoCore.Configs

class MimicChestModule(val plugin: HepokoCore) {
    var chance: Double = 0.40
    var ignorePlayerPlaced: Boolean = true
    var clicksRequired: Int = 15
    var timeLimitSeconds: Int = 15
    var damageAmount: Double = 7.0
    var mouseClickEnabled: Boolean = true
    var keyboardClickEnabled: Boolean = true

    val activeSessions = mutableMapOf<UUID, ScreenMimicSession>()

    fun loadConfig() {
        chance = Configs.mimicConfig.getDouble("chance", 0.40)
        ignorePlayerPlaced = Configs.mimicConfig.getBoolean("ignore_player_placed", true)
        clicksRequired = Configs.mimicConfig.getInt("clicks_required", 15)
        timeLimitSeconds = Configs.mimicConfig.getInt("time_limit_seconds", 15)
        damageAmount = Configs.mimicConfig.getDouble("damage_amount", 7.0)
        mouseClickEnabled = Configs.mimicConfig.getBoolean("mouse_click", true)
        keyboardClickEnabled = Configs.mimicConfig.getBoolean("keyboard_click", true)
    }

    fun generateActionChain(): List<String> {
        val count = (3..5).random()
        val availableTypes = mutableListOf<String>()
        if (mouseClickEnabled) availableTypes.add("MOUSE")
        if (keyboardClickEnabled) availableTypes.add("KEYBOARD")
        if (availableTypes.isEmpty()) availableTypes.add("KEYBOARD")

        val mousePool = listOf("ЛКМ", "ПКМ")
        val keyboardPool = listOf("W", "A", "S", "D", "ПРОБЕЛ")

        return List(count) {
            val selectedType = availableTypes.random()
            if (selectedType == "MOUSE") mousePool.random() else keyboardPool.random()
        }
    }
}

class ScreenMimicSession(
    val player: UUID,
    val chestLocation: org.bukkit.Location,
    val actions: MutableList<String>,
    var currentProgress: Int = 0,
    var currentChainIndex: Int = 0,
    var timeLeftTicks: Int,
    val maxTimeTicks: Int,
    var taskId: Int = -1,
    val inputCooldowns: MutableMap<String, Long> = mutableMapOf()
)