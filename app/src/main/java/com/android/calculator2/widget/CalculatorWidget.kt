package com.android.calculator2.widget

import ai.elimu.calculator.R
import ai.elimu.model.v2.enums.content.NumeracySkill
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.android.calculator2.CalculatorExpressionTokenizer
import com.android.calculator2.receiver.StudentUpdatedReceiver
import com.android.calculator2.util.DigitLabelHelper
import com.android.calculator2.util.DigitLabelHelper.DigitLabelHelperCallback
import com.android.calculator2.util.PlayerUtil
import com.xlythe.math.Base
import com.xlythe.math.Constants
import com.xlythe.math.EquationFormatter
import com.xlythe.math.Persist
import com.xlythe.math.Solver
import org.javia.arity.SyntaxException
import java.text.DecimalFormatSymbols
import androidx.core.text.isDigitsOnly

class CalculatorWidget : AppWidgetProvider() {
    private var mClearText = false

    override fun onReceive(context: Context, intent: Intent) {
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0)
        var value: String? = getValue(context, appWidgetId)
        if (value == context.resources.getString(R.string.error_syntax)) {
            value = ""
        }
        mClearText = intent.getBooleanExtra(SHOW_CLEAR, false)

        // Play audio for numbers and operators
        val action = intent.action!!.substring(intent.action!!.lastIndexOf(".") + 1)
        if (PlayerUtil.RAW_FILE_EQUALS != action) {
            PlayerUtil.playRawFile(context, action)
        }

