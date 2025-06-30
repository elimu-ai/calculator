/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.calculator2

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import com.android.calculator2.util.DigitLabelHelper
import com.android.calculator2.util.DigitLabelHelper.DigitLabelHelperCallback

class CalculatorNumericPadLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : CalculatorPadLayout(context, attrs, defStyle) {
    public override fun onFinishInflate() {
        super.onFinishInflate()

        DigitLabelHelper.instance.getTextForDigits(
            context,
            object : DigitLabelHelperCallback {
                override fun setDigitText(id: Int, text: String?) {
                    val button = findViewById<View?>(id) as Button
                    button.text = text
                }
            })
    }
}

