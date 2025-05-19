package com.android.calculator2.view

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import com.android.calculator2.Calculator
import com.android.calculator2.util.AnimationUtil

/**
 * A collection of buttons that occupy the same space, only one of which is visible at a time
 */
class MultiButton : FrameLayout {
    private var mActiveViewId = NO_ID

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    override fun onFinishInflate() {
        super.onFinishInflate()

        for (i in 0..< childCount) {
            getChildAt(i).visibility = INVISIBLE
        }
    }

    /**
     * Disable the currently active button and enable the one with the given resId
     *
     * @param resId
     */
    fun setEnabled(resId: Int) {
        if (mActiveViewId == resId) {
            return
        }

        val newView = findViewById<View?>(resId)
        if (newView == null) {
            Log.w(TAG, "Cannot enable MultiButton view by resId $resId")
            return
        }

        if (mActiveViewId != NO_ID) {
            val oldView = findViewById<View?>(mActiveViewId)
            AnimationUtil.shrinkAndGrow(oldView, newView)
        } else {
            newView.visibility = VISIBLE
        }

        mActiveViewId = resId
    }

    val enabledView: View?
        /**
         * Gets currently enabled view
         *
         * @return enabled view or null if none
         */
        get() = findViewById<View?>(mActiveViewId)

    companion object {
        private val TAG = Calculator.TAG
    }
}
