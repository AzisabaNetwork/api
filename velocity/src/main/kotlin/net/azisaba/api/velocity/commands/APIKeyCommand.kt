package net.azisaba.api.velocity.commands

import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import net.azisaba.api.schemas.AzisabaAPI
import net.azisaba.api.velocity.DatabaseManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

object APIKeyCommand : AbstractCommand() {
    override fun createBuilder(): LiteralArgumentBuilder<CommandSource> =
        literal("apikey")
            .requires { it is Player && it.hasPermission("azisabaapi.apikey") }
            .then(argument("force", BoolArgumentType.bool())
                .executes { execute(it.source as Player, BoolArgumentType.getBool(it, "force")) }
            )
            .executes { execute(it.source as Player, false) }

    private fun execute(player: Player, force: Boolean): Int {
        val existingAPIKey = transaction(DatabaseManager.azisabaApi) {
            AzisabaAPI.APIKey.find(AzisabaAPI.APIKeyTable.player eq player.uniqueId.toString()).firstOrNull()
        }

        if (existingAPIKey != null) {
            if (force) {
                transaction(DatabaseManager.azisabaApi) {
                    existingAPIKey.delete()
                }
            } else {
                player.sendMessage(
                    Component.text("すでにAPIキーが存在します。", NamedTextColor.RED)
                        .append(Component.text("/apikey true", NamedTextColor.YELLOW))
                        .append(Component.text("で再発行できますが、元のAPIキーは使用できなくなります。", NamedTextColor.RED))
                )
                return 0
            }
        }

        val newAPIKey = transaction(DatabaseManager.azisabaApi) {
            AzisabaAPI.APIKey.new {
                this.player = player.uniqueId.toString()
                this.key = UUID.randomUUID().toString() // UUID.randomUUID() uses SecureRandom, so it is ok
                this.createdAt = System.currentTimeMillis()
            }
        }

        // we can't just send the key as chat message because it is logged in client's latest.log
        // however, `click_event`s are not logged (at least on vanilla), so we can use that to send the key.
        player.sendMessage(
            Component.text("APIキーを発行しました。コピーするには", NamedTextColor.GREEN)
                .append(
                    Component.text("ここをクリック", NamedTextColor.AQUA)
                        .decorate(TextDecoration.UNDERLINED)
                        .hoverEvent(HoverEvent.showText(Component.text("クリックでコピー")))
                        .clickEvent(ClickEvent.copyToClipboard(newAPIKey.key))
                )
                .append(Component.text("してください。", NamedTextColor.GREEN)))
        player.sendMessage(Component.text("再発行は/apikey trueで実行できます。実行すると元のAPIキーは使用できなくなります。", NamedTextColor.YELLOW))
        return 0
    }
}
