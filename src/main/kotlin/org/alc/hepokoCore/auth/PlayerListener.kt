package org.alc.hepokoCore.auth

import com.warrenstrange.googleauth.GoogleAuthenticator
import io.papermc.paper.event.player.AsyncChatEvent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.*

import org.alc.hepokoCore.Configs

class PlayerListener (private val auth: AuthEveryDay) : Listener {
    private val gAuth = GoogleAuthenticator()
    private fun isNotAuth(player: Player): Boolean {
        return !auth.authenticatedPlayers.contains(player.uniqueId)
    }

    @EventHandler
    fun onPreLogin(event: AsyncPlayerPreLoginEvent) {
        val ip = event.address.hostAddress
        runCatching {
            val query = "SELECT blocked_by FROM blocked_ips WHERE ip = ?;"
            auth.dbConnection?.prepareStatement(query).use { ps ->
                ps?.setString(1, ip)
                val rs = ps?.executeQuery()
                if (rs?.next() == true) {
                    val owner = rs.getString("blocked_by")
                    event.disallow(
                        AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                        net.kyori.adventure.text.Component.text("§6[AuthEveryDay]\n§cЭтот IP-адрес заблокирован владельцем аккаунта $owner!")
                    )
                }
            }
        }
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        val uuid = player.uniqueId
        val lastQuit = auth.lastDisconnectTimes[uuid]
        val sessionDurationMs = Configs.authConfig.getLong("session-duration-minutes", 5) * 60 * 1000
        if (lastQuit != null && (System.currentTimeMillis() - lastQuit) < sessionDurationMs) {
            auth.authenticatedPlayers.add(uuid)
            player.sendMessage("§6[AuthEveryDay] §aС возвращением! Сессия восстановлена.")
            return
        }
        val is2faEnabled = Configs.authConfig.getBoolean("2fa-enabled", true)
        val is2faOnly = is2faEnabled && Configs.authConfig.getBoolean("2fa_only", false)
        var hasAnyProtection = false
        var isRegistered = false
        runCatching {
            val query = "SELECT discord_id, totp_secret FROM users WHERE username = ?;"
            auth.dbConnection?.prepareStatement(query).use { ps ->
                ps?.setString(1, player.name.lowercase())
                val rs = ps?.executeQuery()
                if (rs?.next() == true) {
                    isRegistered = true
                    val ds = rs.getString("discord_id")
                    val app = rs.getString("totp_secret")
                    if (!ds.isNullOrEmpty() || !app.isNullOrEmpty()) {
                        hasAnyProtection = true
                    }
                }
            }
        }
        if (!isRegistered) {
            player.sendMessage("§6[AuthEveryDay] §cДобро пожаловать! Зарегистрируйтесь: /register <пароль>")
            return
        }
        if (is2faOnly && !hasAnyProtection) {
            player.sendMessage("§6[AuthEveryDay] §c[ВАЖНО] На сервере включена обязательная защита аккаунтов!")
            player.sendMessage("§eВы зарегистрированы, но не привязали защиту. Пожалуйста, настройте её:")
            player.sendMessage("§b➡ /2fa_discord <ваш_Discord_ID> §7(Вход по кнопке)")
            player.sendMessage("§b➡ /2fa_app setup §7(Вход по коду приложения)")
        } else {
            player.sendMessage("§6[AuthEveryDay] §cПожалуйста, войдите в аккаунт: /login <пароль>")
        }
    }

    @EventHandler
    fun onChat(event: AsyncChatEvent) {
        val player = event.player
        val uuid = player.uniqueId
        if (auth.pendingTOTPAuth.contains(uuid)) {
            event.isCancelled = true
            val rawText = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(event.message())
            val code = rawText.trim().toIntOrNull() ?: 0
            var secret = ""
            var discordId = ""
            runCatching {
                val query = "SELECT totp_secret, discord_id FROM users WHERE username = ?;"
                auth.dbConnection?.prepareStatement(query).use { ps ->
                    ps?.setString(1, player.name.lowercase())
                    val rs = ps?.executeQuery()
                    if (rs?.next() == true) {
                        secret = rs.getString("totp_secret")
                        discordId = rs.getString("discord_id")
                    }
                }
            }
            if (gAuth.authorize(secret, code)) {
                auth.pendingTOTPAuth.remove(uuid)
                auth.authenticatedPlayers.add(uuid)
                player.sendMessage("§6[AuthEveryDay] §aКод введен верно! Вход успешно подтвержден через приложение. Приятной игры.")
                val msgId = auth.pending2FA[uuid]
                if (msgId != null && auth.jda != null && discordId.isNotEmpty()) {
                    auth.jda?.retrieveUserById(discordId)?.queue { user ->
                        user.openPrivateChannel().queue { channel ->
                            channel.retrieveMessageById(msgId).queue { message ->
                                val kickButton = Button.danger("auth_kick:${uuid}", "🚪 Кикнуть с сервера")
                                message.editMessage("✅ **Вход успешно подтвержден через приложение Authenticator!** Активная сессия для профиля `${player.name}`.\n\nНажмите кнопку ниже, если хотите принудительно завершить сессию:")
                                    .setComponents(net.dv8tion.jda.api.interactions.components.ActionRow.of(kickButton))
                                    .queue()
                            }
                        }
                    }
                }
            } else {
                player.sendMessage("§6[AuthEveryDay] §cНеверный код! Введите заново 6 цифр из приложения в чат:")
            }
            return
        }
        if (isNotAuth(player)) {
            event.isCancelled = true
            player.sendMessage("§6[AuthEveryDay] §cВы не можете писать в чат до авторизации!")
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        val player = event.player
        val uuid = player.uniqueId
        if (auth.authenticatedPlayers.contains(uuid)) {
            auth.lastDisconnectTimes[uuid] = System.currentTimeMillis()
        }
        val msgId = auth.pending2FA[uuid]
        if (msgId != null && auth.jda != null) {
            runCatching {
                var discordId = ""
                val query = "SELECT discord_id FROM users WHERE username = ?;"
                auth.dbConnection?.prepareStatement(query).use { ps ->
                    ps?.setString(1, player.name.lowercase())
                    val rs = ps?.executeQuery()
                    if (rs?.next() == true) discordId = rs.getString("discord_id")
                }
                if (discordId.isNotEmpty()) {
                    auth.jda?.retrieveUserById(discordId)?.queue { user ->
                        user.openPrivateChannel().queue { channel ->
                            channel.retrieveMessageById(msgId).queue { message ->
                                message.editMessage("❌ **Сессия завершена.** Игрок `${player.name}` вышел с сервера.")
                                    .setComponents(emptyList())
                                    .queue()
                            }
                        }
                    }
                }
            }
        }
        auth.authenticatedPlayers.remove(uuid)
        auth.pending2FA.remove(uuid)
        auth.pendingTOTPSetup.remove(uuid)
        auth.pendingTOTPAuth.remove(uuid)
    }

    @EventHandler
    fun onMove(e: PlayerMoveEvent) {
        if (isNotAuth(e.player)) e.isCancelled = true
    }

    @EventHandler
    fun onBreak(e: BlockBreakEvent) {
        if (isNotAuth(e.player)) e.isCancelled = true
    }

    @EventHandler
    fun onPlace(e: BlockPlaceEvent) {
        if (isNotAuth(e.player)) e.isCancelled = true
    }
}
