package com.android.calculator2.view.display

import android.view.View
import android.widget.EditText

/**
 * Created by Will on 12/13/2014.
 */
interface EventListener {
    fun onEditTextChanged(editText: EditText?)

    fun onRemoveView(view: View?)

    fun nextView(currentView: View?): View?

    fun previousView(currentView: View?): View?
}
