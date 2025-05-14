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
package com.android.calculator2.view

import android.text.Editable
import android.text.SpannableStringBuilder
import com.xlythe.math.Constants
import com.xlythe.math.Solver

class CalculatorEditable private constructor(source: CharSequence?) :
    SpannableStringBuilder(source) {
    private var isInsideReplace = false

    override fun replace(
        start: Int,
        end: Int,
        tb: CharSequence,
        tbstart: Int,
        tbend: Int
    ): SpannableStringBuilder? {
        if (isInsideReplace) {
            return super.replace(start, end, tb, tbstart, tbend)
        } else {
            isInsideReplace = true
            try {
                val delta = tb.subSequence(tbstart, tbend).toString()
                return internalReplace(start, end, delta)
            } finally {
                isInsideReplace = false
            }
        }
    }

    private fun internalReplace(start: Int, end: Int, delta: String): SpannableStringBuilder? {
        var start = start
        var delta = delta
        for (i in ORIGINALS.indices.reversed()) {
            delta = delta.replace(ORIGINALS[i], REPLACEMENTS[i])
        }

        val length = delta.length
        if (length == 1) {
            val text = delta[0]

            // don't allow two dots in the same number
            if (text == Constants.DECIMAL_POINT) {
                var p = start - 1
                while (p >= 0 && Solver.isDigit(get(p))
                    && (get(p) != Constants.DECIMAL_POINT)
                ) {
                    --p
                }
                if (p >= 0 && get(p) == Constants.DECIMAL_POINT) {
                    return super.replace(start, end, "")
                }
            }

            var prevChar = if (start > 0) get(start - 1) else '\u0000'

            // don't allow 2 successive minuses
            if (text == Constants.MINUS && prevChar == Constants.MINUS) {
                return super.replace(start, end, "")
            }

            // don't allow multiple successive operators unless we're dealing with negatives
            if (Solver.isOperator(text) && text != Constants.MINUS) {
                while (Solver.isOperator(prevChar)) {
                    if (start == 1) {
                        return super.replace(start, end, "")
                    }

                    --start
                    prevChar = if (start > 0) get(start - 1) else '\u0000'
                }
            }
        }
        return super.replace(start, end, delta)
    }

    class Factory : Editable.Factory() {
        override fun newEditable(source: CharSequence?): Editable {
            return CalculatorEditable(source)
        }
    }

    companion object {
        private val ORIGINALS = charArrayOf('-', '*', '/')
        private val REPLACEMENTS = charArrayOf('\u2212', '\u00d7', '\u00f7')
    }
}
