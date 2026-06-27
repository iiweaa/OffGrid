package com.offgrid.app.link.wifidirect

import android.content.Context

object NetworkConfig {
    private const val PREFS_NAME = "offgrid_network"
    private const val KEY_ROLE = "network_role"
    private const val KEY_GROUP_NAME = "group_name"
    private const val KEY_PASSPHRASE = "passphrase"

    fun getRole(context: Context): NetworkRole {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return try {
            NetworkRole.valueOf(prefs.getString(KEY_ROLE, NetworkRole.AUTO.name)!!)
        } catch (_: Exception) {
            NetworkRole.AUTO
        }
    }

    fun setRole(context: Context, role: NetworkRole) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ROLE, role.name)
            .apply()
    }

    fun getGroupName(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_GROUP_NAME, "") ?: ""
    }

    fun setGroupName(context: Context, name: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_GROUP_NAME, name.trim())
            .apply()
    }

    fun getPassphrase(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PASSPHRASE, "") ?: ""
    }

    fun setPassphrase(context: Context, passphrase: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PASSPHRASE, passphrase.trim())
            .apply()
    }
}
