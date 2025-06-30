/*
 * Copyright (C) 2010 The Android Open Source Project
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

import ai.elimu.calculator.R
import android.content.Context
import android.text.Html
import android.text.InputType
import android.widget.TextView
import com.android.calculator2.view.display.AdvancedDisplay

class MatrixTransposeView : TextView {
    constructor(context: Context?) : super(context)

    constructor(display: AdvancedDisplay) : super(display.context) {
        setInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)
        text = Html.fromHtml("<sup><small>T</small></sup>")
        setTextAppearance(display.context, R.style.Theme_Calculator_Display)
        setPadding(0, 0, 0, 0)
    }

    override fun toString(): String {
        return PATTERN
    }

    companion object {
        const val PATTERN: String = "^T"
    }
}
