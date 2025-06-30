package com.android.calculator2.view

import ai.elimu.calculator.R
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import com.android.calculator2.viewpager.PagerAdapter
import com.android.calculator2.viewpager.VerticalViewPager
import androidx.core.view.size

/**
 * Container for a vertical view pager that adds indicator dots to show the currently
 * selected page.
 */
class AuxPadView : FrameLayout {
    private var mIndicatorView: ViewGroup? = null

    internal class AuxPagerAdapter(private val mViewPager: VerticalViewPager) : PagerAdapter() {
        override val count: Int
            get() = mViewPager.size

        override fun instantiateItem(container: ViewGroup?, position: Int): Any? {
            return mViewPager.getChildAt(position)
        }

        override fun destroyItem(container: ViewGroup?, position: Int, `object`: Any?) {
            mViewPager.removeViewAt(position)
        }

        override fun isViewFromObject(view: View?, `object`: Any?): Boolean {
            return view === `object`
        }
    }

    internal inner class PageChangeListener : VerticalViewPager.OnPageChangeListener {
        private var mSelectedIndex = 0

        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int,
        ) {
        }

        override fun onPageSelected(position: Int) {
            mIndicatorView!!.getChildAt(mSelectedIndex).isSelected = false
            mIndicatorView!!.getChildAt(position).isSelected = true
            mSelectedIndex = position
        }

        override fun onPageScrollStateChanged(state: Int) {
        }
    }

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
        defStyleRes: Int,
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    override fun onFinishInflate() {
        super.onFinishInflate()

        val viewPager: VerticalViewPager = findViewById<VerticalViewPager>(R.id.viewPager)
        viewPager.setAdapter(AuxPagerAdapter(viewPager))
        viewPager.setOnPageChangeListener(PageChangeListener())

        val margin = context.resources
            .getDimensionPixelSize(R.dimen.viewpager_indicator_margin_size)

        mIndicatorView = findViewById<ViewGroup>(R.id.pageIndicators)
        for (i in 0..<viewPager.size) {
            val imageView = ImageView(context)
            imageView.setImageResource(R.drawable.view_pager_indicator)
            imageView.setSelected(i == 0)
            val lp =
                LinearLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT
                )
            lp.setMargins(margin, margin, margin, margin)
            imageView.setLayoutParams(lp)
            mIndicatorView!!.addView(imageView)
        }
    }
}
