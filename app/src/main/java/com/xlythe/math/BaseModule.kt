package com.xlythe.math

import org.javia.arity.SyntaxException
import java.lang.Long
import java.util.regex.Pattern
import kotlin.Any
import kotlin.Array
import kotlin.Char
import kotlin.Int
import kotlin.NumberFormatException
import kotlin.String
import kotlin.arrayOfNulls
import kotlin.math.floor
import kotlin.plus

class BaseModule internal constructor(solver: Solver?) : Module(solver) {
    // Regex to strip out things like "90" from "sin(90)"
    // Modify the constants to include a fake character, SELECTION_HANDLE
    private val REGEX_NUMBER: String = Constants.REGEX_NUMBER!!
        .substring(0, Constants.REGEX_NUMBER!!.length - 1) + SELECTION_HANDLE + "]"
    private val REGEX_NOT_NUMBER: String = Constants.REGEX_NOT_NUMBER!!
        .substring(0, Constants.REGEX_NOT_NUMBER!!.length - 1) + SELECTION_HANDLE + "]"

    // The current base. Defaults to decimal.
    private var mBase = Base.DECIMAL

    // A listener for when the base changes.
    var onBaseChangeListener: OnBaseChangeListener? = null

    var base: Base
        get() = mBase
        set(base) {
            mBase = base
            if (this.onBaseChangeListener != null) onBaseChangeListener!!.onBaseChange(mBase)
        }

    @Throws(SyntaxException::class)
    fun setBase(input: String, base: Base): String? {
        val text = updateTextToNewMode(input, mBase, base)
        this.base = base
        return text
    }

