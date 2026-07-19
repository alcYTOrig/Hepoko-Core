package org.alc.hepokoCore.utils

import java.util.Date
import java.util.regex.Pattern

object TimeParser {
    private val pattern = Pattern.compile("(\\d+)([smhd])")

    fun parseTimeString(timeStr: String): Date? {
        val matcher = pattern.matcher(timeStr.lowercase())
        if (!matcher.matches()) return null

        val amount = matcher.group(1).toLong()
        val unit = matcher.group(2)

        val durationMs = when (unit) {
            "s" -> amount * 1000
            "m" -> amount * 1000 * 60
            "h" -> amount * 1000 * 60 * 60
            "d" -> amount * 1000 * 60 * 60 * 24
            else -> return null
        }
        return Date(System.currentTimeMillis() + durationMs)
    }
}