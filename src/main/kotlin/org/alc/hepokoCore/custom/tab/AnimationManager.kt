package org.alc.hepokoCore.custom.tab

import org.alc.hepokoCore.HepokoCore
import org.alc.hepokoCore.Configs

class AnimationManager(private val plugin: HepokoCore) {
    private val animations = mutableMapOf<String, AnimationHolder>()

    fun loadAnimations() {
        animations.clear()

        val section = Configs.animationsTabConfig.getConfigurationSection("animations")
            ?: Configs.animationsTabConfig.getConfigurationSection("Animations")

        if (section == null) {
            plugin.logger.warning("[HepokoCore] Секция 'animations' не найдена в AnimationsTabConfig.yml!")
            return
        }

        for (key in section.getKeys(false)) {
            val texts = section.getStringList("$key.texts")
            val interval = section.getLong("$key.change-interval", 4L)

            if (texts.isEmpty()) {
                plugin.logger.warning("[HepokoCore] Анимация '$key' пустая или не содержит строк 'texts'!")
                continue
            }

            animations[key.lowercase()] = AnimationHolder(texts, interval)
            plugin.logger.info("[HepokoCore] Успешно загружена анимация: $key (${texts.size} кадров)")
        }
    }

    fun updateTicks() {
        animations.values.forEach { it.tick() }
    }

    fun getFrame(name: String): String {
        return animations[name.lowercase()]?.getCurrentFrame() ?: ""
    }

    private class AnimationHolder(val frames: List<String>, val interval: Long) {
        private var tickCounter = 0L
        private var currentFrameIndex = 0

        fun tick() {
            if (frames.isEmpty()) return
            tickCounter++
            if (tickCounter % interval == 0L) {
                currentFrameIndex = (currentFrameIndex + 1) % frames.size
            }
        }

        fun getCurrentFrame(): String {
            if (frames.isEmpty()) return ""
            return frames[currentFrameIndex]
        }
    }
}