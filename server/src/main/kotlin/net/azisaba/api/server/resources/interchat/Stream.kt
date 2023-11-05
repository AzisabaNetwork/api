@file:Suppress("SqlResolve", "SqlNoDataSourceInspection")

package net.azisaba.api.server.resources.interchat

import io.ktor.resources.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.azisaba.api.server.interchat.*
import net.azisaba.api.server.interchat.protocol.OutgoingErrorMessagePacket
import net.azisaba.api.server.interchat.protocol.OutgoingGuildPacket
import net.azisaba.api.server.plugins.authSimple
import net.azisaba.api.server.resources.WebSocketRequestHandler
import net.azisaba.api.server.util.DurationUtil
import net.azisaba.api.util.JSON
import net.azisaba.interchat.api.guild.GuildInviteResult
import net.azisaba.interchat.api.guild.GuildMember
import net.azisaba.interchat.api.guild.GuildRole
import net.azisaba.interchat.api.network.Protocol
import net.azisaba.interchat.api.network.protocol.GuildInvitePacket
import net.azisaba.interchat.api.network.protocol.GuildInviteResultPacket
import net.azisaba.interchat.api.network.protocol.GuildMessagePacket
import net.azisaba.interchat.api.text.KanaTranslator
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import java.time.Duration
import java.util.concurrent.CompletionException

@Serializable
@Resource("/interchat/stream")
class Stream : WebSocketRequestHandler() {
    override suspend fun DefaultWebSocketServerSession.handleRequest() {
        val server = call.request.queryParameters["server"] ?: run {
            return close(CloseReason(CloseReason.Codes.GOING_AWAY, "server parameter must be specified"))
        }
        val conn = ConnectedSocket(null, server, this)
        InterChatPacketListener.sockets += conn
        try {
            for (frame in incoming) {
                frame as? Frame.Text ?: continue
                val packet = JSON.decodeFromString<Packet>(frame.readText())
                if (conn.uuid == null && packet !is AuthPacket) continue // if not authenticated and packet is not AuthPacket, ignore it
                packet.handle(conn) // actually handle packet
            }
        } finally {
            InterChatPacketListener.sockets -= conn
        }
    }

    @Serializable
    sealed interface Packet {
        suspend fun handle(connection: ConnectedSocket)
    }

    @SerialName("auth")
    @Serializable
    data class AuthPacket(val key: String) : Packet {
        override suspend fun handle(connection: ConnectedSocket) {
            if (connection.uuid == null) {
                val principal = authSimple(key) ?: run {
                    connection.sendPacket(OutgoingErrorMessagePacket("Invalid API Key"))
                    return connection.connection.close(
                        CloseReason(
                            CloseReason.Codes.GOING_AWAY,
                            "Failed to authenticate you."
                        )
                    )
                }
                connection.uuid = principal.player
                connection.sendPacket(OutgoingGuildPacket(connection.getSelectedGuildId()))
            }
        }
    }

