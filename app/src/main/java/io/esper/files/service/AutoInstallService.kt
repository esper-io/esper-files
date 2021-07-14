package io.esper.files.service

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import io.esper.files.util.InstallUtil.jumpToAccessibilitySetting


class AutoInstallService : AccessibilityService() {
    private val mHandler = Handler()
    override fun onServiceConnected() {
        Log.i(TAG, "onServiceConnected: ")
//        Toast.makeText(this, "Files Install Service Activated", Toast.LENGTH_SHORT).show()
        performGlobalAction(GLOBAL_ACTION_BACK)
        mHandler.postDelayed({ performGlobalAction(GLOBAL_ACTION_BACK) }, DELAY_PAGE.toLong())
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy: ")
        Toast.makeText(this, "Files Install Service Stopped. Please Restart!", Toast.LENGTH_SHORT)
            .show()
        jumpToAccessibilitySetting(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        try {
            if (!event.packageName.toString()
                    .contains("packageinstaller")
            )
                return
            Log.i(TAG, "onAccessibilityEvent: $event")
            val eventNode = event.source
//            if (eventNode == null) {
//                performGlobalAction(GLOBAL_ACTION_RECENTS)
//                mHandler.postDelayed({
//                    performGlobalAction(GLOBAL_ACTION_BACK)
//                }, DELAY_PAGE.toLong())
//                return
//            }

            val rootNode = rootInActiveWindow ?: return
            Log.i(TAG, "rootNode: $rootNode")
            findTxtClick(rootNode, "continue")
            findTxtClick(rootNode, "install")
            findTxtClick(rootNode, "next")
            findTxtClick(rootNode, "done")
            findTxtClick(rootNode, "turn")
            findTxtClick(rootNode, "allow")
            eventNode.recycle()
            rootNode.recycle()
        } catch (e: Exception) {
        } finally {

        }
    }

    private fun findTxtClick(nodeInfo: AccessibilityNodeInfo, txt: String) {
        val nodes = nodeInfo.findAccessibilityNodeInfosByText(txt)
        if (nodes == null || nodes.isEmpty()) return
        Log.i(TAG, "findTxtClick: " + txt + ", " + nodes.size + ", " + nodes)
        for (node in nodes) {
            //Checkbox action maybe needed in Huawei Phones
            if (node.isEnabled && node.isClickable && (node.className == "android.widget.Button" || node.className == "android.widget.CheckBox"
                        )
            ) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        }
    }

    override fun onInterrupt() {}

    companion object {
        private val TAG = AutoInstallService::class.java.simpleName
        private const val DELAY_PAGE = 320
    }
}