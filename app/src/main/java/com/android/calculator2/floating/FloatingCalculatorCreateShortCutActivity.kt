package com.android.calculator2.floating

import ai.elimu.calculator.R
import android.app.Activity
import android.content.Intent
import android.content.Intent.ShortcutIconResource
import android.os.Bundle

/**
 * Created by Will on 4/9/2014.
 */
class FloatingCalculatorCreateShortCutActivity : Activity() {
    public override fun onCreate(state: Bundle?) {
        super.onCreate(state)

        if (Intent.ACTION_CREATE_SHORTCUT == getIntent().getAction()) {
            // create shortcut if requested
            val icon = ShortcutIconResource.fromContext(this, R.drawable.ic_launcher_floating)

            val intent = Intent()
            val launchIntent = Intent(this, FloatingCalculatorOpenShortCutActivity::class.java)

            intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, launchIntent)
            intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.app_name))
            intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon)

            setResult(RESULT_OK, intent)
            finish()
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.blank, R.anim.blank)
    }
}