    @SerialName("message")
    @Serializable
    data class MessagePacket(val guildId: Long? = null, val message: String) : Packet {
        override suspend fun handle(connection: ConnectedSocket) {
            if (InterChatPacketListener.getHideAllUntil(connection.uuid!!) > System.currentTimeMillis()) {
                connection.sendPacket(OutgoingErrorMessagePacket("hideallが有効になっているため、メッセージを送信できません。"))
                return
            }
            val guildId = guildId ?: connection.getSelectedGuildId()
            val self = InterChatApi.guildManager.getMember(guildId, connection.uuid!!).join()
            if (self.hiddenByMember()) {
                connection.sendPacket(OutgoingErrorMessagePacket("このギルドをhideしているため、メッセージを送信できません。"))
                return
            }
            var newMessage: String = message
            val transliteratedMessage =
                if (newMessage.startsWith("#")) {
                    newMessage = newMessage.substring(1)
                    null
                } else {
                    val translateKana = InterChatApi.userManager.fetchUser(connection.uuid!!).join().translateKana()
                    if (translateKana) {
                        val suggestions = withContext(Dispatchers.IO) {
                            KanaTranslator.translateSync(newMessage)
                        }
                        if (suggestions.isNotEmpty()) {
                            suggestions[0]
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                }
            InterChatApi.guildManager.getMember(guildId, connection.uuid!!).exceptionally { null }.join() ?: return
            val guildMessagePacket =
                GuildMessagePacket(guildId, connection.server, connection.uuid!!, message, transliteratedMessage)
            Protocol.GUILD_MESSAGE.send(JedisBoxProvider.get().pubSubHandler, guildMessagePacket)
        }
    }

    @SerialName("select")
    @Serializable
    data class SelectPacket(val guildId: Long) : Packet {
        override suspend fun handle(connection: ConnectedSocket) {
            InterChatApi.queryExecutor.query("UPDATE `players` SET `selected_guild` = ? WHERE `id` = ?") { ps ->
                ps.setLong(1, guildId)
                ps.setString(2, connection.uuid.toString())
                ps.executeUpdate()
            }
            connection.sendPacket(OutgoingGuildPacket(guildId))
        }
    }

    @SerialName("switch_server")
    @Serializable
    data class SwitchServerPacket(val server: String) : Packet {
        override suspend fun handle(connection: ConnectedSocket) {
            connection.server = server
            UserDataProviderImpl.requestDataAsync(connection.uuid!!, server)
            connection.sendPacket(OutgoingGuildPacket(connection.getSelectedGuildId()))
        }
    }

    @SerialName("invite")
    @Serializable
    data class InvitePacket(val player: String) : Packet {
        override suspend fun handle(connection: ConnectedSocket) {
            val selectedGuild = connection.getSelectedGuildId()
            if (selectedGuild == -1L) return
            val self = InterChatApi.guildManager.getMember(selectedGuild, connection.uuid!!).join()
            // check permission
            if (GuildRole.MODERATOR.ordinal < self.role().ordinal) {
                connection.sendPacket(OutgoingErrorMessagePacket("Missing permission"))
                return
            }
            // resolve uuid
            val user = InterChatApi.getUserByName(connection, player) ?: return
            // check existing member
            try {
                InterChatApi.guildManager.getMember(selectedGuild, user.id()).join()
                connection.sendPacket(OutgoingErrorMessagePacket("$player is already a member of this guild"))
                return
            } catch (e: CompletionException) {
                // not in guild, that's expected
            }
            // check if player is accepting invites
            if (!user.acceptingInvites()) {
                connection.sendPacket(OutgoingErrorMessagePacket("$player is not accepting invites"))
                return
            }
            // check ban
            InterChatApi.guildManager.getBan(selectedGuild, user.id()).join().orElse(null)?.let { ban ->
                if (ban.reasonPublic() || GuildRole.MODERATOR.ordinal >= self.role().ordinal) {
                    // if public / moderator or higher
                    connection.sendPacket(OutgoingErrorMessagePacket("$player is banned from this guild for ${ban.reason()}"))
                } else {
                    connection.sendPacket(OutgoingErrorMessagePacket("$player is banned from this guild"))
                }
                return
            }
            // add invite entry to database
            InterChatApi.queryExecutor.query("INSERT INTO `guild_invites` (`guild_id`, `target`, `actor`, `expires_at`) VALUES (?, ?, ?, ?)") { ps ->
                ps.setLong(1, selectedGuild)
                ps.setString(2, user.id().toString())
                ps.setString(3, self.uuid().toString())
                ps.setLong(4, System.currentTimeMillis() + 1000 * 60 * 5)
                ps.executeUpdate()
            }
            // notify others
            Protocol.GUILD_INVITE.send(
                JedisBoxProvider.get().pubSubHandler,
                GuildInvitePacket(selectedGuild, self.uuid(), user.id())
            )
        }
    }

    @SerialName("respond_invite")
    @Serializable
    data class RespondInvitePacket(val guildName: String, val accept: Boolean) : Packet {
        override suspend fun handle(connection: ConnectedSocket) {
            val guild = try {
                InterChatApi.guildManager.fetchGuildByName(guildName).join()
            } catch (e: CompletionException) {
                connection.sendPacket(OutgoingErrorMessagePacket("${connection.uuid} is not invited to $guildName"))
                return
            }
            val invite = try {
                InterChatApi.guildManager.getInvite(guild.id(), connection.uuid!!).join().apply { delete().join() }
            } catch (e: CompletionException) {
                connection.sendPacket(OutgoingErrorMessagePacket("${connection.uuid} is not invited to $guildName"))
                return
            }
            if (accept) {
                // check ban
                if (InterChatApi.checkBan(connection, guild.id(), connection.uuid!!) != null) {
                    return
                }
                // add member to guild
                GuildMember(guild.id(), invite.target(), GuildRole.MEMBER).update().join()
                // set selected guild
                InterChatApi.queryExecutor.query("UPDATE `players` SET `selected_guild` = ? WHERE `id` = ?") { ps ->
                    ps.setLong(1, guild.id())
                    ps.setString(2, connection.uuid.toString())
                    ps.executeUpdate()
                }
                // add log entry
                val actor = InterChatApi.userManager.fetchUser(connection.uuid!!).join()
                InterChatApi.submitLog(guild.id(), actor, "Accepted the invite from " + invite.actor() + " (" + actor.name() + ") and joined the guild")
            } else {
                connection.sendFeedback(Component.text("${guild.name()}の招待を却下しました。", NamedTextColor.GREEN))
            }
            Protocol.GUILD_INVITE_RESULT.send(
                JedisBoxProvider.get().pubSubHandler,
                GuildInviteResultPacket(invite, if (accept) GuildInviteResult.ACCEPTED else GuildInviteResult.REJECTED)
            )
        }
    }

    @SerialName("nick")
    @Serializable
    data class NickPacket(val nickname: String? = null) : Packet {
        override suspend fun handle(connection: ConnectedSocket) {
            val selectedGuild = connection.getSelectedGuildId()
            if (selectedGuild == -1L) {
                connection.sendPacket(OutgoingErrorMessagePacket("ギルドが選択されていません。"))
                return
            }
            val member = InterChatApi.guildManager.getMember(selectedGuild, connection.uuid!!).join()
            GuildMember(member.guildId(), member.uuid(), member.role(), nickname, member.hiddenByMember()).update().join()
            connection.sendFeedback(Component.text("ニックネームを設定しました。($nickname)", NamedTextColor.GREEN))
        }
    }

    @SerialName("toggle_translate")
    @Serializable
    data class ToggleTranslatePacket(val doTranslate: Boolean) : Packet {
        override suspend fun handle(connection: ConnectedSocket) {
            InterChatApi.queryExecutor.query("UPDATE `players` SET `translate_kana` = ? WHERE `id` = ?") { ps ->
                ps.setBoolean(1, doTranslate)
                ps.setString(2, connection.uuid.toString())
                ps.executeUpdate()
            }
            if (doTranslate) {
                connection.sendFeedback(Component.text("かな変換をオンにしました。", NamedTextColor.GREEN))
            } else {
                connection.sendFeedback(Component.text("かな変換をオフにしました。", NamedTextColor.GREEN))
            }
        }
    }

    @SerialName("role")
    @Serializable
    data class RolePacket(
        @SerialName("member")
        val memberName: String,
        @SerialName("role")
        val roleString: String,
    ) : Packet {
        override suspend fun handle(connection: ConnectedSocket) {
            val self = InterChatApi.guildManager.getMember(connection.getSelectedGuildId(), connection.uuid!!).join()
            // resolve uuid
            val user = InterChatApi.getUserByName(connection, memberName) ?: return
            val member = InterChatApi.guildManager.getMember(connection.getSelectedGuildId(), user.id()).join()
            val role = GuildRole.valueOf(roleString.uppercase())
            if (self.role() != GuildRole.OWNER) {
                // member must be owner
                connection.sendPacket(OutgoingErrorMessagePacket("あなたはギルドのオーナーではありません。"))
                return
            }
            if (member.role() == GuildRole.OWNER && role != GuildRole.OWNER) {
                val owners = self.guild.join().members.join().count { it.role() == GuildRole.OWNER }
                if (owners == 1) {
                    // guild must have at least one owner
                    connection.sendPacket(OutgoingErrorMessagePacket("オーナーが一人未満になるためこの操作は実行できません。"))
                    return
                }
            }
            member.update(role).join()
            connection.sendFeedback(Component.text("${user.name()}の権限を${roleString}に設定しました。", NamedTextColor.GREEN))
        }
    }

    @SerialName("toggle_invites")
    @Serializable
    data object ToggleInvitesPacket : Packet {
        override suspend fun handle(connection: ConnectedSocket) {
            val self = InterChatApi.userManager.fetchUser(connection.uuid!!).join()
            InterChatApi.queryExecutor.query("UPDATE `players` SET `accepting_invites` = ? WHERE `id` = ?") { ps ->
                ps.setBoolean(1, !self.acceptingInvites())
                ps.setString(2, connection.uuid.toString())
                ps.executeUpdate()
            }
            if (self.acceptingInvites()) {
                // now false
                connection.sendFeedback(Component.text("招待を受け取らない設定にしました。", NamedTextColor.GREEN))
            } else {
                // now true
                connection.sendFeedback(Component.text("招待を受け取る設定にしました。", NamedTextColor.GREEN))
            }
        }
    }

    @SerialName("hide_guild")
    @Serializable
    data object HideGuildPacket : Packet {
        override suspend fun handle(connection: ConnectedSocket) {
            val guild = InterChatApi.guildManager.fetchGuildById(connection.getSelectedGuildId()).join()
            val self = InterChatApi.guildManager.getMember(connection.getSelectedGuildId(), connection.uuid!!).join()
            GuildMember(self.guildId(), self.uuid(), self.role(), self.nickname(), !self.hiddenByMember()).update().join()
            if (self.hiddenByMember()) {
                // now unhidden
                connection.sendFeedback(Component.text("${guild.name()}のHide設定を解除しました。", NamedTextColor.GREEN))
            } else {
                // now hidden
                connection.sendFeedback(Component.text("${guild.name()}をHideしました。", NamedTextColor.GREEN))
            }
        }
    }

    @SerialName("hide_all")
    @Serializable
    data class HideAllPacket(val duration: String) : Packet {
        private val disableWords: List<String> = mutableListOf("off", "disable", "disabled", "no")

        override suspend fun handle(connection: ConnectedSocket) {
            val duration = if (duration.isEmpty()) {
                val current = InterChatApi.querySql("SELECT `hide_all_until` FROM `players` WHERE `id` = ?") { ps ->
                    ps.setString(1, connection.uuid.toString())
                    ps.executeQuery().use { rs ->
                        if (rs.next()) {
                            rs.getLong("hide_all_until")
                        } else {
                            0L
                        }
                    }
                }
                if (current < System.currentTimeMillis()) {
                    Duration.ofMinutes(30)
                } else {
                    Duration.ZERO
                }
            } else if (disableWords.contains(duration.lowercase())) {
                Duration.ZERO
            } else {
                try {
                    DurationUtil.convertStringToDuration(duration)
                } catch (e: IllegalArgumentException) {
                    connection.sendPacket(OutgoingErrorMessagePacket("時間の形式が無効です: $duration"))
                    return
                }
            }
            if (duration.isNegative || duration.seconds > 30 * 24 * 60 * 60) { // negative or more than 30 days
                connection.sendPacket(OutgoingErrorMessagePacket("時間は0秒以上30日以下にする必要があります。"))
                return
            }
            InterChatApi.querySql("UPDATE `players` SET `hide_all_until` = ? WHERE `id` = ?") { ps ->
                ps.setLong(1, System.currentTimeMillis() + duration.toMillis())
                ps.setString(2, connection.uuid.toString())
                ps.executeUpdate()
            }
            if (duration.isZero) {
                connection.sendFeedback(Component.text("HideAllを無効にしました。", NamedTextColor.GREEN))
            } else {
                connection.sendFeedback(Component.text("HideAllを${DurationUtil.convertDurationToString(duration)}の間有効にしました。", NamedTextColor.GREEN))
            }
        }
    }

    @SerialName("format")
    @Serializable
    data class FormatPacket(val format: String) : Packet {
        private val playerNameVariables = setOf("%playername", "%username", "%username-n")

        override suspend fun handle(connection: ConnectedSocket) {
            if (!format.contains("%msg") || playerNameVariables.none { format.contains(it) }) {
                connection.sendPacket(OutgoingErrorMessagePacket("formatには%msgと、%playername/%username/%username-nのいずれかを含める必要があります。"))
                return
            }
            val self = InterChatApi.guildManager.getMember(connection.getSelectedGuildId(), connection.uuid!!).join()
            if (GuildRole.MODERATOR < self.role()) {
                connection.sendPacket(OutgoingErrorMessagePacket("Missing permissions"))
                return
            }
            InterChatApi.querySql("UPDATE `guilds` SET `format` = ? WHERE `id` = ?") { ps ->
                ps.setString(1, format)
                ps.setLong(2, connection.getSelectedGuildId())
                ps.executeUpdate()
            }
            connection.sendFeedback(Component.text("チャット形式を変更しました: $format", NamedTextColor.GREEN))
            InterChatApi.submitLog(
                connection.getSelectedGuildId(),
                InterChatApi.userManager.fetchUser(connection.uuid!!).join(),
                "Set format to $format"
            )
        }
    }
}
