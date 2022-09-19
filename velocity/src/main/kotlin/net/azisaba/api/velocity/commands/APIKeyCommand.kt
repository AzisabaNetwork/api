package net.azisaba.api.velocity.commands

import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import net.azisaba.api.velocity.DatabaseManager
import net.azisaba.api.schemes.APIKey
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
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
        val existingAPIKey = APIKey.select("SELECT * FROM `api_keys` WHERE `player` = ? LIMIT 1", player.uniqueId.toString()).firstOrNull()

        if (existingAPIKey != null) {
            if (force) {
                DatabaseManager.execute("DELETE FROM `api_keys` WHERE `player` = ?") {
                    it.setString(1, player.uniqueId.toString())
                    it.executeUpdate()
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

        val newAPIKey = APIKey(0, UUID.randomUUID().toString(), player.uniqueId, System.currentTimeMillis(), 0)
        APIKey.insertB("api_keys", newAPIKey) { put("id", null); put("0", null) }

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
