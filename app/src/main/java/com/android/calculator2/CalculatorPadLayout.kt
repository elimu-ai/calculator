/*
* Copyright (C) 2014 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.android.calculator2

import android.R
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.core.view.size
import androidx.core.view.isGone

/**
 * A layout that places children in an evenly distributed grid based on the specified
 * [R.attr.columnCount] and [R.attr.rowCount] attributes.
 */
open class CalculatorPadLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ViewGroup(context, attrs, defStyle) {
    private val mRowCount: Int
    private val mColumnCount: Int

    init {
        val a = context.obtainStyledAttributes(
            attrs,
            intArrayOf(R.attr.rowCount, R.attr.columnCount), defStyle, 0
        )
        mRowCount = a.getInt(0, 1)
        mColumnCount = a.getInt(1, 1)
        a.recycle()
    }

    override fun shouldDelayChildPressedState(): Boolean {
        return false
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val paddingLeft = getPaddingLeft()
        val paddingRight = getPaddingRight()
        val paddingTop = getPaddingTop()
        val paddingBottom = getPaddingBottom()
        val isRTL: Boolean
        if (Build.VERSION.SDK_INT >= 17) {
            isRTL = getLayoutDirection() == LAYOUT_DIRECTION_RTL
        } else {
            isRTL = false
        }
        val columnWidth =
            Math.round((right - left - paddingLeft - paddingRight).toFloat()) / mColumnCount
        val rowHeight =
            Math.round((bottom - top - paddingTop - paddingBottom).toFloat()) / mRowCount
        var rowIndex = 0
        var columnIndex = 0
        for (childIndex in 0..<size) {
            val childView = getChildAt(childIndex)
            if (childView.isGone) {
                continue
            }
            val lp = childView.layoutParams as MarginLayoutParams
            val childTop = paddingTop + lp.topMargin + rowIndex * rowHeight
            val childBottom = childTop - lp.topMargin - lp.bottomMargin + rowHeight
            val childLeft =
                paddingLeft + lp.leftMargin + (if (isRTL) (mColumnCount - 1) - columnIndex else columnIndex) * columnWidth
            val childRight = childLeft - lp.leftMargin - lp.rightMargin + columnWidth
            val childWidth = childRight - childLeft
            val childHeight = childBottom - childTop
            if (childWidth != childView.measuredWidth ||
                childHeight != childView.measuredHeight
            ) {
                childView.measure(
                    MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY)
                )
            }
            childView.layout(childLeft, childTop, childRight, childBottom)
            rowIndex = (rowIndex + (columnIndex + 1) / mColumnCount) % mRowCount
            columnIndex = (columnIndex + 1) % mColumnCount
        }
    }

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams {
        return MarginLayoutParams(context, attrs)
    }

    override fun generateDefaultLayoutParams(): LayoutParams {
        return MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
    }

    override fun generateLayoutParams(p: LayoutParams?): LayoutParams {
        return MarginLayoutParams(p)
    }

    override fun checkLayoutParams(p: LayoutParams?): Boolean {
        return p is MarginLayoutParams
    }
}

