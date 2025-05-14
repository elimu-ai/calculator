package com.xlythe.math

import java.text.DecimalFormatSymbols
import java.util.regex.Pattern

/**
 * Created by Will on 11/8/2014.
 */
object Constants {
    const val INFINITY_UNICODE: String = "\u221e"

    // Double.toString() for Infinity
    const val INFINITY: String = "Infinity"

    // Double.toString() for NaN
    const val NAN: String = "NaN"
    const val MINUS: Char = '\u2212'
    const val MUL: Char = '\u00d7'
    const val PLUS: Char = '+'
    const val DIV: Char = '\u00f7'
    const val PLACEHOLDER: Char = '\u200B'
    const val POWER: Char = '^'
    const val EQUAL: Char = '='
    const val LEFT_PAREN: Char = '('
    const val RIGHT_PAREN: Char = ')'

    // Values for decimals and comas
    private var DECIMAL_FORMAT: DecimalFormatSymbols? = null
    @JvmField
    var DECIMAL_POINT: Char = 0.toChar()
    var DECIMAL_SEPARATOR: Char = 0.toChar()
    var BINARY_SEPARATOR: Char = 0.toChar()
    var HEXADECIMAL_SEPARATOR: Char = 0.toChar()
    var MATRIX_SEPARATOR: Char = 0.toChar()

    @JvmField
    var REGEX_NUMBER: String? = null
    @JvmField
    var REGEX_NOT_NUMBER: String? = null

    init {
        rebuildConstants()
    }

    /**
     * If the locale changes, but the app is still in memory, you may need to rebuild these constants
     */
    fun rebuildConstants() {
        DECIMAL_FORMAT = DecimalFormatSymbols()

        // These will already be known by Java
        DECIMAL_POINT = DECIMAL_FORMAT!!.decimalSeparator
        DECIMAL_SEPARATOR = DECIMAL_FORMAT!!.groupingSeparator

        // Use a space for Bin and Hex
        BINARY_SEPARATOR = ' '
        HEXADECIMAL_SEPARATOR = ' '

        // We have to be careful with the Matrix Separator.
        // It defaults to "," but that's a common decimal point.
        MATRIX_SEPARATOR = if (DECIMAL_POINT == ',') ' '
        else ','

        val number = "A-F0-9" +
                Pattern.quote(DECIMAL_POINT.toString()) +
                Pattern.quote(DECIMAL_SEPARATOR.toString()) +
                Pattern.quote(BINARY_SEPARATOR.toString()) +
                Pattern.quote(HEXADECIMAL_SEPARATOR.toString())

        REGEX_NUMBER = "[" + number + "]"
        REGEX_NOT_NUMBER = "[^" + number + "]"
    }
}
