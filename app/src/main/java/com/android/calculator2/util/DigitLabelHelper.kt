package com.android.calculator2.util

import ai.elimu.calculator.R
import android.content.Context
import java.text.DecimalFormatSymbols
import java.util.Locale

class DigitLabelHelper {
    private var mCachedLocaleHash = 0
    private var mDecFormatSymbols: DecimalFormatSymbols? = null

    interface DigitLabelHelperCallback {
        fun setDigitText(id: Int, text: String?)
    }

    private fun getDecimalFormatForCurrentLocale(context: Context): DecimalFormatSymbols {
        val resources = context.getResources()
        var locale = resources.getConfiguration().locale
        if (locale.hashCode() != mCachedLocaleHash) {
            if (!resources.getBoolean(R.bool.use_localized_digits)) {
                locale = Locale.Builder()
                    .setLocale(locale)
                    .setUnicodeLocaleKeyword(UNICODE_LOCALE_KEY, UNICODE_LOCALE_VALUE)
                    .build()
            }
            mCachedLocaleHash = locale.hashCode()
            mDecFormatSymbols = DecimalFormatSymbols.getInstance(locale)
        }
        return mDecFormatSymbols!!
    }

    fun getTextForDigits(context: Context, callback: DigitLabelHelperCallback) {
        val symbols = getDecimalFormatForCurrentLocale(context)
        val zeroDigit = symbols.getZeroDigit()
        for (i in sDigitIds.indices) {
            val id: Int = sDigitIds[i]
            if (id == R.id.dec_point) {
                callback.setDigitText(id, symbols.getDecimalSeparator().toString())
            } else {
                callback.setDigitText(id, (zeroDigit.code + i).toChar().toString())
            }
        }
    }

    companion object {
        private const val UNICODE_LOCALE_KEY = "nu"
        private const val UNICODE_LOCALE_VALUE = "latn"
        private var sInstance: DigitLabelHelper? = null
        private val sDigitIds = intArrayOf(
            R.id.digit0,
            R.id.digit1,
            R.id.digit2,
            R.id.digit3,
            R.id.digit4,
            R.id.digit5,
            R.id.digit6,
            R.id.digit7,
            R.id.digit8,
            R.id.digit9,
            R.id.dec_point
        )

        @JvmStatic
        @get:Synchronized
        val instance: DigitLabelHelper
            get() {
                if (sInstance == null) {
                    sInstance = DigitLabelHelper()
                }
                return sInstance!!
            }

        fun getIdForDigit(digit: Int): Int {
            return sDigitIds[digit]
        }
    }
}
