package com.android.calculator2.view

import ai.elimu.calculator.R
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.HorizontalScrollView
import kotlin.math.max
import kotlin.math.min
import androidx.core.view.isNotEmpty

open class ScrollableDisplay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyle: Int = 0
) : HorizontalScrollView(context, attrs, defStyle) {
    private var mMaxHeight = 0
    private var gravityRight = false
    private var autoScrolling = false

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.ScrollableDisplay, defStyle, 0)
        setMaxHeight(a.getDimensionPixelSize(R.styleable.ScrollableDisplay_max_height, -1))
        a.recycle()
    }

    fun setMaxHeight(maxHeight: Int) {
        mMaxHeight = maxHeight
    }

    val view: View
        get() = getChildAt(0)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        if (mMaxHeight != -1) {
            val width = measuredWidth
            val height = min(measuredHeight.toDouble(), mMaxHeight.toDouble()).toInt()

            setMeasuredDimension(width, height)
        }
    }

    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        autoScrolling = false
        super.onTouchEvent(ev)
        return false
    }

    override fun computeScroll() {
        if (autoScrolling) return
        super.computeScroll()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        // HorizontalScrollView is broken for Gravity.RIGHT. So we're fixing it.
        autoScrolling = false
        val childWidth = this.view.width
        super.onLayout(changed, left, top, right, bottom)
        val delta = this.view.width - childWidth
        val view = this.view
        val p = view.layoutParams as LayoutParams
        val verticalGravity = p.gravity and Gravity.VERTICAL_GRAVITY_MASK
        if (this.scrollRange > 0) {
            gravityRight = true
            p.gravity = Gravity.LEFT or verticalGravity
            view.setLayoutParams(p)
            super.onLayout(changed, left, top, right, bottom)
        }
        if (gravityRight) {
            if (this.scrollRange == 0) {
                gravityRight = false
                p.gravity = Gravity.RIGHT or verticalGravity
                view.setLayoutParams(p)
                super.onLayout(changed, left, top, right, bottom)
            }
        }
        if (gravityRight && delta > 0) {
            scrollBy(delta, 0)
            autoScrolling = true
        }
    }

    private val scrollRange: Int
        get() {
            var scrollRange = 0
            if (isNotEmpty()) {
                val child = getChildAt(0)
                scrollRange = max(
                    0.0,
                    (child.getWidth() - (getWidth() - getPaddingLeft() - getPaddingRight())).toDouble()
                ).toInt()
            }
            return scrollRange
        }

    override fun scrollTo(x: Int, y: Int) {
        if (autoScrolling) return
        super.scrollTo(x, y)
    }
}
