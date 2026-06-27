package com.offgrid.app.link.node

import android.content.Context

/**
 * Persists the local [NodeId] across process restarts.
 *
 * The identifier is generated once per app install and stored in a private
 * SharedPreferences file.
 */
class NodeIdStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Returns the existing NodeId or creates and persists a new random one.
     */
    fun getOrCreate(): NodeId {
        val existing = prefs.getString(KEY_NODE_ID, null)
        if (existing != null) {
            NodeId.parse(existing)?.let { return it }
        }
        val newId = NodeId.random()
        prefs.edit().putString(KEY_NODE_ID, newId.toHex()).apply()
        return newId
    }

    companion object {
        private const val PREFS_NAME = "offgrid_node"
        private const val KEY_NODE_ID = "node_id"
    }
}
