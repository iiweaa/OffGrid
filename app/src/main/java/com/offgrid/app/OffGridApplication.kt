package com.offgrid.app

import android.app.Application
import android.util.Log
import com.offgrid.app.link.node.NodeId
import com.offgrid.app.link.node.NodeIdStore

class OffGridApplication : Application() {

    var localNodeId: NodeId = NodeId(0L)
        private set

    override fun onCreate() {
        super.onCreate()
        localNodeId = NodeIdStore(this).getOrCreate()
        Log.i(TAG, "Local NodeId: ${localNodeId.toHex()}")
    }

    companion object {
        private const val TAG = "OffGridApplication"
    }
}
