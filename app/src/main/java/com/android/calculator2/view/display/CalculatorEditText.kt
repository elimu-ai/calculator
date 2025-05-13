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
package com.android.calculator2.view.display

import ai.elimu.calculator.R
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.os.Handler
import android.os.SystemClock
import android.text.Editable
import android.text.Html
import android.text.Spanned
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.Log
import android.view.ActionMode
import android.view.Gravity
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import com.android.calculator2.view.MatrixView
import com.android.calculator2.view.TextUtil.countOccurrences
import com.xlythe.math.BaseModule
import com.xlythe.math.Constants
import com.xlythe.math.EquationFormatter
import com.xlythe.math.Solver
import kotlin.math.min
import androidx.core.view.size

class CalculatorEditText : EditText {
    private val mShowCursor = SystemClock.uptimeMillis()
    private val mHighlightPaint = Paint()
    private val mHandler = Handler()
    private val mRefresher: Runnable = object : Runnable {
        override fun run() {
            invalidate()
        }
    }
    private var mEquationFormatter: EquationFormatter? = null
    private var mInput = ""
    private var mSelectionHandle = 0
    private var mSolver: Solver? = null
    private var mEventListener: EventListener? = null

    constructor(context: Context?) : super(context) {
        setUp()
    }

    constructor(context: Context?, attr: AttributeSet?) : super(context, attr) {
        setUp()
    }

    private fun setUp() {
        isLongClickable = false

        // Disable highlighting text
        setCustomSelectionActionModeCallback(NoTextSelectionMode())

        // Display ^ , and other visual cues
        mEquationFormatter = EquationFormatter()
        addTextChangedListener(object : TextWatcher {
            var updating: Boolean = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                if (updating) return
                updating = true

                mInput = s.toString()
                    .replace(Constants.PLACEHOLDER, Constants.POWER)
                    .replace(mSolver!!.baseModule.separator.toString() + "", "")

                // Get the selection handle, since we're setting text and that'll overwrite it
                mSelectionHandle = selectionStart

                // Adjust the handle by removing any comas or spacing to the left
                val cs = s.subSequence(0, mSelectionHandle).toString()
                mSelectionHandle -= countOccurrences(cs, mSolver!!.baseModule.separator)

                // Update the text with formatted (comas, etc) text
                setText(formatText(mInput))
                setSelection(min(mSelectionHandle.toDouble(), getText().length.toDouble()).toInt())

                updating = false
            }
        })

        setOnKeyListener(object : OnKeyListener {
            override fun onKey(view: View?, i: Int, keyEvent: KeyEvent): Boolean {
                if (i == KeyEvent.KEYCODE_DEL) {
                    val sel = selectionStart
                    val edit = editableText.toString()

                    // If we're trying to delete a separator shift the selector over
                    if (sel >= 1
                        && edit.get(sel - 1) == mSolver!!.baseModule.separator
                    ) {
                        setSelection(sel - 1)
                    }
                }

                return false
            }
        })

        onFocusChangeListener = object : OnFocusChangeListener {
            override fun onFocusChange(v: View?, hasFocus: Boolean) {
                if (hasFocus) mEventListener!!.onEditTextChanged(this@CalculatorEditText)
            }
        }
    }

    private fun formatText(input: String): Spanned? {
        var input = input
        if (mSolver != null) {
            // Add grouping, and then split on the selection handle
            // which is saved as a unique char
            val grouped = mEquationFormatter!!.addComas(mSolver!!, input, mSelectionHandle)
            if (grouped.contains(BaseModule.SELECTION_HANDLE.toString())) {
                val temp = grouped.split(BaseModule.SELECTION_HANDLE.toString().toRegex())
                    .dropLastWhile { it.isEmpty() }.toTypedArray()
                mSelectionHandle = temp[0].length
                input = ""
                for (s in temp) {
                    input += s
                }
            } else {
                input = grouped
                mSelectionHandle = input.length
            }
        }

        return Html.fromHtml(mEquationFormatter!!.insertSupScripts(input))
    }

    override fun toString(): String {
        return mInput
    }

    override fun focusSearch(direction: Int): View? {
        val p = parent
        if (p is AdvancedDisplay) {
            val parent = p
            var v: View?
            when (direction) {
                FOCUS_FORWARD -> {
                    v = parent.nextView(this)
                    while (!v!!.isFocusable) v = parent.nextView(v)
                    return v
                }

                FOCUS_BACKWARD -> {
                    v = parent.previousView(this)
                    while (!v!!.isFocusable) v = parent.previousView(v)
                    if (MatrixView::class.java.isAssignableFrom(v.javaClass)) {
                        v = (v as ViewGroup).getChildAt(v.size - 1)
                        v = (v as ViewGroup).getChildAt(v.size - 1)
                    }
                    return v
                }
            }
        } else {
            Log.d(TAG, "parent isn't an AdvancedDisplay")
        }
        return super.focusSearch(direction)
    }

    public override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // TextViews don't draw the cursor if textLength is 0. Because we're an
        // array of TextViews, we'd prefer that it did.
        if (getText().length == 0 && isEnabled && (isFocused || isPressed)) {
            if ((SystemClock.uptimeMillis() - mShowCursor) % (2 * BLINK) < BLINK) {
                mHighlightPaint.setColor(currentTextColor)
                mHighlightPaint.style = Paint.Style.STROKE
                if (Build.VERSION.SDK_INT >= 21) {
                    mHighlightPaint.strokeWidth = 6f
                }
                canvas.drawLine(
                    (width / 2).toFloat(),
                    0f,
                    (width / 2).toFloat(),
                    height.toFloat(),
                    mHighlightPaint
                )
                mHandler.postAtTime(mRefresher, SystemClock.uptimeMillis() + BLINK)
            }
        }
    }

    internal inner class NoTextSelectionMode : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            // Prevents the selection action mode on double tap.
            return false
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            return false
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
        }
    }

    companion object {
        private const val BLINK = 500
        private const val TAG = "CalculatorEditText"
        @JvmStatic
        fun getInstance(
            context: Context?,
            solver: Solver?,
            eventListener: EventListener
        ): CalculatorEditText {
            val text = inflate(context, R.layout.view_edittext, null) as CalculatorEditText
            text.mSolver = solver
            text.mEventListener = eventListener
            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.gravity = Gravity.CENTER_VERTICAL
            text.setLayoutParams(params)
            return text
        }
    }
}
