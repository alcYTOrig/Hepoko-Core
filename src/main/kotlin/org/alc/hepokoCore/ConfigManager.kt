package org.alc.hepokoCore

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class ConfigManager(private val plugin: HepokoCore) {
    private val configs = HashMap<String, FileConfiguration>()
    private val files = HashMap<String, File>()

    fun loadConfig(name: String): FileConfiguration {
        val file = File(plugin.dataFolder, name)
        if (!file.exists()) {
            if (!plugin.dataFolder.exists()) {
                plugin.dataFolder.mkdirs()
            }
            runCatching {
                plugin.getResource(name)?.use { inputStream ->
                    file.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            }
        }
        val configuration = YamlConfiguration.loadConfiguration(file)
        configs[name] = configuration
        files[name] = file
        return configuration
    }

    fun getConfig(name: String): FileConfiguration {
        return configs[name] ?: loadConfig(name)
    }

    fun saveConfig(name: String) {
        val config = configs[name] ?: return
        val file = files[name] ?: return
        runCatching {
            config.save(file)
        }.onFailure {
            plugin.logger.severe("Не удалось сохранить файл $name: ${it.message}")
        }
    }

    fun reloadConfig(name: String) {
        val file = files[name] ?: return
        configs[name] = YamlConfiguration.loadConfiguration(file)
    }
}