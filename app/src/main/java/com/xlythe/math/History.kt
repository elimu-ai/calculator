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
package com.xlythe.math

import androidx.recyclerview.widget.RecyclerView
import java.io.DataInput
import java.io.DataOutput
import java.io.IOException
import java.util.Vector

class History {
    val entries: Vector<HistoryEntry> = Vector<HistoryEntry>()
    private var mPos = 0
    private var mObserver: RecyclerView.Adapter<*>? = null

    internal constructor() {
        clear()
    }

    fun clear() {
        entries.clear()
        entries.add(HistoryEntry("", ""))
        mPos = 0
        notifyChanged()
    }

    private fun notifyChanged() {
        if (mObserver != null) {
            mObserver!!.notifyDataSetChanged()
        }
    }

    internal constructor(version: Int, `in`: DataInput) {
        if (version >= VERSION_1) {
            val size = `in`.readInt()
            for (i in 0..<size) {
                entries.add(HistoryEntry(version, `in`))
            }
            mPos = `in`.readInt()
        } else {
            throw IOException("invalid version $version")
        }
    }

    fun setObserver(observer: RecyclerView.Adapter<*>?) {
        mObserver = observer
    }

    @Throws(IOException::class)
    fun write(out: DataOutput) {
        out.writeInt(entries.size)
        for (entry in this.entries) {
            entry.write(out)
        }
        out.writeInt(mPos)
    }

    fun update(text: String?) {
        current()!!.edited = text
    }

    fun current(): HistoryEntry? {
        return entries.elementAt(mPos)
    }

    fun moveToPrevious(): Boolean {
        if (mPos > 0) {
            --mPos
            return true
        }
        return false
    }

    fun moveToNext(): Boolean {
        if (mPos < entries.size - 1) {
            ++mPos
            return true
        }
        return false
    }

    fun enter(formula: String, result: String?) {
        current()!!.clearEdited()
        if (entries.size >= MAX_ENTRIES) {
            entries.removeAt(0)
        }
        if ((entries.size < 2 || formula != entries.elementAt(entries.size - 2).base) && !formula.isEmpty() && !formula.isEmpty()) {
            entries.insertElementAt(HistoryEntry(formula, result), entries.size - 1)
        }
        mPos = entries.size - 1
        notifyChanged()
    }

    val text: String?
        get() = current()!!.edited

    val base: String
        get() = current()!!.base

    fun remove(he: HistoryEntry?) {
        entries.remove(he)
        mPos--
    }

    companion object {
        private const val VERSION_1 = 1
        private const val MAX_ENTRIES = 100
    }
}