    @Throws(SyntaxException::class)
    fun updateTextToNewMode(originalText: String, oldBase: Base, newBase: Base): String? {
        if (oldBase == newBase || originalText.isEmpty() || originalText.matches(REGEX_NOT_NUMBER.toRegex())) {
            return originalText
        }

        val operations: Array<String?> =
            originalText.split(REGEX_NUMBER.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val numbers: Array<String?> =
            originalText.split(REGEX_NOT_NUMBER.toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
        val translatedNumbers = arrayOfNulls<String>(numbers.size)
        for (i in numbers.indices) {
            if (!numbers[i]!!.isEmpty()) {
                when (oldBase) {
                    Base.BINARY -> when (newBase) {
                        Base.BINARY -> {}
                        Base.DECIMAL -> try {
                            translatedNumbers[i] = newBase(numbers[i]!!, 2, 10)
                        } catch (e: NumberFormatException) {
                            throw SyntaxException()
                        }

                        Base.HEXADECIMAL -> try {
                            translatedNumbers[i] = newBase(numbers[i]!!, 2, 16)
                        } catch (e: NumberFormatException) {
                            throw SyntaxException()
                        }
                    }

                    Base.DECIMAL -> when (newBase) {
                        Base.BINARY -> try {
                            translatedNumbers[i] = newBase(numbers[i]!!, 10, 2)
                        } catch (e: NumberFormatException) {
                            throw SyntaxException()
                        }

                        Base.DECIMAL -> {}
                        Base.HEXADECIMAL -> try {
                            translatedNumbers[i] = newBase(numbers[i]!!, 10, 16)
                        } catch (e: NumberFormatException) {
                            throw SyntaxException()
                        }
                    }

                    Base.HEXADECIMAL -> when (newBase) {
                        Base.BINARY -> try {
                            translatedNumbers[i] = newBase(numbers[i]!!, 16, 2)
                        } catch (e: NumberFormatException) {
                            throw SyntaxException()
                        }

                        Base.DECIMAL -> try {
                            translatedNumbers[i] = newBase(numbers[i]!!, 16, 10)
                        } catch (e: NumberFormatException) {
                            e.printStackTrace()
                            throw SyntaxException()
                        }

                        Base.HEXADECIMAL -> {}
                    }
                }
            }
        }
        var text: String? = ""
        val o = removeWhitespace(operations)
        val n = removeWhitespace(translatedNumbers)
        if (originalText.substring(0, 1).matches(REGEX_NUMBER.toRegex())) {
            var i = 0
            while (i < o.size && i < n.size) {
                text += n[i]
                text += o[i]
                i++
            }
        } else {
            var i = 0
            while (i < o.size && i < n.size) {
                text += o[i]
                text += n[i]
                i++
            }
        }
        if (o.size > n.size) {
            text += o[o.size - 1]
        } else if (n.size > o.size) {
            text += n[n.size - 1]
        }
        return text
    }

    @Throws(SyntaxException::class)
    private fun newBase(originalNumber: String, originalBase: Int, base: Int): String {
        var split: Array<String?> =
            originalNumber.split(Pattern.quote(decimalPoint.toString() + "").toRegex())
                .dropLastWhile { it.isEmpty() }.toTypedArray()
        if (split.isEmpty()) {
            split = arrayOfNulls<String>(1)
            split[0] = "0"
        }
        if (split[0]!!.isEmpty()) {
            split[0] = "0"
        }
        if (originalBase != 10) {
            split[0] = split[0]!!.toLong(originalBase).toString()
        }

        var wholeNumber = ""
        when (base) {
            2 -> wholeNumber = Long.toBinaryString(split[0]!!.toLong())
            10 -> wholeNumber = split[0]!!
            16 -> wholeNumber = Long.toHexString(split[0]!!.toLong())
        }
        if (split.size == 1) return wholeNumber.uppercase()

        // Catch overflow (it's a decimal, it can be (slightly) rounded
        if (split[1]!!.length > 13) {
            split[1] = split[1]!!.substring(0, 13)
        }

        var decimal = 0.0
        if (originalBase != 10) {
            val decimalFraction = split[1]!!.toLong(originalBase)
                .toString() + "/" + originalBase + "^" + split[1]!!.length
            decimal = solver!!.mSymbols.eval(decimalFraction)
        } else {
            decimal = ("0." + split[1]).toDouble()
        }
        if (decimal == 0.0) return wholeNumber.uppercase()

        var decimalNumber = ""
        var i = 0
        var id = 0
        while (decimal != 0.0 && i <= PRECISION) {
            decimal *= base.toDouble()
            id = floor(decimal).toInt()
            decimal -= id.toDouble()
            decimalNumber += Integer.toHexString(id)
            i++
        }
        return (wholeNumber + decimalPoint + decimalNumber).uppercase()
    }

    private fun removeWhitespace(strings: Array<String?>): Array<Any?> {
        val formatted = ArrayList<String?>(strings.size)
        for (s in strings) {
            if (s != null && !s.isEmpty()) formatted.add(s)
        }
        return formatted.toTypedArray()
    }

    fun groupSentence(originalText: String, selectionHandle: Int): String {
        var originalText = originalText
        if (originalText.isEmpty() || originalText.matches(REGEX_NOT_NUMBER.toRegex())) return originalText

        if (selectionHandle >= 0) {
            originalText = originalText.substring(0, selectionHandle) +
                    SELECTION_HANDLE +
                    originalText.substring(selectionHandle)
        }
        val operations: Array<String?> =
            originalText.split(REGEX_NUMBER.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val numbers: Array<String?> =
            originalText.split(REGEX_NOT_NUMBER.toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
        val translatedNumbers = arrayOfNulls<String>(numbers.size)
        for (i in numbers.indices) {
            if (!numbers[i]!!.isEmpty()) {
                translatedNumbers[i] = groupDigits(numbers[i]!!, mBase)
            }
        }
        var text = ""
        val o = removeWhitespace(operations)
        val n = removeWhitespace(translatedNumbers)
        if (originalText.substring(0, 1).matches(REGEX_NUMBER.toRegex())) {
            var i = 0
            while (i < o.size && i < n.size) {
                text += n[i]
                text += o[i]
                i++
            }
        } else {
            var i = 0
            while (i < o.size && i < n.size) {
                text += o[i]
                text += n[i]
                i++
            }
        }
        if (o.size > n.size) {
            text += o[o.size - 1]
        } else if (n.size > o.size) {
            text += n[n.size - 1]
        }

        return text
    }

    fun groupDigits(number: String, base: Base): String {
        var number = number
        var sign = ""
        if (Solver.isNegative(number)) {
            sign = Constants.MINUS.toString()
            number = number.substring(1)
        }
        var wholeNumber = number
        var remainder = ""
        // We only group the whole number
        if (number.contains(decimalPoint.toString() + "")) {
            if (!number.startsWith(decimalPoint.toString() + "")) {
                val temp: Array<String?> =
                    number.split(Pattern.quote(decimalPoint.toString() + "").toRegex())
                        .dropLastWhile { it.isEmpty() }.toTypedArray()
                wholeNumber = temp[0]!!
                remainder = decimalPoint.toString() + (if (temp.size == 1) "" else temp[1])
            } else {
                wholeNumber = ""
                remainder = number
            }
        }

        val modifiedNumber = group(wholeNumber, getSeparatorDistance(base), getSeparator(base))

        return sign + modifiedNumber + remainder
    }

    private fun group(wholeNumber: String, spacing: Int, separator: Char): String {
        val sb = StringBuilder()
        var digitsSeen = 0
        for (i in wholeNumber.length - 1 downTo 0) {
            val curChar = wholeNumber[i]
            if (curChar != SELECTION_HANDLE) {
                if (digitsSeen > 0 && digitsSeen % spacing == 0) {
                    sb.insert(0, separator)
                }
                ++digitsSeen
            }
            sb.insert(0, curChar)
        }
        return sb.toString()
    }

    fun getSeparator(base: Base): Char {
        return when (base) {
            Base.DECIMAL -> decSeparator
            Base.BINARY -> binSeparator
            Base.HEXADECIMAL -> hexSeparator
        }
    }

    val separator: Char
        get() = getSeparator(mBase)

    private fun getSeparatorDistance(base: Base): Int {
        return when (base) {
            Base.DECIMAL -> decSeparatorDistance
            Base.BINARY -> binSeparatorDistance
            Base.HEXADECIMAL -> hexSeparatorDistance
        }
    }

    interface OnBaseChangeListener {
        fun onBaseChange(newBase: Base?)
    }

    companion object {
        private const val TAG = "Calculator"

        // Used to keep a reference to the cursor in text
        const val SELECTION_HANDLE: Char = '\u2620'

        // How many decimal places to approximate base changes
        private const val PRECISION = 8
    }
}
