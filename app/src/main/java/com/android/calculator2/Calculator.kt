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

import ai.elimu.analytics.utils.LearningEventUtil
import ai.elimu.calculator.BuildConfig
import ai.elimu.calculator.R
import ai.elimu.common.utils.data.model.tts.QueueMode
import ai.elimu.common.utils.ui.setStatusBarColorCompat
import ai.elimu.common.utils.viewmodel.TextToSpeechViewModel
import ai.elimu.common.utils.viewmodel.TextToSpeechViewModelImpl
import ai.elimu.model.v2.enums.content.NumeracySkill
import ai.elimu.model.v2.gson.content.NumberGson
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewAnimationUtils
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.android.calculator2.CalculatorExpressionEvaluator.EvaluateCallback
import com.android.calculator2.HistoryAdapter.HistoryItemCallback
import com.android.calculator2.receiver.StudentUpdatedReceiver
import com.android.calculator2.util.NumPadUtils
import com.android.calculator2.util.tagToSpokenText
import com.android.calculator2.view.DisplayOverlay
import com.android.calculator2.view.GraphView
import com.android.calculator2.view.MatrixEditText
import com.android.calculator2.view.MatrixInverseView
import com.android.calculator2.view.MatrixTransposeView
import com.android.calculator2.view.MatrixView
import com.android.calculator2.view.MatrixView.MVDisplayComponent
import com.android.calculator2.view.MultiButton
import com.android.calculator2.view.display.AdvancedDisplay
import com.android.calculator2.view.display.AdvancedDisplay.OnTextSizeChangeListener
import com.xlythe.math.Base
import com.xlythe.math.Constants
import com.xlythe.math.GraphModule
import com.xlythe.math.History
import com.xlythe.math.HistoryEntry
import com.xlythe.math.Persist
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random
import androidx.core.text.isDigitsOnly

@AndroidEntryPoint
class Calculator : AppCompatActivity(), OnTextSizeChangeListener, EvaluateCallback, OnLongClickListener {

    private lateinit var ttsViewModel: TextToSpeechViewModel

    private enum class CalculatorState {
        INPUT, EVALUATE, RESULT, ERROR
    }

    private val mFormulaTextWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(
            charSequence: CharSequence?,
            start: Int,
            count: Int,
            after: Int
        ) {
        }

        override fun onTextChanged(
            charSequence: CharSequence?,
            start: Int,
            count: Int,
            after: Int
        ) {
        }

