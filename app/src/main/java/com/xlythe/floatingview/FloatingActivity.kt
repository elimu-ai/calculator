package com.xlythe.floatingview

import ai.elimu.calculator.R
import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager

/**
 * Created by Will on 4/9/2014.
 */
class FloatingActivity : Activity() {
    @SuppressLint("InlinedApi")
    public override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        if (getIntent().getBooleanExtra(EXTRA_HIDE_STATUS_BAR, false)) {
            if (Build.VERSION.SDK_INT < 16) {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
                )
            } else {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ACTIVE_ACTIVITY = this
    }

    override fun onPause() {
        super.onPause()
        if (FloatingView.ACTIVE_VIEW != null) FloatingView.ACTIVE_VIEW.closeView()
        ACTIVE_ACTIVITY = null
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.blank, R.anim.blank)
    }

    companion object {
        const val EXTRA_HIDE_STATUS_BAR: String = "hide_status_bar"
        @JvmField
        var ACTIVE_ACTIVITY: FloatingActivity? = null
    }
}
