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

import java.io.DataInput
import java.io.DataOutput
import java.io.IOException

class HistoryEntry {
    private var mBase: String? = null
    var edited: String? = null

    internal constructor(base: String, edited: String?) {
        mBase = base
        this.edited = edited
    }

    internal constructor(version: Int, `in`: DataInput) {
        if (version >= VERSION_1) {
            mBase = `in`.readUTF()
            this.edited = `in`.readUTF()
        } else {
            throw IOException("invalid version $version")
        }
    }

    @Throws(IOException::class)
    fun write(out: DataOutput) {
        out.writeUTF(mBase)
        out.writeUTF(this.edited)
    }

    override fun toString(): String {
        return mBase!!
    }

    fun clearEdited() {
        this.edited = mBase
    }

    val base: String
        get() = mBase!!

    companion object {
        private const val VERSION_1 = 1
    }
}
