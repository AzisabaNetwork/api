package net.azisaba.api.velocity

import org.intellij.lang.annotations.Language
import java.sql.PreparedStatement

object DatabaseManager {
    private val dataSource = PluginConfig.instance.database.createDataSource()

    fun <T> execute(@Language("SQL") query: String, action: (PreparedStatement) -> T): T =
        dataSource.connection.use { connection ->
            connection.prepareStatement(query).use { statement ->
                action(statement)
            }
        }
}
