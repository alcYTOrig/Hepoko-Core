package org.alc.hepokoCore.auth

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.buttons.Button
import org.alc.hepokoCore.HepokoCore
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID

class DiscordButtonListener(private val plugin: HepokoCore, private val auth: AuthEveryDay) : ListenerAdapter() {

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        val id = event.componentId
        val playerUuid = getUuidFromId(id) ?: return
        if (!checkMessageValidity(playerUuid, event)) return
        val player = getOnlinePlayer(playerUuid) ?: return
        if (id.startsWith("auth_approve:")) {
            var hasTotp = false
            runCatching {
                val query = "SELECT totp_secret FROM users WHERE username = ?;"
                auth.dbConnection?.prepareStatement(query).use { ps ->
                    ps?.setString(1, player.name.lowercase())
                    val rs = ps?.executeQuery()
                    if (rs?.next() == true && !rs.getString("totp_secret").isNullOrEmpty()) {
                        hasTotp = true
                    }
                }
            }
            if (hasTotp) {
                auth.pendingTOTPAuth.add(playerUuid)
                event.editMessage("✅ Первая фаза (Discord) пройдена! Введите 6-значный код из приложения Authenticator прямо в игровой чат Minecraft.")
                    .setComponents(emptyList()).queue()
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    player.sendMessage("§6[AuthEveryDay] §aВзаимодействие с Discord подтверждено!")
                    player.sendMessage("§6[AuthEveryDay] §eВторой этап: Введите код из приложения Authenticator прямо в чат:")
                })
            } else {
                auth.authenticatedPlayers.add(playerUuid)
                val kickButton = Button.danger("auth_kick:${player.uniqueId}", "🚪 Кикнуть с сервера")
                event.editMessage("✅ **Вход успешно подтвержден через Discord!** Активная игровая сессия для профиля `${player.name}`.\n\nНажмите кнопку ниже, если хотите принудительно завершить сессию:")
                    .setComponents(net.dv8tion.jda.api.interactions.components.ActionRow.of(kickButton))
                    .queue()
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    player.sendMessage("§6[AuthEveryDay] §aАвторизация успешно пройдена через Discord! Приятной игры.")
                })
            }
        } else if (id.startsWith("auth_block_ip:")) {
            val badIp = player.address?.address?.hostAddress ?: "Неизвестно"
            val time = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            runCatching {
                val query = "INSERT OR IGNORE INTO blocked_ips (ip, blocked_by, timestamp) VALUES (?, ?, ?);"
                auth.dbConnection?.prepareStatement(query).use { ps ->
                    ps?.setString(1, badIp)
                    ps?.setString(2, player.name.lowercase())
                    ps?.setString(3, time)
                    ps?.executeUpdate()
                }
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    player.kick(net.kyori.adventure.text.Component.text("§6[AuthEveryDay]\n§cВход заблокирован через Discord!\n§7Ваш IP внесен в черный список."))
                })
                auth.pending2FA.remove(playerUuid)
                event.editMessage("🚨 **Вход заблокирован!** IP-адрес `$badIp` внесен в черный список сервера.")
                    .setComponents(emptyList()).queue()
            }
        } else if (id.startsWith("auth_kick:")) {
            Bukkit.getScheduler().runTask(plugin, Runnable {
                player.kick(net.kyori.adventure.text.Component.text("§6[AuthEveryDay]\n§cВы были принудительно кикнуты с сервера через Discord!"))
            })
            auth.pending2FA.remove(playerUuid)
            event.editMessage("🚪 Сессия закрыта. Игрок `${player.name}` принудительно кикнут с сервера.")
                .setComponents(emptyList()).queue()
        }
    }

    private fun getUuidFromId(id: String): UUID? {
        val uuidStr = id.substringAfter(":")
        return runCatching { UUID.fromString(uuidStr) }.getOrNull()
    }

    private fun checkMessageValidity(uuid: UUID, event: ButtonInteractionEvent): Boolean {
        return auth.pending2FA[uuid] == event.messageId
    }

    private fun getOnlinePlayer(uuid: UUID): Player? {
        val player = Bukkit.getPlayer(uuid)
        if (player == null || !player.isOnline) {
            auth.pending2FA.remove(uuid)
            return null
        }
        return player
    }
}
