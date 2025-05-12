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

import ai.elimu.calculator.R
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import java.util.Objects
import kotlin.math.max

class CalculatorPadViewPager @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ViewPager(context, attrs) {
    private var mBaseManager: NumberBaseManager? = null

    private val mStaticPagerAdapter: PagerAdapter = object : PagerAdapter() {
        override fun getCount(): Int {
            return getChildCount()
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            return getChildAt(position)
        }

        override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
            removeViewAt(position)
        }

        override fun isViewFromObject(view: View, obj: Any): Boolean {
            return view === obj
        }

        override fun getPageWidth(position: Int): Float {
            return if (position == 1) 7.0f / 9.0f else 1.0f
        }
    }

    private val mOnPageChangeListener: OnPageChangeListener =
        object : SimpleOnPageChangeListener() {
            private fun recursivelyEnable(view: View, enabled: Boolean) {
                var enabled = enabled
                if (view is ViewGroup) {
                    val viewGroup = view
                    for (childIndex in 0..<viewGroup.getChildCount()) {
                        recursivelyEnable(viewGroup.getChildAt(childIndex), enabled)
                    }
                } else {
                    if (mBaseManager != null) {
                        enabled = enabled and !mBaseManager!!.isViewDisabled(view.getId())
                    }
                    view.setEnabled(enabled)
                }
            }

            override fun onPageSelected(position: Int) {
                if (getAdapter() === mStaticPagerAdapter) {
                    for (childIndex in 0..<getChildCount()) {
                        recursivelyEnable(getChildAt(childIndex), childIndex == position)
                    }
                }
            }
        }

    private val mPageTransformer: PageTransformer = object : PageTransformer {
        override fun transformPage(view: View, position: Float) {
            if (position < 0.0f) {
                // Pin the left page to the left side.
                view.setTranslationX(getWidth() * -position)
                view.setAlpha(max((1.0f + position).toDouble(), 0.0).toFloat())
            } else {
                // Use the default slide transition when moving to the next page.
                view.setTranslationX(0.0f)
                view.setAlpha(1.0f)
            }
        }
    }

    init {
        setAdapter(mStaticPagerAdapter)
        setBackgroundColor(getResources().getColor(android.R.color.black))
        setOnPageChangeListener(mOnPageChangeListener)
        setPageMargin(getResources().getDimensionPixelSize(R.dimen.pad_page_margin))
        setPageTransformer(false, mPageTransformer)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        // Invalidate the adapter's data set since children may have been added during inflation.
        if (getAdapter() === mStaticPagerAdapter) {
            mStaticPagerAdapter.notifyDataSetChanged()
        }
    }

    fun setBaseManager(baseManager: NumberBaseManager?) {
        mBaseManager = baseManager
    }
}