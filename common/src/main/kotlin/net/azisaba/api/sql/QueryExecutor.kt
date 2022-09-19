package net.azisaba.api.sql

import org.intellij.lang.annotations.Language
import java.sql.PreparedStatement

interface QueryExecutor {
    companion object {
        @JvmStatic
        lateinit var executor: QueryExecutor
    }

    fun <T> execute(@Language("SQL") query: String, action: (PreparedStatement) -> T): T
}
