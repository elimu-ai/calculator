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
import android.text.Editable
import android.text.InputType
import android.text.Spanned
import android.text.method.NumberKeyListener
import android.view.ActionMode
import android.view.Gravity
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import com.android.calculator2.view.display.EventListener
import com.xlythe.math.Constants

class MatrixEditText(matrixView: MatrixView, listener: EventListener) :
    EditText(matrixView.context), OnFocusChangeListener {
    val matrixView: MatrixView
    private val mListener: EventListener

    init {
        setCustomSelectionActionModeCallback(NoTextSelectionMode())
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
        val padding =
            context.resources.getDimensionPixelSize(R.dimen.matrix_edit_text_padding)
        setPadding(padding, 0, padding, 0)
        this.matrixView = matrixView
        setKeyListener(MatrixKeyListener())
        onFocusChangeListener = this
        setGravity(Gravity.CENTER)
        mListener = listener
    }

    override fun onFocusChange(v: View?, hasFocus: Boolean) {
        if (hasFocus) {
            mListener.onEditTextChanged(this)
            if (getText().toString() == Constants.NAN) {
                setText("")
            }
        }
    }

    override fun toString(): String {
        return getText().toString()
    }

    override fun focusSearch(direction: Int): View? {
        when (direction) {
            FOCUS_FORWARD -> return matrixView.nextView(this)
            FOCUS_BACKWARD -> return matrixView.previousView(this)
        }
        return super.focusSearch(direction)
    }

    internal inner class MatrixKeyListener : NumberKeyListener() {
        override fun getInputType(): Int {
            return EditorInfo.TYPE_CLASS_NUMBER or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        }

        override fun getAcceptedChars(): CharArray {
            return ACCEPTED_CHARS
        }

        override fun filter(
            source: CharSequence?,
            start: Int,
            end: Int,
            dest: Spanned?,
            dstart: Int,
            dend: Int
        ): CharSequence? {
            return null
        }

        override fun onKeyDown(
            view: View?,
            content: Editable?,
            keyCode: Int,
            event: KeyEvent?
        ): Boolean {
            if (keyCode == KeyEvent.KEYCODE_DEL) {
                if (matrixView.isEmpty()) mListener.onRemoveView(matrixView)
            }
            return super.onKeyDown(view, content, keyCode, event)
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

    override fun setTextSize(size: Float) {
        super.setTextSize(0.4f * size)
    }

    override fun setTextSize(unit: Int, size: Float) {
        super.setTextSize(unit, 0.4f * size)
    }

    companion object {
        private val ACCEPTED_CHARS = "0123456789,.-\u2212".toCharArray()
    }
}
