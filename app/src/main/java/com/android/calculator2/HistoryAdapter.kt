/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.calculator2

import ai.elimu.calculator.R
import android.content.Context
import android.text.Html
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.calculator2.view.HistoryLine
import com.xlythe.math.EquationFormatter
import com.xlythe.math.History
import com.xlythe.math.HistoryEntry
import java.util.Vector

open class HistoryAdapter(val context: Context?, history: History, callback: HistoryItemCallback) :
    RecyclerView.Adapter<HistoryAdapter.ViewHolder?>() {
    private val mEntries: Vector<HistoryEntry> = history.entries
    private val mEquationFormatter: EquationFormatter = EquationFormatter()
    protected var mCallback: HistoryItemCallback = callback

    interface HistoryItemCallback {
        fun onHistoryItemSelected(entry: HistoryEntry?)
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var historyExpr: TextView = v.findViewById<View?>(R.id.historyExpr) as TextView
        var historyResult: TextView = v.findViewById<View?>(R.id.historyResult) as TextView
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view =
            LayoutInflater.from(this.context)
                .inflate(R.layout.history_entry, parent, false) as HistoryLine
        return ViewHolder(view)
    }

    protected open val layoutResourceId: Int
        get() = R.layout.history_entry

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val view = holder.itemView as HistoryLine
        val entry = mEntries.elementAt(position)
        view.adapter = this@HistoryAdapter
        view.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                mCallback.onHistoryItemSelected(entry)
            }
        })
        holder.historyExpr.setText(formatText(entry.base))
        holder.historyResult.setText(entry.edited)
    }

    override fun getItemCount(): Int {
        return mEntries.size - 1
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    protected fun formatText(text: String): Spanned? {
        return Html.fromHtml(mEquationFormatter.insertSupScripts(text))
    }
}
