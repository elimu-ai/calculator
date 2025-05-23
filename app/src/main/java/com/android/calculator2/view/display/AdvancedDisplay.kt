package com.android.calculator2.view.display

import ai.elimu.calculator.R
import android.content.Context
import android.graphics.Paint
import android.text.Editable
import android.text.InputType
import android.text.Spanned
import android.text.TextPaint
import android.text.TextWatcher
import android.text.method.KeyListener
import android.text.method.NumberKeyListener
import android.util.AttributeSet
import android.util.TypedValue
import android.view.ContextMenu
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.android.calculator2.view.CalculatorEditable
import com.android.calculator2.view.ScrollableDisplay
import com.android.calculator2.view.TextUtil.countOccurrences
import com.android.calculator2.view.display.CalculatorEditText.Companion.getInstance
import com.xlythe.math.Constants
import com.xlythe.math.Solver
import com.xlythe.math.Solver.Companion.isOperator
import kotlin.math.max
import kotlin.math.min
import androidx.core.view.isEmpty
import androidx.core.view.size

class AdvancedDisplay(context: Context, attrs: AttributeSet?) : ScrollableDisplay(context, attrs),
    EventListener {
    // For cut, copy, and paste
    internal var mMenuHandler: MenuHandler = MenuHandler(this)

    // Currently focused text box
    var activeEditText: EditText? = null
        private set

    // The LinearLayout inside of this HorizontalScrollView
    private val mRoot: Root = Root(context)

    // Math library
    private var mSolver: Solver? = null

    // A cached copy of getText so we don't calculate it every time its called
    private var mCachedText: String? = null

    // Try and use as large a text as possible, if the width allows it
    private var mWidthConstraint = -1
    private var mHeightConstraint = -1
    private val mTempPaint: Paint = TextPaint()
    private var mOnTextSizeChangeListener: OnTextSizeChangeListener? = null

    // Variables for setting custom views (like Matrices)
    val components: MutableCollection<DisplayComponent> = HashSet<DisplayComponent>()

    // Variables to apply to underlying EditTexts
    private val mRegisteredSyncs: MutableMap<String?, Sync?> = HashMap<String?, Sync?>()
    private var mMaximumTextSize = 0f
    private var mMinimumTextSize = 0f
    private var mStepTextSize = 0f
    private var mTextSize = 0f
    var currentTextColor: Int = 0
        private set
    private var mFactory: Editable.Factory? = null
    private var mKeyListener: KeyListener? = null
    private var mTextIsUpdating = false
    private val mTextWatchers: MutableList<TextWatcher> = ArrayList<TextWatcher>()
    private val mTextWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            if (mTextIsUpdating) return

            val text: CharSequence = text
            for (watcher in mTextWatchers) {
                watcher.beforeTextChanged(text, 0, 0, text.length)
            }
            mCachedText = null
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (mTextIsUpdating) return

            val text: CharSequence = text
            for (watcher in mTextWatchers) {
                watcher.onTextChanged(text, 0, 0, text.length)
            }
        }

        override fun afterTextChanged(s: Editable?) {
            if (mTextIsUpdating) return

            val e = mFactory!!.newEditable(text)
            for (watcher in mTextWatchers) {
                watcher.afterTextChanged(e)
            }
        }
    }

    init {
        val params = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        )
        params.gravity = Gravity.CENTER_VERTICAL
        mRoot.setLayoutParams(params)
        mRoot.gravity = Gravity.RIGHT
        mRoot.isLongClickable = true
        addView(mRoot)

        if (attrs != null) {
            val a = context.obtainStyledAttributes(
                attrs, R.styleable.CalculatorEditText, 0, 0
            )
            mMinimumTextSize = a.getDimension(
                R.styleable.CalculatorEditText_textSize, this.textSize
            )
            mMaximumTextSize = mMinimumTextSize
            mTextSize = mMaximumTextSize
            mMaximumTextSize = a.getDimension(
                R.styleable.CalculatorEditText_maxTextSize, this.textSize
            )
            mMinimumTextSize = a.getDimension(
                R.styleable.CalculatorEditText_minTextSize, this.textSize
            )
            mStepTextSize = a.getDimension(
                R.styleable.CalculatorEditText_stepTextSize,
                (mMaximumTextSize - mMinimumTextSize) / 3
            )
            this.currentTextColor = a.getColor(R.styleable.CalculatorEditText_textColor, 0)
            a.recycle()

            setTextSize(TypedValue.COMPLEX_UNIT_PX, mMaximumTextSize)
            setMinimumHeight((mMaximumTextSize * 1.2).toInt() + paddingBottom + paddingTop)
        }

        val factory: Editable.Factory = CalculatorEditable.Factory()
        setEditableFactory(factory)

        val keywords = listOf<String>(
            context.getString(R.string.arcsin) + "(",
            context.getString(R.string.arccos) + "(",
            context.getString(R.string.arctan) + "(",
            context.getString(R.string.fun_sin) + "(",
            context.getString(R.string.fun_cos) + "(",
            context.getString(R.string.fun_tan) + "(",
            context.getString(R.string.fun_log) + "(",
            context.getString(R.string.mod) + "(",
            context.getString(R.string.fun_ln) + "(",
            context.getString(R.string.det) + "(",
            context.getString(R.string.dx),
            context.getString(R.string.dy),
            context.getString(R.string.cbrt) + "("
        )
        val calculatorKeyListener: NumberKeyListener = object : NumberKeyListener() {
            override fun getInputType(): Int {
                return EditorInfo.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
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
                /*
                 * the EditText should still accept letters (eg. 'sin') coming from the on-screen touch buttons, so don't filter anything.
                 */
                return null
            }

            override fun onKeyDown(
                view: View?,
                content: Editable?,
                keyCode: Int,
                event: KeyEvent?
            ): Boolean {
                if (keyCode == KeyEvent.KEYCODE_DEL) {
                    val selectionHandle: Int = selectionStart
                    if (selectionHandle == 0) {
                        // Remove the view in front
                        val index = getChildIndex(activeEditText)
                        if (index > 0) {
                            removeView(getChildAt(index - 1))
                            return true
                        }
                    } else {
                        // Check and remove keywords
                        val textBeforeInsertionHandle =
                            activeEditText?.getText().toString().substring(0, selectionHandle)
                        val textAfterInsertionHandle = activeEditText?.getText().toString()
                            .substring(
                                selectionHandle,
                                activeEditText?.getText().toString().length
                            )

                        for (s in keywords) {
                            if (textBeforeInsertionHandle.endsWith(s)) {
                                val deletionLength = s.length
                                val text = textBeforeInsertionHandle.substring(
                                    0,
                                    textBeforeInsertionHandle.length - deletionLength
                                ) + textAfterInsertionHandle
                                activeEditText?.setText(text)
                                setSelection(selectionHandle - deletionLength)
                                return true
                            }
                        }
                    }
                }
                return super.onKeyDown(view, content, keyCode, event)
            }
        }
        setKeyListener(calculatorKeyListener)
    }

    fun setEditableFactory(factory: Editable.Factory) {
        mFactory = factory
        registerSync(object : Sync("setEditableFactory") {
            override fun apply(textView: TextView?) {
                textView?.setEditableFactory(mFactory)
            }
        })
    }

    fun setKeyListener(input: KeyListener?) {
        mKeyListener = input
        registerSync(object : Sync("setKeyListener") {
            override fun apply(textView: TextView?) {
                textView?.setKeyListener(mKeyListener)
            }
        })
    }

    fun setTextColor(color: Int) {
        this.currentTextColor = color
        registerSync(object : Sync("setTextColor") {
            override fun apply(textView: TextView?) {
                textView?.setTextColor(currentTextColor)
            }
        })
    }

    fun setTextSize(unit: Int, size: Float) {
        val oldTextSize = mTextSize
        mTextSize = size
        registerSync(object : Sync("setTextSize") {
            override fun apply(textView: TextView?) {
                textView?.setTextSize(unit, mTextSize)
            }
        })
        if (mOnTextSizeChangeListener != null && this.textSize != oldTextSize) {
            mOnTextSizeChangeListener!!.onTextSizeChanged(this, oldTextSize)
        }
    }

    var textSize: Float
        get() = mTextSize
        set(size) {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
        }

    fun setOnTextSizeChangeListener(listener: OnTextSizeChangeListener?) {
        mOnTextSizeChangeListener = listener
    }

    fun getVariableTextSize(text: String): Float {
        if (mWidthConstraint < 0 || mMaximumTextSize <= mMinimumTextSize) {
            // Not measured, bail early.
            return this.textSize
        }

        // Count exponents, which aren't measured properly.
        val exponents = countOccurrences(text, '^')

        // Step through increasing text sizes until the text would no longer fit.
        var lastFitTextSize = mMinimumTextSize
        while (lastFitTextSize < mMaximumTextSize) {
            val nextSize = min(
                (lastFitTextSize + mStepTextSize).toDouble(),
                mMaximumTextSize.toDouble()
            ).toFloat()
            mTempPaint.textSize = nextSize
            if (mTempPaint.measureText(text) > mWidthConstraint) {
                break
            } else if (nextSize + nextSize * exponents / 2 > mHeightConstraint) {
                break
            } else {
                lastFitTextSize = nextSize
            }
        }

        return lastFitTextSize
    }

    fun addTextChangedListener(watcher: TextWatcher?) {
        mTextWatchers.add(watcher!!)
    }

    private fun registerSync(sync: Sync) {
        mRegisteredSyncs.put(sync.tag, sync)
        apply(this, sync)
    }

    private fun apply(view: View?, sync: Sync) {
        if (view is ViewGroup) {
            val vg = view
            for (i in 0..<vg.size) {
                apply(vg.getChildAt(i), sync)
            }
        } else if (view is TextView) {
            sync.apply(view)
        }
    }

    override fun removeView(view: View?) {
        val index = mRoot.getChildIndex(view)
        if (index == -1) return

        // Remove the requested view
        mRoot.removeViewAt(index)

        // Combine the 2 EditTexts on either side
        val leftSide = mRoot.getChildAt(index - 1) as CalculatorEditText
        val rightSide = mRoot.getChildAt(index) as CalculatorEditText
        val cursor = leftSide.getText().length
        leftSide.setText(leftSide.getText().toString() + rightSide.getText().toString())
        mRoot.removeViewAt(index) // Remove the second EditText
        leftSide.requestFocus()
        leftSide.setSelection(cursor)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        mWidthConstraint =
            MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight()
        mHeightConstraint =
            MeasureSpec.getSize(heightMeasureSpec) - paddingTop - paddingBottom
        setTextSize(TypedValue.COMPLEX_UNIT_PX, getVariableTextSize(this.text.toString()))
    }

    private val selectionStart: Int
        get() {
            if (this.activeEditText == null) return 0
            return this.activeEditText!!.selectionStart
        }

    private fun setSelection(position: Int) {
        this.activeEditText!!.setSelection(position)
    }

    /**
     * Clears the text in the display
     */
    fun clear() {
        mCachedText = null

        // Notify the text watcher
        mTextWatcher.beforeTextChanged(null, 0, 0, 0)

        // Clear all views
        mRoot.removeAllViews()

        // Always start with a CalculatorEditText
        this.activeEditText = getInstance(context, mSolver, this)
        addView(this.activeEditText)

        // Notify the text watcher
        mTextWatcher.onTextChanged(null, 0, 0, 0)
        mTextWatcher.afterTextChanged(null)
    }

    override fun onEditTextChanged(editText: EditText?) {
        this.activeEditText = editText
    }

    override fun onRemoveView(view: View?) {
        removeView(view)
    }

    /**
     * Loop around when arrow keys are pressed
     */
    override fun nextView(currentView: View?): View? {
        var foundCurrentView = false
        for (i in 0..<mRoot.size) {
            if (foundCurrentView) return mRoot.getChildAt(i)
            else if (currentView === mRoot.getChildAt(i)) foundCurrentView = true
        }
        return mRoot.getChildAt(0)
    }

    /**
     * Loop around when arrow keys are pressed
     */
    override fun previousView(currentView: View?): View? {
        var foundCurrentView = false
        for (i in mRoot.size - 1 downTo 0) {
            if (foundCurrentView) return mRoot.getChildAt(i)
            else if (currentView === mRoot.getChildAt(i)) foundCurrentView = true
        }
        return mRoot.getChildAt(mRoot.size - 1)
    }

    fun next() {
        if (activeEditText!!.selectionStart == activeEditText!!.getText().length) {
            val v = activeEditText!!.focusSearch(FOCUS_FORWARD)
            v?.requestFocus()
            activeEditText!!.setSelection(0)
        } else {
            activeEditText!!.setSelection(activeEditText!!.selectionStart + 1)
        }
    }

    fun hasNext(): Boolean {
        return hasNext(this)
    }

    private fun hasNext(view: View?): Boolean {
        if (view is AdvancedDisplayControls) {
            return (view as AdvancedDisplayControls).hasNext()
        } else if (view is ViewGroup) {
            val vg = view
            for (i in 0..<vg.size) {
                if (hasNext(vg.getChildAt(i))) {
                    return true
                }
            }
            return false
        }
        return false
    }

    fun backspace() {
        val aet = this.activeEditText
        aet?.dispatchKeyEvent(KeyEvent(0, KeyEvent.KEYCODE_DEL))
    }

    /**
     * Inserts text at the cursor of the active EditText
     */
    fun insert(delta: CharSequence) {
        insert(delta.toString())
    }

    /**
     * Inserts text at the cursor of the active EditText
     */
    fun insert(delta: String) {
        var delta = delta
        if (this.activeEditText == null) {
            setText(delta)
        } else {
            // Notify the text watcher
            mTextWatcher.beforeTextChanged(null, 0, 0, 0)
            mTextIsUpdating = true


            // limit the max number of characters the edit text can have
            if (delta.isNotEmpty() &&
                this.activeEditText!!.length() + delta.length > MAX_TEXT_EDIT_CHARS
            ) {
                val chars = min(
                    delta.length.toDouble(),
                    max(0.0, (MAX_TEXT_EDIT_CHARS - this.activeEditText!!.length()).toDouble())
                ).toInt()
                delta = delta.substring(0, chars)

                if (delta.isEmpty()) {
                    Toast.makeText(context, R.string.text_max_chars, Toast.LENGTH_SHORT)
                        .show()
                }
            }

            if (CalculatorEditText::class.java.isInstance(this.activeEditText)) {
                // Logic to insert, split text if there's another view, etc
                var cursor: Int
                var cacheCursor: Int
                cacheCursor = this.activeEditText!!.selectionStart
                cursor = cacheCursor
                val index = mRoot.getChildIndex(this.activeEditText)
                val cache = StringBuilder()

                // Loop over the text, adding custom views when needed
                loop@ while (!delta.isEmpty()) {
                    for (c in this.components) {
                        val equation = c.parse(delta)
                        if (equation != null) {
                            // Update the EditText with the cached text
                            this.activeEditText!!.getText().insert(cursor, cache)
                            cache.setLength(0)
                            cacheCursor = 0

                            // We found a custom view
                            mRoot.addView(
                                c.getView<View?, Any?>(
                                    context,
                                    mSolver,
                                    equation,
                                    this
                                )
                            )

                            // Keep EditTexts in between custom views
                            splitText(cursor, index, delta)
                            mRoot.getChildAt(index + 2).requestFocus()

                            // Update text and loop again
                            delta = delta.substring(equation.length)
                            continue@loop
                        }
                    }

                    // Don't allow leading operators
                    if (cursor == 0 && this.activeEditText === mRoot.getChildAt(0) && isOperator(
                            delta
                        )
                        && (delta != Constants.MINUS.toString())
                    ) {
                        delta = delta.substring(1)
                        continue@loop
                    }

                    // Append the next character to the EditText
                    cache.append(delta[0])
                    delta = delta.substring(1)
                    cursor++
                }

                // Update the EditText with the cached text
                this.activeEditText!!.getText().insert(cacheCursor, cache)
            } else {
                // We let the custom edit text handle displaying the text
                val cursor = this.activeEditText!!.selectionStart
                this.activeEditText!!.getText().insert(cursor, delta)
            }

            // Notify the text watcher
            mTextIsUpdating = false
            mTextWatcher.onTextChanged(null, 0, 0, 0)
            mTextWatcher.afterTextChanged(null)
        }
    }

    private fun splitText(cursor: Int, index: Int, text: String) {
        // Grab the left and right strings
        val leftText = this.activeEditText!!.getText().toString().substring(0, cursor)
        val rightText = this.activeEditText!!.getText().toString().substring(cursor)

        // Update the left EditText
        this.activeEditText!!.setText(leftText)

        // Create a right EditText
        val et: EditText = getInstance(context, mSolver, this)
        et.setText(rightText)
        addView(et, index + 2)

        // Decide who needs focus
        if (text.isEmpty()) {
            mRoot.getChildAt(index + 1).requestFocus()
        } else {
            mRoot.getChildAt(index + 2).requestFocus()
            (mRoot.getChildAt(index + 2) as CalculatorEditText).setSelection(0)
        }
    }

    fun setSolver(solver: Solver?) {
        mSolver = solver
    }

    override fun addView(child: View?) {
        if (child === mRoot) {
            super.addView(child)
            return
        }
        mRoot.addView(child)
    }

    override fun addView(child: View?, index: Int) {
        if (child === mRoot) {
            super.addView(child, index)
            return
        }
        mRoot.addView(child, index)
    }

    fun getChildIndex(child: View?): Int {
        return mRoot.getChildIndex(child)
    }

    override fun setEnabled(enabled: Boolean) {
        // We only want to disable our children. So we're not calling super on purpose.
        registerSync(object : Sync("setEnabled") {
            override fun apply(textView: TextView?) {
                textView?.setEnabled(enabled)
            }
        })
    }

    val text: String
        /**
         * Returns the text in the display
         */
        get() {
            if (mCachedText != null) {
                return mCachedText!!
            }

            var text = ""
            for (i in 0..<mRoot.size) {
                text += mRoot.getChildAt(i).toString()
            }
            mCachedText = text
            return text
        }

    /**
     * Set the text for the display
     */
    fun setText(resId: Int) {
        setText(context.getString(resId))
    }

    /**
     * Set the text for the display
     */
    fun setText(text: String?) {
        // Notify the text watcher
        var text = text
        mTextWatcher.beforeTextChanged(null, 0, 0, 0)
        mTextIsUpdating = true

        // Remove existing text
        clear()

        // Clear on null
        if (text == null) return

        // Don't allow leading operators
        while (text!!.isNotEmpty() && isOperator(text[0])
            && !text.startsWith(Constants.MINUS.toString())
        ) {
            text = text.substring(1)
        }

        val cache = StringBuilder()

        // Loop over the text, adding custom views when needed
        loop@ while (!text!!.isEmpty()) {
            for (c in this.components) {
                val equation = c.parse(text)
                if (equation != null) {
                    // Apply the cache
                    val trailingText: EditText = (mRoot.lastView as CalculatorEditText)
                    trailingText.setText(cache)
                    cache.setLength(0)

                    // We found a custom view
                    mRoot.addView(c.getView<View?, Any?>(context, mSolver, equation, this))

                    // Keep EditTexts in between custom views
                    addView(getInstance(context, mSolver, this))

                    // Update text and loop again
                    text = text!!.substring(equation.length)
                    continue@loop
                }
            }

            // Append the next character to the trailing EditText
            cache.append(text!![0])
            text = text.substring(1)
        }

        // Apply the cache
        val trailingText: EditText = (mRoot.lastView as CalculatorEditText)
        trailingText.setText(cache)
        trailingText.setSelection(trailingText.length())
        trailingText.requestFocus()

        // Notify the text watcher
        mTextIsUpdating = false
        mTextWatcher.onTextChanged(null, 0, 0, 0)
        mTextWatcher.afterTextChanged(null)
    }

    fun registerComponent(component: DisplayComponent) {
        components.add(component)
    }

    fun registerComponents(components: MutableCollection<DisplayComponent>) {
        components.addAll(components)
    }

    // Everything below is for copy/paste
    interface OnTextSizeChangeListener {
        fun onTextSizeChanged(textView: AdvancedDisplay?, oldSize: Float)
    }

    internal inner class Root @JvmOverloads constructor(
        context: Context?,
        attrs: AttributeSet? = null
    ) : LinearLayout(context, attrs) {
        init {
            orientation = HORIZONTAL
        }

        override fun addView(child: View?, index: Int) {
            super.addView(child, index)

            for (sync in mRegisteredSyncs.entries) {
                // Apply all our custom variables to our lovely children
                apply(child, sync.value!!)
            }

            apply(child, object : Sync("addTextChangedListener") {
                override fun apply(textView: TextView?) {
                    textView?.addTextChangedListener(mTextWatcher)
                }
            })
        }

        val lastView: View?
            get() {
                if (isEmpty()) return null
                return getChildAt(childCount - 1)
            }

        /**
         * Returns the position of a view
         */
        fun getChildIndex(view: View?): Int {
            for (i in 0..<size) {
                if (getChildAt(i) === view) return i
            }
            return -1
        }

        override fun onCreateContextMenu(menu: ContextMenu) {
            mMenuHandler.onCreateContextMenu(menu)
        }
    }

    companion object {
        // Restrict keys from hardware keyboards
        private val ACCEPTED_CHARS = "0123456789.+-*/\u2212\u00d7\u00f7()!%^".toCharArray()

        // The maximum allowed text edit chars - no limit can cause FC
        private const val MAX_TEXT_EDIT_CHARS = 500
    }
}
