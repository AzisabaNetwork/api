package net.azisaba.api.spigot

import net.azisaba.api.spigot.privacy.PrivacySettings
import java.sql.DriverManager
import java.util.UUID

object DatabaseManager {
    private fun <T> withConnection(action: (java.sql.Connection) -> T): T {
        val config = PluginConfig.instance.database
        return DriverManager.getConnection(config.jdbcUrl, config.connectionProperties()).use(action)
    }

    fun getPrivacySettings(uuid: UUID): PrivacySettings = withConnection { connection ->
        connection.prepareStatement(
            "SELECT share_inventory, share_enderchest FROM player_data_privacy WHERE player_uuid = ?"
        ).use { statement ->
            statement.setString(1, uuid.toString())
            statement.executeQuery().use { result ->
                if (result.next()) {
                    PrivacySettings(
                        shareInventory = result.getBoolean("share_inventory"),
                        shareEnderChest = result.getBoolean("share_enderchest"),
                    )
                } else {
                    PrivacySettings()
                }
            }
        }
    }

    fun setShareInventory(uuid: UUID, enabled: Boolean) = withConnection { connection ->
        connection.prepareStatement(
            """
            INSERT INTO player_data_privacy
                (player_uuid, share_inventory, share_enderchest, updated_at)
            VALUES (?, ?, FALSE, ?)
            ON DUPLICATE KEY UPDATE
                share_inventory = VALUES(share_inventory), updated_at = VALUES(updated_at)
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, uuid.toString())
            statement.setBoolean(2, enabled)
            statement.setLong(3, System.currentTimeMillis())
            statement.executeUpdate()
        }
    }

    fun setShareEnderChest(uuid: UUID, enabled: Boolean) = withConnection { connection ->
        connection.prepareStatement(
            """
            INSERT INTO player_data_privacy
                (player_uuid, share_inventory, share_enderchest, updated_at)
            VALUES (?, FALSE, ?, ?)
            ON DUPLICATE KEY UPDATE
                share_enderchest = VALUES(share_enderchest), updated_at = VALUES(updated_at)
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, uuid.toString())
            statement.setBoolean(2, enabled)
            statement.setLong(3, System.currentTimeMillis())
            statement.executeUpdate()
        }
    }
}
