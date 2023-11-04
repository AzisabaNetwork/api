package net.azisaba.api.server.util

import java.time.Duration
import java.util.*

object DurationUtil {
    @Throws(IllegalArgumentException::class)
    fun convertStringToDuration(durationString: String): Duration {
        var str = durationString
        str = str.lowercase()
        var duration = Duration.ofSeconds(0)
        while (str.isNotEmpty()) {
            val indexes = mutableListOf(str.indexOf("d"), str.indexOf("h"), str.indexOf("m"), str.indexOf("s"))
            indexes.removeIf { it < 0 }
            val minIndex = indexes.toIntArray().min()
            val numStr = str.substring(0, minIndex)
            require(numStr.isNotEmpty())
            val amount = try {
                numStr.toInt()
            } catch (ex: NumberFormatException) {
                throw IllegalArgumentException()
            }
            require(amount >= 0)
            val unit = str[minIndex]
            when (unit) {
                'd' -> duration = duration.plusDays(amount.toLong())
                'h' -> duration = duration.plusHours(amount.toLong())
                'm' -> duration = duration.plusMinutes(amount.toLong())
                's' -> duration = duration.plusSeconds(amount.toLong())
            }
            str = str.substring(minIndex + 1)
        }
        return duration
    }

    fun convertDurationToString(duration: Duration): String {
        var newDuration = duration
        var str = ""
        if (newDuration.toDays() > 0) {
            str += if (newDuration.seconds > 1) {
                "${newDuration.seconds}日"
            } else {
                "${newDuration.seconds}日"
            }
            newDuration = newDuration.minusDays(newDuration.toDays())
        }
        if (newDuration.toHours() > 0) {
            str += if (newDuration.seconds > 1) {
                "${newDuration.seconds}時間"
            } else {
                "${newDuration.seconds}時間"
            }
            newDuration = newDuration.minusHours(newDuration.toHours())
        }
        if (newDuration.toMinutes() > 0) {
            str += if (newDuration.seconds > 1) {
                "${newDuration.seconds}分"
            } else {
                "${newDuration.seconds}分"
            }
            newDuration = newDuration.minusMinutes(newDuration.toMinutes())
        }
        if (newDuration.seconds > 0) {
            str += if (newDuration.seconds > 1) {
                "${newDuration.seconds}秒"
            } else {
                "${newDuration.seconds}秒"
            }
            //duration = duration.minusSeconds(duration.getSeconds());
        }
        return str.trim { it <= ' ' }
    }
}
