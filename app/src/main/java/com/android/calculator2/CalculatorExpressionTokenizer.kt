/*
* Copyright (C) 2014 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the 'License');
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an 'AS IS' BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.android.calculator2

import ai.elimu.calculator.R
import android.content.Context
import com.xlythe.math.Constants
import java.text.DecimalFormatSymbols
import java.util.LinkedList
import java.util.Locale

class CalculatorExpressionTokenizer(context: Context) {
    private val mReplacements: MutableList<Localizer>
    private val mUseDegrees = false

    init {
        mReplacements = LinkedList<Localizer>()

        var locale = context.resources.configuration.locale
        if (!context.resources.getBoolean(R.bool.use_localized_digits)) {
            locale = Locale.Builder()
                .setLocale(locale)
                .setUnicodeLocaleKeyword("nu", "latn")
                .build()
        }

        val symbols = DecimalFormatSymbols(locale)
        val zeroDigit = symbols.getZeroDigit()

        for (i in 0..9) {
            mReplacements.add(
                Localizer(
                    i.toString(), (i + zeroDigit.code).toChar().toString()
                )
            )
        }

        mReplacements.add(
            Localizer(
                ",",
                Constants.MATRIX_SEPARATOR.toString()
            )
        )
        mReplacements.add(
            Localizer(
                ".",
                symbols.decimalSeparator.toString()
            )
        )
        mReplacements.add(
            Localizer(
                "/",
                context.getString(R.string.op_div)
            )
        )
        mReplacements.add(
            Localizer(
                "*",
                context.getString(R.string.op_mul)
            )
        )
        mReplacements.add(
            Localizer(
                "-",
                context.getString(R.string.op_sub)
            )
        )
        mReplacements.add(
            Localizer(
                "asin",
                context.getString(R.string.arcsin)
            )
        )
        mReplacements.add(
            Localizer(
                "acos",
                context.getString(R.string.arccos)
            )
        )
        mReplacements.add(
            Localizer(
                "atan",
                context.getString(R.string.arctan)
            )
        )
        mReplacements.add(
            Localizer(
                "sin",
                context.getString(R.string.fun_sin)
            )
        )
        mReplacements.add(
            Localizer(
                "cos",
                context.getString(R.string.fun_cos)
            )
        )
        mReplacements.add(
            Localizer(
                "tan",
                context.getString(R.string.fun_tan)
            )
        )
        if (mUseDegrees) {
            mReplacements.add(Localizer("sin", "sind"))
            mReplacements.add(Localizer("cos", "cosd"))
            mReplacements.add(Localizer("tan", "tand"))
        }
        mReplacements.add(
            Localizer(
                "ln",
                context.getString(R.string.fun_ln)
            )
        )
        mReplacements.add(
            Localizer(
                "log",
                context.getString(R.string.fun_log)
            )
        )
        mReplacements.add(
            Localizer(
                "det",
                context.getString(R.string.det)
            )
        )
        mReplacements.add(
            Localizer(
                "Infinity",
                context.getString(R.string.inf)
            )
        )
    }

    fun getNormalizedExpression(expr: String): String {
        var expr = expr
        for (replacement in mReplacements) {
            expr = expr.replace(replacement.local, replacement.english)
        }
        return expr
    }

    fun getLocalizedExpression(expr: String): String {
        var expr = expr
        for (replacement in mReplacements) {
            expr = expr.replace(replacement.english, replacement.local)
        }
        return expr
    }

    private inner class Localizer(var english: String, var local: String)
}
