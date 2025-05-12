package com.android.calculator2.floating

import ai.elimu.calculator.R
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.Settings

/**
 * Created by Will on 4/9/2014.
 */
class FloatingCalculatorOpenShortCutActivity : Activity() {
    private val mHandler = Handler()

    public override fun onCreate(state: Bundle?) {
        super.onCreate(state)

        if (!Settings.canDrawOverlays(this)) {
            mHandler.post(object : Runnable {
                override fun run() {
                    requestSystemWindowPermission()
                }
            })
        } else {
            startFloatingCalculator()
        }
    }

    private fun startFloatingCalculator() {
        val intent = Intent(this, FloatingCalculator::class.java)
        startService(intent)
        finish()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.blank, R.anim.blank)
    }

    private fun requestSystemWindowPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:" + getPackageName())
        )
        startActivityForResult(intent, REQUEST_SYSTEM_WINDOW_PERMISSION)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_SYSTEM_WINDOW_PERMISSION) {
            if (Settings.canDrawOverlays(this)) {
                startFloatingCalculator()
            } else {
                finish()
            }
        }
    }

    companion object {
        private const val REQUEST_SYSTEM_WINDOW_PERMISSION = 69
    }
}
