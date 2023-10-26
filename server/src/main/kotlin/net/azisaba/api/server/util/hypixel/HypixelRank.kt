package net.azisaba.api.server.util.hypixel


fun translateChatColor(s: String) =
    when (s) {
        "BLACK" -> "§0"
        "DARK_BLUE" -> "§1"
        "DARK_GREEN" -> "§2"
        "DARK_AQUA" -> "§3"
        "DARK_RED" -> "§4"
        "DARK_PURPLE" -> "§5"
        "GOLD" -> "§6"
        "GRAY" -> "§7"
        "DARK_GRAY" -> "§8"
        "BLUE" -> "§9"
        "GREEN" -> "§a"
        "AQUA" -> "§b"
        "RED" -> "§c"
        "LIGHT_PURPLE" -> "§d"
        "YELLOW" -> "§e"
        "WHITE" -> "§f"
        "MAGIC" -> "§k"
        "BOLD" -> "§l"
        "STRIKETHROUGH" -> "§m"
        "UNDERLINE" -> "§n"
        "ITALIC" -> "§o"
        "RESET" -> "§r"
        else -> ""
    }

enum class HypixelRank(val prefix: (monthlyRankColor: String, rankPlusColor: String) -> String) {
    ADMIN({ _, _ -> "§c[ADMIN] " }),
    GAME_MASTER({ _, _ -> "§2[GM] " }),
    MODERATOR({ _, _ -> "§2[MOD] " }),
    YOUTUBER({ _, _ -> "§c[§fYOUTUBE§c] " }),
    SUPERSTAR({ m, r -> "${translateChatColor(m)}[MVP${translateChatColor(r)}++${translateChatColor(m)}] " }),
    MVP_PLUS({ _, r -> "§b[MVP${translateChatColor(r)}+§b] " }),
    MVP({ _, _ -> "§b[MVP] " }),
    VIP_PLUS({ _, _ -> "§a[VIP§6+§a] " }),
    VIP({ _, _ -> "§a[VIP] " }),
    NORMAL({ _, _ -> "§7" }),
    NONE({ _, _ -> "§7" }),
    ;
}
