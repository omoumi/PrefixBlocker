package com.example.prefixblocker

import android.content.Context
import android.content.SharedPreferences

object PrefixStore {
    private const val PREFS = "prefix_blocker"
    private const val KEY_PREFIXES = "prefixes"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_LOG = "log"
    private const val MAX_LOG = 200

    data class BlockedCall(val time: Long, val number: String)

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(context: Context): List<String> =
        prefs(context).getString(KEY_PREFIXES, "")!!
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    fun save(context: Context, prefixes: List<String>) {
        prefs(context).edit().putString(KEY_PREFIXES, prefixes.joinToString("\n")).apply()
    }

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    private fun rawLog(context: Context): List<String> =
        prefs(context).getString(KEY_LOG, "")!!.split("\n").filter { it.isNotEmpty() }

    @Synchronized
    fun addToLog(context: Context, number: String) {
        val entry = "${System.currentTimeMillis()}|$number"
        val lines = (listOf(entry) + rawLog(context)).take(MAX_LOG)
        prefs(context).edit().putString(KEY_LOG, lines.joinToString("\n")).apply()
    }

    fun loadLog(context: Context): List<BlockedCall> =
        rawLog(context).mapNotNull { line ->
            val i = line.indexOf('|')
            val time = if (i > 0) line.substring(0, i).toLongOrNull() else null
            if (time == null) null else BlockedCall(time, line.substring(i + 1))
        }

    fun clearLog(context: Context) {
        prefs(context).edit().remove(KEY_LOG).apply()
    }

    fun normalize(number: String): String {
        val trimmed = number.trim()
        val digits = trimmed.filter { it.isDigit() }
        val n = if (trimmed.startsWith("+")) "+$digits" else digits
        return when {
            n.startsWith("+98") -> "0" + n.substring(3)
            n.startsWith("0098") -> "0" + n.substring(4)
            else -> n
        }
    }

    fun matches(context: Context, number: String): Boolean {
        val n = normalize(number)
        if (n.isEmpty()) return false
        return load(context).any { prefix ->
            val p = normalize(prefix)
            p.isNotEmpty() && n.startsWith(p)
        }
    }
}