        override fun afterTextChanged(editable: Editable) {
            setState(CalculatorState.INPUT)
            mEvaluator.evaluate(editable, this@Calculator)

            if (editable.toString().contains(mX)) {
                mEqualsGraphButton?.setEnabled(R.id.graph)
            } else {
                mEqualsGraphButton?.setEnabled(R.id.eq)
            }
        }
    }

    private val mFormulaOnKeyListener: View.OnKeyListener = object : View.OnKeyListener {
        override fun onKey(view: View?, keyCode: Int, keyEvent: KeyEvent): Boolean {
            when (keyCode) {
                KeyEvent.KEYCODE_NUMPAD_ENTER, KeyEvent.KEYCODE_ENTER -> {
                    if (keyEvent.action == KeyEvent.ACTION_UP) {
                        val v = mEqualsGraphButton?.enabledView
                        mCurrentButton = v
                        if (v != null) {
                            when (v.id) {
                                R.id.eq -> onEquals()
                                R.id.graph -> onGraph()
                            }
                        }
                    }
                    // ignore all other actions
                    return true
                }
            }
            return false
        }
    }

    private var mCurrentState: CalculatorState? = null
    private val mTokenizer: CalculatorExpressionTokenizer by lazy {
        CalculatorExpressionTokenizer(this)
    }
    private val mEvaluator: CalculatorExpressionEvaluator by lazy {
        CalculatorExpressionEvaluator(mTokenizer)
    }
    private var mDisplayView: DisplayOverlay? = null
    private var mFormulaEditText: AdvancedDisplay? = null
    private var mResultEditText: AdvancedDisplay? = null
    private var mPadViewPager: CalculatorPadViewPager? = null
    private var mDeleteButton: View? = null
    private var mClearButton: View? = null
    private var mCurrentButton: View? = null
    private var mEqualsGraphButton: MultiButton? = null
    private var mCurrentAnimator: Animator? = null
    private var mHistory: History? = null
    private var mHistoryAdapter: RecyclerView.Adapter<*>? = null
    private var mPersist: Persist? = null
    private var mBaseManager: NumberBaseManager? = null
    private val mX: String by lazy { getString(R.string.X) }
    private var mGraphController: GraphController? = null
    private val mLayoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, 0)

    override fun onCreate(savedInstanceState: Bundle?) {
        var savedInstanceState = savedInstanceState
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calculator)

        ttsViewModel = ViewModelProvider(this)[TextToSpeechViewModelImpl::class.java]

        // Personalize available operators
        val buttonOperatorMul: Button = findViewById<Button>(R.id.op_mul)
        val buttonOperatorSub: Button = findViewById<Button>(R.id.op_sub)
        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val numeracySkillSet = sharedPreferences.getStringSet(
            StudentUpdatedReceiver.PREF_STUDENT_NUMERACY_SKILLS,
            null
        )
        Log.d(javaClass.getName(), "numeracySkillSet: $numeracySkillSet")
        if (numeracySkillSet == null) {
            buttonOperatorMul.visibility = View.INVISIBLE
            buttonOperatorSub.visibility = View.INVISIBLE
        } else {
            if (!numeracySkillSet.contains(NumeracySkill.MULTIPLICATION.toString())) {
                buttonOperatorMul.visibility = View.INVISIBLE
            }
            if (!numeracySkillSet.contains(NumeracySkill.SUBTRACTION.toString())) {
                buttonOperatorSub.visibility = View.INVISIBLE
            }
        }

        mDisplayView =
            findViewById<DisplayOverlay>(R.id.display)
        mFormulaEditText =
            findViewById<AdvancedDisplay>(R.id.formula)
        mResultEditText =
            findViewById<AdvancedDisplay>(R.id.result)
        mPadViewPager = findViewById<CalculatorPadViewPager>(R.id.pad_pager)

        mEqualsGraphButton = findViewById<View>(R.id.pad_numeric).findViewById<MultiButton>(R.id.equals_graph)

        mDeleteButton = findViewById(R.id.del)
        mClearButton = findViewById(R.id.clr)

        if (mEqualsGraphButton == null ||
            mEqualsGraphButton?.visibility != View.VISIBLE
        ) {
            mEqualsGraphButton = findViewById<View>(R.id.pad_operator).findViewById<MultiButton>(R.id.equals_graph)
        }

        savedInstanceState = savedInstanceState ?: Bundle.EMPTY
        setState(
            CalculatorState.entries[savedInstanceState.getInt(
                KEY_CURRENT_STATE,
                CalculatorState.INPUT.ordinal
            )]
        )

        mFormulaEditText!!.setSolver(mEvaluator.solver)
        mResultEditText!!.setSolver(mEvaluator.solver)

        var base = Base.DECIMAL
        val baseOrdinal = savedInstanceState.getInt(KEY_BASE, -1)
        if (baseOrdinal != -1) {
            base = Base.entries[baseOrdinal]
        }
        mBaseManager = NumberBaseManager(base)
        if (mPadViewPager != null) {
            mPadViewPager!!.setBaseManager(mBaseManager)
        }
        setBase(base)

        mFormulaEditText!!.addTextChangedListener(mFormulaTextWatcher)
        mFormulaEditText!!.setOnKeyListener(mFormulaOnKeyListener)
        mFormulaEditText!!.setOnTextSizeChangeListener(this)
        mFormulaEditText!!.setText(
            mTokenizer.getLocalizedExpression(
                savedInstanceState.getString(KEY_CURRENT_EXPRESSION, "")
            )
        )
        if (TextUtils.isEmpty(mFormulaEditText!!.text)) {
            mEqualsGraphButton?.setEnabled(R.id.eq)
        }

        mEvaluator.evaluate(mFormulaEditText!!.text, this)
        mFormulaEditText!!.setTextColor(ContextCompat.getColor(this, R.color.display_formula_text_color))
        mDeleteButton!!.setOnLongClickListener(this)
        mResultEditText!!.setTextColor(ContextCompat.getColor(this, R.color.display_result_text_color))
        mResultEditText!!.isEnabled = false

        mFormulaEditText!!.registerComponent(MVDisplayComponent())
        mResultEditText?.registerComponents(mFormulaEditText!!.components)

        mDisplayView!!.bringToFront()

        // Disable IME for this application
        window.setFlags(
            WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
            WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
        )

        // Rebuild constants. If the user changed their locale, it won't kill the app
        // but it might change a decimal point from . to ,
        Constants.rebuildConstants()
        val dot: Button = findViewById<Button>(R.id.dec_point)
        dot.text = Constants.DECIMAL_POINT.toString()

        val graphView: GraphView =
            findViewById<GraphView>(R.id.graphView)
        val graphModule = GraphModule(mEvaluator.solver)
        mGraphController = GraphController(graphView, graphModule, mDisplayView!!)

        var displayMode: DisplayOverlay.DisplayMode = DisplayOverlay.DisplayMode.FORMULA
        val modeOrdinal = savedInstanceState.getInt(KEY_DISPLAY_MODE, -1)
        if (modeOrdinal != -1) {
            displayMode = DisplayOverlay.DisplayMode.entries[modeOrdinal]
        }
        mDisplayView!!.mode = displayMode
        mDisplayView!!.getViewTreeObserver().addOnGlobalLayoutListener(
            object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (mDisplayView!!.height > 0) {
                        mDisplayView!!.initializeHistoryAndGraphView()
                        if (mDisplayView!!.mode == DisplayOverlay.DisplayMode.GRAPH) {
                            mGraphController!!.startGraph(mFormulaEditText!!.text)
                        }
                    }
                }
            })
    }

    override fun onResume() {
        super.onResume()

        // Load new history
        mPersist = Persist(this)
        mPersist!!.load()
        mHistory = mPersist!!.history

        mHistoryAdapter = HistoryAdapter(
            this, mHistory!!,
            object : HistoryItemCallback {
                override fun onHistoryItemSelected(entry: HistoryEntry?) {
                    //Disabled insert text in the formula
//                mFormulaEditText.insert(entry.getEdited());
//                mDisplayView.collapseHistory();
                }
            })
        mHistory!!.setObserver(mHistoryAdapter)
        mDisplayView!!.historyView.setAdapter(mHistoryAdapter)
        mDisplayView!!.scrollToMostRecent()
    }

    override fun onPause() {
        super.onPause()
        mPersist!!.save()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // If there's an animation in progress, end it immediately to ensure the state is
        // up-to-date before it is serialized.
        if (mCurrentAnimator != null) {
            mCurrentAnimator!!.end()
        }

        super.onSaveInstanceState(outState)
        outState.putInt(KEY_CURRENT_STATE, mCurrentState!!.ordinal)
        outState.putString(
            KEY_CURRENT_EXPRESSION,
            mTokenizer.getNormalizedExpression(mFormulaEditText!!.text)
        )
        outState.putInt(KEY_BASE, mBaseManager!!.numberBase.ordinal)
        outState.putInt(KEY_DISPLAY_MODE, mDisplayView!!.mode.ordinal)
    }

    private fun setClearVisibility(visible: Boolean) {
        mClearButton!!.visibility = if (visible) View.VISIBLE else View.GONE
        mDeleteButton!!.visibility = if (visible) View.GONE else View.VISIBLE
    }

    private fun setState(state: CalculatorState) {
        if (mCurrentState != state) {
            mCurrentState = state
            setClearVisibility(state == CalculatorState.RESULT || state == CalculatorState.ERROR)

            if (state == CalculatorState.ERROR) {
                val errorColor = ContextCompat.getColor(this, R.color.calculator_error_color)
                mFormulaEditText!!.setTextColor(errorColor)
                mResultEditText!!.setTextColor(errorColor)
                window.setStatusBarColorCompat(errorColor)
            } else {
                mFormulaEditText!!.setTextColor(
                    ContextCompat.getColor(this, R.color.display_formula_text_color)
                )
                mResultEditText!!.setTextColor(
                    ContextCompat.getColor(this, R.color.display_result_text_color)
                )
                window.setStatusBarColorCompat(R.color.calculator_accent_color)
            }
        }
    }

    override fun onBackPressed() {
        if (mPadViewPager == null || mPadViewPager!!.currentItem == 0) {
            // If the user is currently looking at the first pad (or the pad is not paged),
            // allow the system to handle the Back button.
            super.onBackPressed()
        } else {
            // Otherwise, select the previous pad.
            mPadViewPager!!.setCurrentItem(mPadViewPager!!.currentItem - 1)
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        // If there's an animation in progress, end it immediately to ensure the state is
        // up-to-date before the pending user interaction is handled.
        if (mCurrentAnimator != null) {
            mCurrentAnimator!!.end()
        }
    }

    fun onButtonClick(view: View) {
        // Play audio for numbers and operators

        if ((view.tag != null) && NumPadUtils.RAW_FILE_EQUALS != view.tag) {
            val spokenText = view.tag.toString().tagToSpokenText()
            ttsViewModel.speak(spokenText, QueueMode.FLUSH, Random.nextInt().toString())

            if (spokenText.isDigitsOnly()) {
                LearningEventUtil.reportNumberLearningEvent(
                    numberGson = NumberGson().apply {
                        value = spokenText.toInt()},
                    context = applicationContext,
                    analyticsApplicationId = BuildConfig.ANALYTICS_APPLICATION_ID)
            }
        }

        mCurrentButton = view
        when (view.id) {
            R.id.eq -> onEquals()
            R.id.graph -> onGraph()
            R.id.del -> onDelete()
            R.id.clr -> onClear()
            R.id.det, R.id.fun_cos, R.id.fun_ln, R.id.fun_log, R.id.fun_sin, R.id.fun_tan ->                 // Add left parenthesis after functions.
                mFormulaEditText!!.insert((view as Button).getText().toString() + "(")

            R.id.hex -> setBase(Base.HEXADECIMAL)
            R.id.bin -> setBase(Base.BINARY)
            R.id.dec -> setBase(Base.DECIMAL)
            R.id.matrix -> mFormulaEditText!!.insert(MatrixView.getPattern())
            R.id.matrix_inverse -> mFormulaEditText!!.insert(MatrixInverseView.PATTERN)
            R.id.matrix_transpose -> mFormulaEditText!!.insert(MatrixTransposeView.PATTERN)
            R.id.plus_row -> if (mFormulaEditText!!.activeEditText is MatrixEditText) {
                (mFormulaEditText!!.activeEditText as MatrixEditText).matrixView.addRow()
            }

            R.id.minus_row -> if (mFormulaEditText!!.activeEditText is MatrixEditText) {
                (mFormulaEditText!!.activeEditText as MatrixEditText).matrixView
                    .removeRow()
            }

            R.id.plus_col -> if (mFormulaEditText!!.activeEditText is MatrixEditText) {
                (mFormulaEditText!!.activeEditText as MatrixEditText).matrixView
                    .addColumn()
            }

            R.id.minus_col -> if (mFormulaEditText!!.activeEditText is MatrixEditText) {
                (mFormulaEditText!!.activeEditText as MatrixEditText).matrixView
                    .removeColumn()
            }

            R.id.const_x -> mFormulaEditText!!.insert((view as Button).getText())
            else -> {
                // Clear the input if we are currently displaying a result, and if the key pressed
                // is not a postfix or infix operator.
                val buttonText = (view as Button).getText()
                val buttonString = buttonText.toString()
                if (mCurrentState == CalculatorState.RESULT &&
                    !(buttonString == getString(R.string.op_div) ||
                            buttonString == getString(R.string.op_mul) ||
                            buttonString == getString(R.string.op_sub) ||
                            buttonString == getString(R.string.op_add) ||
                            buttonString == getString(R.string.op_pow) ||
                            buttonString == getString(R.string.op_fact) ||
                            buttonString == getString(R.string.eq))
                ) {
                    mFormulaEditText!!.clear()
                }
                mFormulaEditText!!.insert(buttonText)
            }
        }
    }

    override fun onLongClick(view: View): Boolean {
        mCurrentButton = view
        if (view.id == R.id.del) {
            onClear()
            return true
        }
        return false
    }

    override fun onEvaluate(expr: String?, result: String?, errorResourceId: Int) {
        if (mCurrentState == CalculatorState.INPUT) {
            if (result == null || result == mFormulaEditText!!.text) {
                mResultEditText!!.clear()
            } else {
                mResultEditText!!.setText("=$result")
            }
        } else if (errorResourceId != INVALID_RES_ID) {
            onError(errorResourceId)
        } else if (!TextUtils.isEmpty(result)) {
            expr?.let {
                mHistory!!.enter(expr, result)
            }
            mDisplayView!!.scrollToMostRecent()
            onResult(result!!)
        } else if (mCurrentState == CalculatorState.EVALUATE) {
            // The current expression cannot be evaluated -> return to the input state.
            setState(CalculatorState.INPUT)
        }
    }

    override fun onTextSizeChanged(textView: AdvancedDisplay?, oldSize: Float) {
        textView ?: return
        if (mCurrentState != CalculatorState.INPUT) {
            // Only animate text changes that occur from user input.
            return
        }

        // Calculate the values needed to perform the scale and translation animations,
        // maintaining the same apparent baseline for the displayed text.
        val textScale = oldSize / textView.textSize
        val translationX: Float =
            (1.0f - textScale) *
                    (textView.width / 2.0f - textView.getPaddingEnd())
        val translationY = (1.0f - textScale) *
                (textView.height / 2.0f - textView.paddingBottom)
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(
            ObjectAnimator.ofFloat<View?>(textView, View.SCALE_X, textScale, 1.0f),
            ObjectAnimator.ofFloat<View?>(textView, View.SCALE_Y, textScale, 1.0f),
            ObjectAnimator.ofFloat<View?>(textView, View.TRANSLATION_X, translationX, 0.0f),
            ObjectAnimator.ofFloat<View?>(textView, View.TRANSLATION_Y, translationY, 0.0f)
        )
        animatorSet.setDuration(
            resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
        )
        animatorSet.interpolator = AccelerateDecelerateInterpolator()
        animatorSet.start()
    }

    private fun onEquals() {
        if (mCurrentState == CalculatorState.INPUT) {
            if (mFormulaEditText!!.hasNext()) {
                mFormulaEditText!!.next()
            } else {
                setState(CalculatorState.EVALUATE)
                mEvaluator.evaluate(mFormulaEditText!!.text, this)
            }
        }
    }

    private fun onGraph() {
        mGraphController!!.startGraph(mFormulaEditText!!.text)
    }

    private fun onDelete() {
        // Delete works like backspace; remove the last character from the expression.
        mFormulaEditText!!.backspace()
    }

    private fun reveal(sourceView: View, colorRes: Int, listener: Animator.AnimatorListener?) {
        // Make reveal cover the display and status bar.
        val revealView = View(this)
        mLayoutParams.height = mDisplayView!!.displayHeight
        mLayoutParams.gravity = Gravity.BOTTOM
        revealView.setLayoutParams(mLayoutParams)
        revealView.setBackgroundColor(ContextCompat.getColor(this, colorRes))
        mDisplayView!!.addView(revealView)

        val revealAnimator: Animator
        val clearLocation = IntArray(2)
        sourceView.getLocationInWindow(clearLocation)
        clearLocation[0] += sourceView.width / 2
        clearLocation[1] += sourceView.height / 2
        val revealCenterX = clearLocation[0] - revealView.left
        val revealCenterY = clearLocation[1] - revealView.top
        val x1_2 = (revealView.left - revealCenterX).toDouble().pow(2.0)
        val x2_2 = (revealView.right - revealCenterX).toDouble().pow(2.0)
        val y_2 = (revealView.top - revealCenterY).toDouble().pow(2.0)
        val revealRadius = max(sqrt(x1_2 + y_2), sqrt(x2_2 + y_2)).toFloat()

        revealAnimator =
            ViewAnimationUtils.createCircularReveal(
                revealView,
                revealCenterX, revealCenterY, 0.0f, revealRadius
            )
        revealAnimator.duration = resources.getInteger(android.R.integer.config_longAnimTime).toLong()

        val alphaAnimator: Animator = ObjectAnimator.ofFloat<View?>(revealView, View.ALPHA, 0.0f)
        alphaAnimator.duration = resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
        alphaAnimator.addListener(listener)

        val animatorSet = AnimatorSet()
        animatorSet.play(revealAnimator).before(alphaAnimator)
        animatorSet.interpolator = AccelerateDecelerateInterpolator()
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                mDisplayView!!.removeView(revealView)
                mCurrentAnimator = null
            }
        })
        mCurrentAnimator = animatorSet
        animatorSet.start()
    }

    private fun onClear() {
        if (TextUtils.isEmpty(mFormulaEditText!!.text)) {
            return
        }
        val sourceView = (if (mClearButton?.visibility == View.VISIBLE)
            mClearButton
        else
            mDeleteButton)!!
        reveal(sourceView, R.color.calculator_accent_color, object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                mFormulaEditText!!.clear()
            }
        })
    }

    private fun onError(errorResourceId: Int) {
        if (mCurrentState != CalculatorState.EVALUATE) {
            // Only animate error on evaluate.
            mResultEditText!!.setText(errorResourceId)
            return
        }

        reveal(
            mCurrentButton!!,
            R.color.calculator_error_color,
            object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    setState(CalculatorState.ERROR)
                    mResultEditText!!.setText(errorResourceId)
                }
            })
    }

    private fun onResult(result: String) {
        // Play audio for result

        if (result.isDigitsOnly()) {
            ttsViewModel.speak(getString(R.string.desc_eq) + result, QueueMode.FLUSH,
                Random.nextInt().toString())
        } else {
            ttsViewModel.speak(getString(R.string.desc_eq), QueueMode.FLUSH, Random.nextInt().toString())
        }

        // Make the clear button appear immediately.
        setClearVisibility(true)

        // Calculate the values needed to perform the scale and translation animations,
        // accounting for how the scale will affect the final position of the text.
        val resultScale =
            mFormulaEditText!!.getVariableTextSize(result) / mResultEditText!!.textSize
        val resultTranslationX: Float =
            (1.0f - resultScale) *
                    (mResultEditText!!.width / 2.0f - mResultEditText!!.getPaddingEnd())
        val resultTranslationY = (1.0f - resultScale) *
                (mResultEditText!!.height / 2.0f - mResultEditText!!.paddingBottom) +
                (mFormulaEditText!!.bottom - mResultEditText!!.bottom) +
                (mResultEditText!!.paddingBottom - mFormulaEditText!!.paddingBottom)
        val formulaTranslationY = -mFormulaEditText!!.bottom.toFloat()

        // Use a value animator to fade to the final text color over the course of the animation.
        val resultTextColor = mResultEditText!!.currentTextColor
        val formulaTextColor = mFormulaEditText!!.currentTextColor
        val textColorAnimator =
            ValueAnimator.ofObject(ArgbEvaluator(), resultTextColor, formulaTextColor)
        textColorAnimator.addUpdateListener(object : AnimatorUpdateListener {
            override fun onAnimationUpdate(valueAnimator: ValueAnimator) {
                mResultEditText!!.setTextColor((valueAnimator.getAnimatedValue() as Int?)!!)
            }
        })

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(
            textColorAnimator,
            ObjectAnimator.ofFloat<View?>(mResultEditText, View.SCALE_X, resultScale),
            ObjectAnimator.ofFloat<View?>(mResultEditText, View.SCALE_Y, resultScale),
            ObjectAnimator.ofFloat<View?>(mResultEditText, View.TRANSLATION_X, resultTranslationX),
            ObjectAnimator.ofFloat<View?>(mResultEditText, View.TRANSLATION_Y, resultTranslationY),
            ObjectAnimator.ofFloat<View?>(mFormulaEditText, View.TRANSLATION_Y, formulaTranslationY)
        )
        animatorSet.setDuration(
            resources.getInteger(android.R.integer.config_longAnimTime).toLong()
        )
        animatorSet.interpolator = AccelerateDecelerateInterpolator()
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {}

            override fun onAnimationEnd(animation: Animator) {
                // Reset all of the values modified during the animation.
                mResultEditText!!.setTextColor(resultTextColor)
                mResultEditText!!.scaleX = 1.0f
                mResultEditText!!.scaleY = 1.0f
                mResultEditText!!.translationX = 0.0f
                mResultEditText!!.translationY = 0.0f
                mFormulaEditText!!.translationY = 0.0f

                // Finally update the formula to use the current result.
                mFormulaEditText!!.setText(result)
                setState(CalculatorState.RESULT)
                mCurrentAnimator = null
            }
        })

        mCurrentAnimator = animatorSet
        animatorSet.start()
    }

    private fun setBase(base: Base) {
        val baseChanged = base != mBaseManager!!.numberBase

        // Update the BaseManager, which handles restricting which buttons to show
        mBaseManager!!.numberBase = base

        // Update the evaluator, which handles the math
        mEvaluator.setBase(mFormulaEditText!!.text, base, object : EvaluateCallback {
            override fun onEvaluate(expr: String?, result: String?, errorResourceId: Int) {
                if (errorResourceId != INVALID_RES_ID) {
                    onError(errorResourceId)
                } else {
                    mResultEditText!!.setText(result)
                    if (!TextUtils.isEmpty(result)) {
                        onResult(result!!)
                    }
                }
            }
        })
        setSelectedBaseButton(base)

        // disable any buttons that are not relevant to the current base
        for (resId in mBaseManager!!.viewIds) {
            // TODO: handle duplicates
            // This will not work if the same resId is used on multiple pages,
            // which will be the case after adding the matrix view.
            val view = findViewById<View?>(resId)
            view?.setEnabled(!mBaseManager!!.isViewDisabled(resId))
        }

        // TODO: preserve history
        // Ideally each history entry is tagged with the base that it was created with.
        // Then when we import a history item into the current display, we can convert the
        // base as necessary. As a short term approach, just clear the history when
        // changing the base.
        if (baseChanged && mHistory != null) {
            mHistory!!.clear()
        }
    }

    private fun setSelectedBaseButton(base: Base) {
        findViewById<View?>(R.id.hex).isSelected = base == Base.HEXADECIMAL
        findViewById<View?>(R.id.bin).isSelected = base == Base.BINARY
        findViewById<View?>(R.id.dec).isSelected = base == Base.DECIMAL
    }

    companion object {
        private val NAME: String = Calculator::class.java.getName()
        const val TAG: String = "Calculator"

        // instance state keys
        private val KEY_CURRENT_STATE: String = NAME + "_currentState"
        private val KEY_CURRENT_EXPRESSION: String = NAME + "_currentExpression"
        private val KEY_BASE: String = NAME + "_base"
        private val KEY_DISPLAY_MODE: String = NAME + "_displayMode"

        /**
         * Constant for an invalid resource id.
         */
        const val INVALID_RES_ID: Int = -1
    }
}
