package com.xlythe.math

import android.content.Context
import org.javia.arity.Symbols
import org.javia.arity.SyntaxException
import java.util.Locale

/**
 * Solves math problems
 *
 * Supports:
 * Basic math + functions (trig, pi)
 * Matrices
 * Hex and Bin conversion
 */
class Solver {
    // Used for solving basic math
    var mSymbols: Symbols = Symbols()
    val baseModule: BaseModule = BaseModule(this)
    val matrixModule: MatrixModule
    private var mLineLength = 14
    private var mLocalizer: Localizer? = null

    init {
        this.matrixModule = MatrixModule(this)
    }

    /**
     * Input an equation as a string
     * ex: sin(150)
     * and get the result returned.
     */
    @Throws(SyntaxException::class)
    fun solve(input: String): String? {
        var input = input
        if (displayContainsMatrices(input)) {
            return matrixModule.evaluateMatrices(input)
        }

        if (input.trim { it <= ' ' }.isEmpty()) {
            return ""
        }

        if (mLocalizer != null) input = mLocalizer!!.localize(input)

        // Drop final infix operators (they can only result in error)
        var size = input.length
        while (size > 0 && isOperator(input[size - 1])) {
            input = input.substring(0, size - 1)
            --size
        }

        // Convert to decimal
        val decimalInput = convertToDecimal(input)

        val value = mSymbols.evalComplex(decimalInput)

        var real = ""
        for (precision in mLineLength downTo 7) {
            real = tryFormattingWithPrecision(value.re, precision)
            if (real.length <= mLineLength) {
                break
            }
        }

        var imaginary = ""
        for (precision in mLineLength downTo 7) {
            imaginary = tryFormattingWithPrecision(value.im, precision)
            if (imaginary.length <= mLineLength) {
                break
            }
        }

        real = baseModule.updateTextToNewMode(real, Base.DECIMAL, baseModule.base)!!
            .replace('-', Constants.MINUS)
            .replace(Constants.INFINITY, Constants.INFINITY_UNICODE)
        imaginary = baseModule.updateTextToNewMode(imaginary, Base.DECIMAL, baseModule.base)!!
            .replace('-', Constants.MINUS)
            .replace(Constants.INFINITY, Constants.INFINITY_UNICODE)

        var result = ""
        if (value.re != 0.0 && value.im == 1.0) result = real + "+" + "i"
        else if (value.re != 0.0 && value.im > 0) result = real + "+" + imaginary + "i"
        else if (value.re != 0.0 && value.im == -1.0) result = real + "-" + "i"
        else if (value.re != 0.0 && value.im < 0) result = real + imaginary + "i" // Implicit -
        else if (value.re != 0.0 && value.im == 0.0) result = real
        else if (value.re == 0.0 && value.im == 1.0) result = "i"
        else if (value.re == 0.0 && value.im == -1.0) result = "-i"
        else if (value.re == 0.0 && value.im != 0.0) result = imaginary + "i"
        else if (value.re == 0.0 && value.im == 0.0) result = "0"

        if (mLocalizer != null) result = mLocalizer!!.relocalize(result)

        return result
    }

    fun displayContainsMatrices(text: String): Boolean {
        return this.matrixModule.isMatrix(text)
    }

    @Throws(SyntaxException::class)
    fun convertToDecimal(input: String): String? {
        return baseModule.updateTextToNewMode(input, baseModule.base, Base.DECIMAL)
    }

    @Throws(SyntaxException::class)
    fun tryFormattingWithPrecision(value: Double, precision: Int): String {
        // The standard scientific formatter is basically what we need. We will
        // start with what it produces and then massage it a bit.
        val isNaN = (value.toString().trim { it <= ' ' }.equals(Constants.NAN, ignoreCase = true))
        if (isNaN) {
            throw SyntaxException()
        }
        var result = String.format(Locale.US, "%" + mLineLength + "." + precision + "g", value)
        var mantissa = result
        var exponent: String? = null
        val e = result.indexOf('e')
        if (e != -1) {
            mantissa = result.substring(0, e)

            // Strip "+" and unnecessary 0's from the exponent
            exponent = result.substring(e + 1)
            if (exponent.startsWith("+")) {
                exponent = exponent.substring(1)
            }
            exponent = exponent.toInt().toString()
        }

        var period = mantissa.indexOf('.')
        if (period == -1) {
            period = mantissa.indexOf(',')
        }
        if (period != -1) {
            // Strip trailing 0's
            while (mantissa.length > 0 && mantissa.endsWith("0")) {
                mantissa = mantissa.substring(0, mantissa.length - 1)
            }
            if (mantissa.length == period + 1) {
                mantissa = mantissa.substring(0, mantissa.length - 1)
            }
        }

        if (exponent != null) {
            result = mantissa + 'e' + exponent
        } else {
            result = mantissa
        }
        return result
    }

    fun enableLocalization(context: Context, r: Class<*>) {
        mLocalizer = Localizer(context, r)
    }

    fun setLineLength(length: Int) {
        mLineLength = length
    }

    var base: Base
        get() = baseModule.base
        set(base) {
            baseModule.base = base
        }

    companion object {
        @JvmStatic
        fun isOperator(c: Char): Boolean {
            return ("" +
                    Constants.PLUS +
                    Constants.MINUS +
                    Constants.DIV +
                    Constants.MUL +
                    Constants.POWER).indexOf(c) != -1
        }

        @JvmStatic
        fun isOperator(c: String): Boolean {
            return isOperator(c[0])
        }

        fun isNegative(number: String): Boolean {
            return number.startsWith(Constants.MINUS.toString()) || number.startsWith("-")
        }

        fun isDigit(number: Char): Boolean {
            return number.toString().matches(Constants.REGEX_NUMBER!!.toRegex())
        }
    }
}
