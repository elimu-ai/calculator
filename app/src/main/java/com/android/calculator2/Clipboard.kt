package com.android.calculator2

import ai.elimu.calculator.R
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast

/**
 * Simplify Android copy/paste
 */
object Clipboard {
    @JvmStatic
    fun copy(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(null, text))
        val toastText =
            String.format(context.getResources().getString(R.string.text_copied_toast), text)
        Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
    }

    @JvmStatic
    fun paste(context: Context): String? {
        val clip = getPrimaryClip(context)
        if (clip != null) {
            for (i in 0..<clip.itemCount) {
                val paste = clip.getItemAt(i).coerceToText(context)
                if (paste.length > 0) {
                    return paste.toString()
                }
            }
        }
        return null
    }

    @JvmStatic
    fun canPaste(context: Context): Boolean {
        return paste(context) != null
    }


    private fun getPrimaryClip(context: Context): ClipData? {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        return clipboard.primaryClip
    }

    private fun setPrimaryClip(context: Context, clip: ClipData) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(clip)
    }
}
