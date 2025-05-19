package com.android.calculator2.view

import ai.elimu.calculator.R
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.widget.LinearLayout
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.xlythe.math.History
import com.xlythe.math.HistoryEntry

class HistoryLine(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private var mMenuItemsStrings: Array<String>? = null
    private var mHistoryEntry: HistoryEntry? = null
    private var mHistory: History? = null
    private var mAdapter: RecyclerView.Adapter<*>? = null

    public override fun onCreateContextMenu(menu: ContextMenu) {
        val handler = MenuHandler()
        if (mMenuItemsStrings == null) {
            val resources = getResources()
            mMenuItemsStrings = arrayOfNulls<String>(4) as Array<String>?
            mMenuItemsStrings!![COPY] = String.format(
                resources.getString(R.string.copy),
                mHistoryEntry!!.base + "=" + mHistoryEntry!!.edited
            )
            mMenuItemsStrings!![COPY_BASE] =
                String.format(resources.getString(R.string.copy), mHistoryEntry!!.base)
            mMenuItemsStrings!![COPY_EDITED] =
                String.format(resources.getString(R.string.copy), mHistoryEntry!!.edited)
            mMenuItemsStrings!![REMOVE] = resources.getString(R.string.remove_from_history)
        }
        for (i in mMenuItemsStrings!!.indices) {
            menu.add(Menu.NONE, i, i, mMenuItemsStrings!![i]).setOnMenuItemClickListener(handler)
        }
    }

    fun onTextContextMenuItem(title: CharSequence?): Boolean {
        var handled = false
        if (TextUtils.equals(title, mMenuItemsStrings!![COPY])) {
            copyContent(mHistoryEntry!!.base + "=" + mHistoryEntry!!.edited)
            handled = true
        } else if (TextUtils.equals(title, mMenuItemsStrings!![COPY_BASE])) {
            copyContent(mHistoryEntry!!.base)
            handled = true
        } else if (TextUtils.equals(title, mMenuItemsStrings!![COPY_EDITED])) {
            copyContent(mHistoryEntry!!.edited!!)
            handled = true
        } else if (TextUtils.equals(title, mMenuItemsStrings!![REMOVE])) {
            removeContent()
            handled = true
        }
        return handled
    }

    fun copyContent(content: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(null, content))
        val toastText = String.format(resources.getString(R.string.text_copied_toast), content)
        Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
    }

    private fun removeContent() {
        mHistory!!.remove(mHistoryEntry)
        mAdapter!!.notifyDataSetChanged()
    }

    var historyEntry: HistoryEntry
        get() = mHistoryEntry!!
        set(historyEntry) {
            this.mHistoryEntry = historyEntry
        }

    var history: History?
        get() = mHistory
        set(history) {
            this.mHistory = history
        }

    var adapter: RecyclerView.Adapter<*>
        get() = mAdapter!!
        set(adapter) {
            this.mAdapter = adapter
        }

    fun showMenu() {
        showContextMenu()
    }

    private inner class MenuHandler : MenuItem.OnMenuItemClickListener {
        override fun onMenuItemClick(item: MenuItem): Boolean {
            return onTextContextMenuItem(item.title)
        }
    }

    companion object {
        private const val COPY = 0
        private const val COPY_BASE = 1
        private const val COPY_EDITED = 2
        private const val REMOVE = 3
    }
}
