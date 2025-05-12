/*
 * Copyright (C) 2014 Grantland Chew
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
import android.content.res.Resources
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.widget.Button

/**
 * A Button that resizes it's text to be no larger than the width of the view.
 *
 * @author Grantland Chew <grantlandchew></grantlandchew>@gmail.com>
 */
class AutoFitButtonView : Button {
    // Attributes
    private var mSizeToFit = false
    private var mMaxLines = 0

    /**
     * @return the minimum size (in pixels) of the text size in this AutofitTextView
     */
    var minTextSize: Float = 0f
        private set
    private var mMaxTextSize = 0f
    private var mPrecision = 0f
    private var mPaint: TextPaint? = null

    constructor(context: Context) : super(context) {
        init(context, null, 0)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs, 0)
    }

    constructor(
        context: Context, attrs: AttributeSet?,
        defStyle: Int
    ) : super(context, attrs, defStyle) {
        init(context, attrs, defStyle)
    }

    private fun init(context: Context, attrs: AttributeSet?, defStyle: Int) {
        val scaledDensity = context.getResources().getDisplayMetrics().scaledDensity
        var sizeToFit = true
        var minTextSize: Int = scaledDensity.toInt() * DEFAULT_MIN_TEXT_SIZE
        var precision: Float = PRECISION

        if (attrs != null) {
            val ta = context.obtainStyledAttributes(
                attrs,
                R.styleable.CalculatorEditText,
                defStyle,
                0
            )
            sizeToFit = ta.getBoolean(R.styleable.CalculatorEditText_sizeToFit, sizeToFit)
            minTextSize = ta.getDimensionPixelSize(
                R.styleable.CalculatorEditText_minTextSize,
                minTextSize
            )
            precision = ta.getFloat(R.styleable.CalculatorEditText_precision, precision)
            ta.recycle()
        }

        mPaint = TextPaint()
        this.isSizeToFit = sizeToFit
        setRawTextSize(super.getTextSize())
        setRawMinTextSize(minTextSize.toFloat())
        this.precision = precision
    }

    // Getters and Setters
    var isSizeToFit: Boolean
        /**
         * @return whether or not the text will be automatically resized to fit its constraints.
         */
        get() = mSizeToFit
        /**
         * If true, the text will automatically be resized to fit its constraints; if false, it will
         * act like a normal TextView.
         *
         * @param sizeToFit
         */
        set(sizeToFit) {
            mSizeToFit = sizeToFit
            refitText()
        }

    /**
     * Sets the property of this field (singleLine, to automatically resize the text to fit its constraints.
     */
    fun setSizeToFit() {
        this.isSizeToFit = true
    }

    /**
     * {@inheritDoc}
     */
    override fun getTextSize(): Float {
        return mMaxTextSize
    }

    /**
     * {@inheritDoc}
     */
    override fun setTextSize(unit: Int, size: Float) {
        val context = getContext()
        var r = Resources.getSystem()

        if (context != null) {
            r = context.getResources()
        }

        setRawTextSize(TypedValue.applyDimension(unit, size, r.getDisplayMetrics()))
    }

    private fun setRawTextSize(size: Float) {
        if (size != mMaxTextSize) {
            mMaxTextSize = size
            refitText()
        }
    }

    /**
     * Set the minimum text size to a given unit and value. See TypedValue for the possible
     * dimension units.
     *
     * @param unit The desired dimension unit.
     * @param minSize The desired size in the given units.
     *
     * @attr ref me.grantland.R.styleable#AutofitTextView_minTextSize
     */
    fun setMinTextSize(unit: Int, minSize: Float) {
        val context = getContext()
        var r = Resources.getSystem()

        if (context != null) {
            r = context.getResources()
        }

        setRawMinTextSize(TypedValue.applyDimension(unit, minSize, r.getDisplayMetrics()))
    }

    /**
     * Set the minimum text size to the given value, interpreted as "scaled pixel" units. This size
     * is adjusted based on the current density and user font size preference.
     *
     * @param minSize The scaled pixel size.
     *
     * @attr ref me.grantland.R.styleable#AutofitTextView_minTextSize
     */
    fun setMinTextSize(minSize: Int) {
        setMinTextSize(TypedValue.COMPLEX_UNIT_SP, minSize.toFloat())
    }

    private fun setRawMinTextSize(minSize: Float) {
        if (minSize != this.minTextSize) {
            this.minTextSize = minSize
            refitText()
        }
    }

    var precision: Float
        /**
         * @return the amount of precision used to calculate the correct text size to fit within it's
         * bounds.
         */
        get() = mPrecision
        /**
         * Set the amount of precision used to calculate the correct text size to fit within it's
         * bounds. Lower precision is more precise and takes more time.
         *
         * @param precision The amount of precision.
         */
        set(precision) {
            if (precision != mPrecision) {
                mPrecision = precision
                refitText()
            }
        }

    /**
     * {@inheritDoc}
     */
    override fun setLines(lines: Int) {
        super.setLines(lines)
        mMaxLines = lines
        refitText()
    }

    /**
     * {@inheritDoc}
     */
    override fun getMaxLines(): Int {
        return mMaxLines
    }

    /**
     * {@inheritDoc}
     */
    override fun setMaxLines(maxLines: Int) {
        super.setMaxLines(maxLines)
        if (maxLines != mMaxLines) {
            mMaxLines = maxLines
            refitText()
        }
    }

    /**
     * Re size the font so the specified text fits in the text box assuming the text box is the
     * specified width.
     */
    private fun refitText() {
        if (!mSizeToFit) {
            return
        }

        if (mMaxLines <= 0) {
            // Don't auto-size since there's no limit on lines.
            return
        }

        var text = getText()
        val method = getTransformationMethod()
        if (method != null) {
            text = method.getTransformation(text, this)
        }
        val targetWidth = getWidth() - getPaddingLeft() - getPaddingRight()
        if (targetWidth > 0) {
            val context = getContext()
            var r = Resources.getSystem()
            val displayMetrics: DisplayMetrics?

            var size = mMaxTextSize
            val high = size
            val low = 0f

            if (context != null) {
                r = context.getResources()
            }
            displayMetrics = r.getDisplayMetrics()

            mPaint!!.set(getPaint())
            mPaint!!.setTextSize(size)

            if ((mMaxLines == 1 && mPaint!!.measureText(text, 0, text.length) > targetWidth)
                || Companion.getLineCount(
                    text,
                    mPaint!!,
                    size,
                    targetWidth.toFloat(),
                    displayMetrics
                ) > mMaxLines
            ) {
                size = Companion.getTextSize(
                    text, mPaint!!, targetWidth.toFloat(), mMaxLines, low, high, mPrecision,
                    displayMetrics
                )
            }

            if (size < this.minTextSize) {
                size = this.minTextSize
            }

            super.setTextSize(TypedValue.COMPLEX_UNIT_PX, size)
        }
    }

    override fun onTextChanged(
        text: CharSequence?, start: Int,
        lengthBefore: Int, lengthAfter: Int
    ) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter)
        refitText()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w != oldw) {
            refitText()
        }
    }

    companion object {
        private const val TAG = "AutoFitTextView"
        private const val SPEW = false

        // Minimum size of the text in pixels
        private const val DEFAULT_MIN_TEXT_SIZE = 15 //sp

        // How precise we want to be when reaching the target textWidth size
        private const val PRECISION = 0.5f

        /**
         * Recursive binary search to find the best size for the text
         */
        private fun getTextSize(
            text: CharSequence, paint: TextPaint,
            targetWidth: Float, maxLines: Int,
            low: Float, high: Float, precision: Float,
            displayMetrics: DisplayMetrics?
        ): Float {
            val mid = (low + high) / 2.0f
            var lineCount = 1
            var layout: StaticLayout? = null

            paint.setTextSize(
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_PX, mid,
                    displayMetrics
                )
            )

            if (maxLines != 1) {
                layout = StaticLayout(
                    text, paint, targetWidth.toInt(), Layout.Alignment.ALIGN_NORMAL,
                    1.0f, 0.0f, true
                )
                lineCount = layout.getLineCount()
            }

            if (SPEW) Log.d(
                TAG, "low=" + low + " high=" + high + " mid=" + mid +
                        " target=" + targetWidth + " maxLines=" + maxLines + " lineCount=" + lineCount
            )

            if (lineCount > maxLines) {
                return getTextSize(
                    text, paint, targetWidth, maxLines, low, mid, precision,
                    displayMetrics
                )
            } else if (lineCount < maxLines) {
                return getTextSize(
                    text, paint, targetWidth, maxLines, mid, high, precision,
                    displayMetrics
                )
            } else {
                var maxLineWidth = 0f
                if (maxLines == 1) {
                    maxLineWidth = paint.measureText(text, 0, text.length)
                } else {
                    for (i in 0..<lineCount) {
                        if (layout!!.getLineWidth(i) > maxLineWidth) {
                            maxLineWidth = layout.getLineWidth(i)
                        }
                    }
                }

                if ((high - low) < precision) {
                    return low
                } else if (maxLineWidth > targetWidth) {
                    return getTextSize(
                        text, paint, targetWidth, maxLines, low, mid, precision,
                        displayMetrics
                    )
                } else if (maxLineWidth < targetWidth) {
                    return getTextSize(
                        text, paint, targetWidth, maxLines, mid, high, precision,
                        displayMetrics
                    )
                } else {
                    return mid
                }
            }
        }

        private fun getLineCount(
            text: CharSequence?, paint: TextPaint, size: Float, width: Float,
            displayMetrics: DisplayMetrics?
        ): Int {
            paint.setTextSize(
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_PX, size,
                    displayMetrics
                )
            )
            val layout = StaticLayout(
                text, paint, width.toInt(),
                Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, true
            )
            return layout.getLineCount()
        }
    }
}