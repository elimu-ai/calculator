package com.android.calculator2.view.display

import android.content.Context
import android.text.TextUtils
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import com.android.calculator2.Clipboard
import com.android.calculator2.Clipboard.canPaste
import com.android.calculator2.Clipboard.paste

/**
 * Created by Will on 12/13/2014.
 */
internal class MenuHandler(private val mDisplay: AdvancedDisplay) :
    MenuItem.OnMenuItemClickListener {
    // For copy/paste
    private val mMenuItemsStrings: Array<String?>

    init {
        val resources = context.resources
        mMenuItemsStrings = arrayOfNulls<String>(3)
        mMenuItemsStrings[CUT] = resources.getString(android.R.string.cut)
        mMenuItemsStrings[COPY] = resources.getString(android.R.string.copy)
        mMenuItemsStrings[PASTE] = resources.getString(android.R.string.paste)
    }

    fun onCreateContextMenu(menu: ContextMenu) {
        for (i in mMenuItemsStrings.indices) {
            menu.add(Menu.NONE, i, i, mMenuItemsStrings[i]).setOnMenuItemClickListener(this)
        }
        if (this.text!!.isEmpty()) {
            menu.getItem(CUT).isVisible = false
            menu.getItem(COPY).isVisible = false
        }
        if (!canPaste(this.context)) {
            menu.getItem(PASTE).isVisible = false
        }
    }

    val context: Context
        get() = mDisplay.context

    val text: String?
        get() = mDisplay.text

    override fun onMenuItemClick(item: MenuItem): Boolean {
        return onTextContextMenuItem(item.title)
    }

    fun onTextContextMenuItem(title: CharSequence?): Boolean {
        var handled = false
        if (TextUtils.equals(title, mMenuItemsStrings[CUT])) {
            cutContent()
            handled = true
        } else if (TextUtils.equals(title, mMenuItemsStrings[COPY])) {
            copyContent()
            handled = true
        } else if (TextUtils.equals(title, mMenuItemsStrings[PASTE])) {
            pasteContent()
            handled = true
        }
        return handled
    }


    private fun copyContent() {
        Clipboard.copy(this.context, this.text!!)
    }

    private fun cutContent() {
        Clipboard.copy(this.context, this.text!!)
        mDisplay.clear()
    }

    private fun pasteContent() {
        paste(this.context)?.let {
            mDisplay.insert(it)
        }
    }

    companion object {
        private const val CUT = 0
        private const val COPY = 1
        private const val PASTE = 2
    }
}
