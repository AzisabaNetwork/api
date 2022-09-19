package net.azisaba.api.spigot
/*
import net.azisaba.api.sql.QueryExecutor
import org.intellij.lang.annotations.Language
import java.sql.PreparedStatement

object DatabaseManager : QueryExecutor {
    init {
        QueryExecutor.executor = this
    }

    private val dataSource = PluginConfig.instance.database.createDataSource()

    override fun <T> execute(@Language("SQL") query: String, action: (PreparedStatement) -> T): T =
        dataSource.connection.use { connection ->
            connection.prepareStatement(query).use { statement ->
                action(statement)
            }
        }
}
*/
