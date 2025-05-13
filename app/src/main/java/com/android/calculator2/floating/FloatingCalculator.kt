package com.android.calculator2.floating

import ai.elimu.calculator.R
import android.content.ClipData
import android.content.ClipboardManager
import android.text.TextUtils
import android.view.MotionEvent
import android.view.View
import android.view.View.OnLongClickListener
import android.view.View.OnTouchListener
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.viewpager.widget.ViewPager
import com.android.calculator2.Calculator
import com.android.calculator2.CalculatorExpressionEvaluator
import com.android.calculator2.CalculatorExpressionEvaluator.EvaluateCallback
import com.android.calculator2.CalculatorExpressionTokenizer
import com.android.calculator2.util.DigitLabelHelper
import com.android.calculator2.util.PlayerUtil
import com.android.calculator2.view.display.AdvancedDisplay
import com.xlythe.floatingview.FloatingView
import com.xlythe.math.History
import com.xlythe.math.Persist

class FloatingCalculator : FloatingView() {
    // Calc logic
    private var mListener: View.OnClickListener? = null
    private var mDisplay: AdvancedDisplay? = null
    private var mDelete: ImageButton? = null
    private var mClear: ImageButton? = null
    private var mPager: ViewPager? = null
    private var mPersist: Persist? = null
    private var mHistory: History? = null
    private var mTokenizer: CalculatorExpressionTokenizer? = null
    private var mEvaluator: CalculatorExpressionEvaluator? = null
    private var mState: State? = null

    private enum class State {
        DELETE, CLEAR, ERROR
    }

    public override fun inflateButton(): View {
        return View.inflate(context, R.layout.floating_calculator_icon, null)
    }

    public override fun inflateView(): View {
        val child = View.inflate(context, R.layout.floating_calculator, null)

        mTokenizer = CalculatorExpressionTokenizer(this)
        mEvaluator = CalculatorExpressionEvaluator(mTokenizer!!)

        mPager = child.findViewById<View?>(R.id.panelswitch) as ViewPager

        mPersist = Persist(this)
        mPersist!!.load()

        mHistory = mPersist!!.history

        mDisplay = child.findViewById<View?>(R.id.display) as AdvancedDisplay
        mDisplay!!.setSolver(mEvaluator!!.solver)
        mDisplay!!.setOnLongClickListener(object : OnLongClickListener {
            override fun onLongClick(v: View?): Boolean {
                copyContent(mDisplay!!.text)
                return true
            }
        })

        mDelete = child.findViewById<View?>(R.id.delete) as ImageButton
        mClear = child.findViewById<View?>(R.id.clear) as ImageButton
        mListener = object : View.OnClickListener {
            override fun onClick(v: View) {
                if (v.tag != null && PlayerUtil.RAW_FILE_EQUALS != v.tag) {
                    PlayerUtil.playRawFile(context, v.tag.toString())
                }

                if (v is Button) {
                    if (v.getText().toString() == "=") {
                        mEvaluator!!.evaluate(mDisplay!!.text, object : EvaluateCallback {
                            override fun onEvaluate(
                                expr: String?,
                                result: String?,
                                errorResourceId: Int
                            ) {
                                if (errorResourceId != Calculator.INVALID_RES_ID) {
                                    onError(errorResourceId)
                                } else {
                                    // Play audio for result
                                    if (result != null && TextUtils.isDigitsOnly(result) && result.toInt() < 10) {
                                        val view = child.findViewById<View>(
                                            DigitLabelHelper.getIdForDigit(result.toInt())
                                        )
                                        PlayerUtil.playResult(
                                            context,
                                            view.tag.toString()
                                        )
                                    } else {
                                        PlayerUtil.playRawFile(
                                            context,
                                            PlayerUtil.RAW_FILE_EQUALS
                                        )
                                    }
                                    setText(result)
                                }
                            }
                        })
                    } else if (v.id == R.id.parentheses) {
                        setText("(" + mDisplay!!.text + ")")
                    } else if (v.getText().toString().length >= 2) {
                        onInsert(v.getText().toString() + "(")
                    } else {
                        onInsert(v.getText().toString())
                    }
                } else if (v is ImageButton) {
                    onDelete()
                }
            }
        }
        mDelete!!.setOnClickListener(mListener)
        mDelete!!.setOnLongClickListener(object : OnLongClickListener {
            override fun onLongClick(v: View?): Boolean {
                onClear()
                return true
            }
        })
        mClear!!.setOnClickListener(mListener)
        mClear!!.setOnLongClickListener(object : OnLongClickListener {
            override fun onLongClick(v: View?): Boolean {
                onClear()
                return true
            }
        })

        val adapter = FloatingCalculatorPageAdapter(context, mListener, mHistory)
        mPager!!.setAdapter(adapter)
        mPager!!.setCurrentItem(1)

        child.setOnTouchListener(object : OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                return true
            }
        })
        child.findViewById<View?>(R.id.display_wrapper).bringToFront()

        setState(State.DELETE)

        return child
    }

    private fun onDelete() {
        setState(State.DELETE)
        mDisplay!!.backspace()
    }

    private fun onClear() {
        setState(State.CLEAR)
        mDisplay!!.clear()
    }

    private fun setText(text: String?) {
        setState(State.DELETE)
        mDisplay!!.setText(text)
    }

    private fun onInsert(text: String?) {
        if (mState != State.DELETE) {
            setText(text)
            return
        }

        setState(State.DELETE)
        text?.let {
            mDisplay!!.insert(text)
        }
    }

    private fun onError(resId: Int) {
        setState(State.ERROR)
        mDisplay!!.setText(resId)
    }

    private fun setState(state: State) {
        mDelete!!.setVisibility(if (state == State.DELETE) View.VISIBLE else View.GONE)
        mClear!!.setVisibility(if (state != State.DELETE) View.VISIBLE else View.GONE)
        if (mState != state) {
            when (state) {
                State.CLEAR -> {}
                State.DELETE -> mDisplay!!.setTextColor(resources.getColor(R.color.display_formula_text_color))
                State.ERROR -> mDisplay!!.setTextColor(resources.getColor(R.color.calculator_error_color))
            }
            mState = state
        }
    }

    private fun copyContent(text: String) {
        val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(null, text))
        val toastText = String.format(resources.getString(R.string.text_copied_toast), text)
        Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
    }
}
