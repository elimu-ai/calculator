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

import android.content.Context
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class Persist(private val mContext: Context) {
    var history: History = History()
    var deleteMode: Int = 0
    var mode: Base? = null

    fun load() {
        try {
            val `is`: InputStream = BufferedInputStream(mContext.openFileInput(FILE_NAME), 8192)
            val `in` = DataInputStream(`is`)
            val version = `in`.readInt()
            if (version > LAST_VERSION) {
                throw IOException("data version $version; expected $LAST_VERSION")
            }
            if (version > 1) {
                this.deleteMode = `in`.readInt()
            }
            if (version > 2) {
                val quickSerializable = `in`.readInt()
                for (m in Base.entries) {
                    if (m.quickSerializable == quickSerializable) this.mode = m
                }
            }
            this.history = History(version, `in`)
            `in`.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun save() {
        try {
            val os: OutputStream = BufferedOutputStream(mContext.openFileOutput(FILE_NAME, 0), 8192)
            val out = DataOutputStream(os)
            out.writeInt(LAST_VERSION)
            out.writeInt(this.deleteMode)
            out.writeInt(if (this.mode == null) Base.DECIMAL.quickSerializable else mode!!.quickSerializable)
            history.write(out)
            out.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val LAST_VERSION = 3
        private const val FILE_NAME = "calculator.data"
    }
}