        if (intent.action == DIGIT_0) {
            if (mClearText) {
                value = ""
                mClearText = false
            }
            value += "0"
        } else if (intent.action == DIGIT_1) {
            if (mClearText) {
                value = ""
                mClearText = false
            }
            value += "1"
        } else if (intent.action == DIGIT_2) {
            if (mClearText) {
                value = ""
                mClearText = false
            }
            value += "2"
        } else if (intent.action == DIGIT_3) {
            if (mClearText) {
                value = ""
                mClearText = false
            }
            value += "3"
        } else if (intent.action == DIGIT_4) {
            if (mClearText) {
                value = ""
                mClearText = false
            }
            value += "4"
        } else if (intent.action == DIGIT_5) {
            if (mClearText) {
                value = ""
                mClearText = false
            }
            value += "5"
        } else if (intent.action == DIGIT_6) {
            if (mClearText) {
                value = ""
                mClearText = false
            }
            value += "6"
        } else if (intent.action == DIGIT_7) {
            if (mClearText) {
                value = ""
                mClearText = false
            }
            value += "7"
        } else if (intent.action == DIGIT_8) {
            if (mClearText) {
                value = ""
                mClearText = false
            }
            value += "8"
        } else if (intent.action == DIGIT_9) {
            if (mClearText) {
                value = ""
                mClearText = false
            }
            value += "9"
        } else if (intent.action == DOT) {
            if (mClearText) {
                value = ""
                mClearText = false
            }
            value = addDecimal(value)
        } else if (intent.action == DIV) {
            value = addOperator(value!!, Constants.DIV)
        } else if (intent.action == MUL) {
            value = addOperator(value!!, Constants.MUL)
        } else if (intent.action == MINUS) {
            value = addOperator(value!!, Constants.MINUS)
        } else if (intent.action == PLUS) {
            value = addOperator(value!!, Constants.PLUS)
        } else if (intent.action == EQUALS) {
            if (mClearText) {
                value = ""
                mClearText = false
            } else {
                mClearText = true
            }

            val tokenizer = CalculatorExpressionTokenizer(context)
            val input = tokenizer.getNormalizedExpression(value!!)
            if (input.isEmpty()) return

            val logic = Solver()
            logic.setLineLength(7)

            try {
                val output = logic.solve(input) ?: ""
                value = tokenizer.getLocalizedExpression(output)
            } catch (e: SyntaxException) {
                value = context.resources.getString(R.string.error_syntax)
            }

            // Try to save it to history
            if (value != context.resources.getString(R.string.error_syntax)) {
                // Play audio for result

                if (value.isDigitsOnly() && value.toInt() < 10) {
                    PlayerUtil.playResult(context, PlayerUtil.NUMBER_RAW_FILES[value.toInt()])
                } else {
                    PlayerUtil.playRawFile(context, PlayerUtil.RAW_FILE_EQUALS)
                }

                val persist = Persist(context)
                persist.load()
                if (persist.mode == null) persist.mode = Base.DECIMAL
                val history = persist.history
                history.enter(input, value)
                persist.save()
            }
        } else if (intent.action == CLR) {
            value = ""
        } else if (intent.action == DEL) {
            if (value!!.isNotEmpty()) value = value.substring(0, value.length - 1)
        }
        setValue(context, appWidgetId, value)

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds =
            appWidgetManager.getAppWidgetIds(ComponentName(context, CalculatorWidget::class.java))
        for (appWidgetID in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetID)
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        for (appWidgetID in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetID)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
    ) {
        val remoteViews = RemoteViews(context.packageName, R.layout.widget)

        var value: String = getValue(context, appWidgetId)

        val formatter = EquationFormatter()
        value = formatter.addComas(Solver(), value)

        val displayId =
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1) R.id.display_long_clickable else R.id.display

        remoteViews.setViewVisibility(displayId, View.VISIBLE)
        remoteViews.setTextViewText(displayId, value)
        remoteViews.setTextViewText(R.id.display, value)
        remoteViews.setViewVisibility(R.id.delete, if (mClearText) View.GONE else View.VISIBLE)
        remoteViews.setViewVisibility(R.id.clear, if (mClearText) View.VISIBLE else View.GONE)
        setOnClickListeners(context, appWidgetId, remoteViews)

        // Personalize available operators
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val numeracySkillSet = sharedPreferences.getStringSet(
            StudentUpdatedReceiver.PREF_STUDENT_NUMERACY_SKILLS,
            null
        )
        Log.d(javaClass.getName(), "numeracySkillSet: $numeracySkillSet")
        if (numeracySkillSet == null) {
            remoteViews.setViewVisibility(R.id.mul, View.INVISIBLE)
            remoteViews.setViewVisibility(R.id.minus, View.INVISIBLE)
        } else {
            if (!numeracySkillSet.contains(NumeracySkill.MULTIPLICATION.toString())) {
                remoteViews.setViewVisibility(R.id.mul, View.INVISIBLE)
            }
            if (!numeracySkillSet.contains(NumeracySkill.SUBTRACTION.toString())) {
                remoteViews.setViewVisibility(R.id.minus, View.INVISIBLE)
            }
        }

        DigitLabelHelper.instance.getTextForDigits(
            context,
            object : DigitLabelHelperCallback {
                override fun setDigitText(id: Int, text: String?) {
                    remoteViews.setTextViewText(id, text)
                }
            })

        try {
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        } catch (e: Exception) {
        }
    }

    private fun setOnClickListeners(context: Context?, appWidgetId: Int, remoteViews: RemoteViews) {
        val intent = Intent(context, CalculatorWidget::class.java)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        intent.putExtra(SHOW_CLEAR, mClearText)

        // The pending intent request code must be unique
        // Not just for these 17 buttons, but for each widget as well
        // Painful T_T Right?
        // So take the id and shift it over 5 bits (enough to store our 17
        // values)
        val shiftedAppWidgetId = appWidgetId shl 5

        // And add our button values (0-16)
        intent.setAction(DIGIT_0)
        remoteViews.setOnClickPendingIntent(
            R.id.digit0,
            PendingIntent.getBroadcast(context, shiftedAppWidgetId + 0, intent,
                PendingIntent.FLAG_IMMUTABLE)
        )

        intent.setAction(DIGIT_1)
        remoteViews.setOnClickPendingIntent(
            R.id.digit1,
            PendingIntent.getBroadcast(context, shiftedAppWidgetId + 1, intent,
                PendingIntent.FLAG_IMMUTABLE)
        )

        intent.setAction(DIGIT_2)
        remoteViews.setOnClickPendingIntent(
            R.id.digit2,
            PendingIntent.getBroadcast(context, shiftedAppWidgetId + 2, intent,
                PendingIntent.FLAG_IMMUTABLE)
        )

        intent.setAction(DIGIT_3)
        remoteViews.setOnClickPendingIntent(
            R.id.digit3,
            PendingIntent.getBroadcast(context, shiftedAppWidgetId + 3, intent,
                PendingIntent.FLAG_IMMUTABLE)
        )

        intent.setAction(DIGIT_4)
        remoteViews.setOnClickPendingIntent(
            R.id.digit4,
            PendingIntent.getBroadcast(context, shiftedAppWidgetId + 4, intent,
                PendingIntent.FLAG_IMMUTABLE)
        )

        intent.setAction(DIGIT_5)
        remoteViews.setOnClickPendingIntent(
            R.id.digit5,
            PendingIntent.getBroadcast(context, shiftedAppWidgetId + 5, intent,
                PendingIntent.FLAG_IMMUTABLE)
        )

        intent.setAction(DIGIT_6)
        remoteViews.setOnClickPendingIntent(
            R.id.digit6,
            PendingIntent.getBroadcast(context, shiftedAppWidgetId + 6, intent,
                PendingIntent.FLAG_IMMUTABLE)
        )

        intent.setAction(DIGIT_7)
        remoteViews.setOnClickPendingIntent(
            R.id.digit7,
            PendingIntent.getBroadcast(context, shiftedAppWidgetId + 7, intent,
                PendingIntent.FLAG_IMMUTABLE)
        )

        intent.setAction(DIGIT_8)
        remoteViews.setOnClickPendingIntent(
            R.id.digit8,
            PendingIntent.getBroadcast(context, shiftedAppWidgetId + 8, intent,
                PendingIntent.FLAG_IMMUTABLE)
        )

        intent.setAction(DIGIT_9)
        remoteViews.setOnClickPendingIntent(
            R.id.digit9,
            PendingIntent.getBroadcast(context, shiftedAppWidgetId + 9, intent,
                PendingIntent.FLAG_IMMUTABLE)
        )

        intent.setAction(DOT)
        remoteViews.setOnClickPendingIntent(
            R.id.dec_point,
            PendingIntent.getBroadcast(context, shiftedAppWidgetId + 10, intent,
                PendingIntent.FLAG_IMMUTABLE)
        )

        intent.setAction(DIV)
        remoteViews.setOnClickPendingIntent(
            R.id.div,
            PendingIntent.getBroadcast(context, shiftedAppWidgetId + 11, intent,
                PendingIntent.FLAG_IMMUTABLE)
        )

        intent.setAction(MUL)
        remoteViews.setOnClickPendingIntent(
            R.id.mul,
            PendingIntent.getBroadcast(context, shiftedAppWidgetId + 12, intent,
                PendingIntent.FLAG_IMMUTABLE)
        )

        intent.setAction(MINUS)
        remoteViews.setOnClickPendingIntent(
            R.id.minus,
            PendingIntent.getBroadcast(context, shiftedAppWidgetId + 13, intent,
                PendingIntent.FLAG_IMMUTABLE)
        )

        intent.setAction(PLUS)
        remoteViews.setOnClickPendingIntent(
            R.id.plus,
            PendingIntent.getBroadcast(context, shiftedAppWidgetId + 14, intent,
                PendingIntent.FLAG_IMMUTABLE)
        )

        intent.setAction(EQUALS)
        remoteViews.setOnClickPendingIntent(
            R.id.equal,
            PendingIntent.getBroadcast(context, shiftedAppWidgetId + 15, intent,
                PendingIntent.FLAG_IMMUTABLE)
        )

        intent.setAction(DEL)
        remoteViews.setOnClickPendingIntent(
            R.id.delete,
            PendingIntent.getBroadcast(context, shiftedAppWidgetId + 16, intent,
                PendingIntent.FLAG_IMMUTABLE)
        )

        intent.setAction(CLR)
        remoteViews.setOnClickPendingIntent(
            R.id.clear,
            PendingIntent.getBroadcast(context, shiftedAppWidgetId + 17, intent,
                PendingIntent.FLAG_IMMUTABLE)
        )
    }

    companion object {
        const val PREFERENCE_WIDGET_PREAMBLE: String = "com.android.calculator2.CALC_WIDGET_VALUE_"
        const val DIGIT_0: String = "com.android.calculator2.digit_0"
        const val DIGIT_1: String = "com.android.calculator2.digit_1"
        const val DIGIT_2: String = "com.android.calculator2.digit_2"
        const val DIGIT_3: String = "com.android.calculator2.digit_3"
        const val DIGIT_4: String = "com.android.calculator2.digit_4"
        const val DIGIT_5: String = "com.android.calculator2.digit_5"
        const val DIGIT_6: String = "com.android.calculator2.digit_6"
        const val DIGIT_7: String = "com.android.calculator2.digit_7"
        const val DIGIT_8: String = "com.android.calculator2.digit_8"
        const val DIGIT_9: String = "com.android.calculator2.digit_9"
        const val DOT: String = "com.android.calculator2.dot"
        const val PLUS: String = "com.android.calculator2.plus"
        const val MINUS: String = "com.android.calculator2.minus"
        const val MUL: String = "com.android.calculator2.mul"
        const val DIV: String = "com.android.calculator2.div"
        const val EQUALS: String = "com.android.calculator2.equals"
        const val CLR: String = "com.android.calculator2.clear"
        const val DEL: String = "com.android.calculator2.delete"
        const val SHOW_CLEAR: String = "com.android.calculator2.show_clear"

        private val decimal: Char
            get() {
                val dfs = DecimalFormatSymbols()
                return dfs.decimalSeparator
            }

        private fun setValue(context: Context?, appWidgetId: Int, newValue: String?) {
            PreferenceManager.getDefaultSharedPreferences(context).edit().putString(
                PREFERENCE_WIDGET_PREAMBLE + appWidgetId, newValue
            ).commit()
        }

        private fun addOperator(equation: String, op: Char): String {
            var equation = equation
            if (equation.isEmpty()) {
                if (op == Constants.MINUS) {
                    equation += op
                }
            } else if (equation.length == 1) {
                val lastChar = equation[0]
                if (!Solver.isOperator(lastChar) && lastChar != decimal) {
                    equation += op
                }
            } else {
                val lastChar = equation[equation.length - 1]
                val lastlastChar = equation[equation.length - 2]

                if (Solver.isOperator(lastlastChar) && Solver.isOperator(lastChar)) {
                    if (op != Constants.MINUS) {
                        equation = equation.substring(0, equation.length - 2)
                        equation += op
                    }
                } else if (Solver.isOperator(lastChar)) {
                    if (lastChar == Constants.MINUS || op != Constants.MINUS) {
                        equation = equation.substring(0, equation.length - 1)
                    }
                    equation += op
                } else if (lastChar != decimal) {
                    equation += op
                }
            }

            return equation
        }

        /*
    Add a decimal symbol at the end of input string as long as there is no decimal symbol
    already present in the last number entry
    */
        private fun addDecimal(equation: String?): String? {
            var equation = equation
            if (equation != null) {
                var index = equation.length - 1
                var foundOperator = false
                val decimal: Char = decimal

                while (index >= 0) {
                    val currChar = equation[index]

                    // If decimal symbol is already present, stop the loop and return back.
                    // Two decimal symbols are not permitted
                    if (currChar == decimal) {
                        break
                    } else if (currChar == Constants.MUL || currChar == Constants.DIV || currChar == Constants.PLUS || currChar == Constants.MINUS) {
                        foundOperator = true
                        break
                    }
                    index--
                }

                /* index would be less than zero either when input string is empty or index reached
               beginning of the string in previous loop
               foundOperator would be true if an operator was found while traversing the string */
                if (index < 0 || foundOperator) {
                    equation += decimal.toString()
                }
            }
            return equation
        }

        private fun getValue(context: Context?, appWidgetId: Int): String {
            return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(
                    PREFERENCE_WIDGET_PREAMBLE + appWidgetId,
                    ""
                )!!
        }
    }
}
