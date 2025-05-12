package com.android.calculator2.view.display

import android.widget.TextView

/**
 * Created by Will on 12/13/2014.
 */
abstract class Sync internal constructor(@JvmField var tag: String) {
    abstract fun apply(textView: TextView?)

    override fun hashCode(): Int {
        return tag.hashCode()
    }

    override fun equals(o: Any?): Boolean {
        if (o is Sync) {
            return o.tag == tag
        }
        return false
    }
}

