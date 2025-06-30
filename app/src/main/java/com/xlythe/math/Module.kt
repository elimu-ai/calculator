package com.xlythe.math

/**
 * A super class for BaseModule, GraphModule, MatrixModule
 */
open class Module internal constructor(
// Used whenever math is necessary
    val solver: Solver?
) {
    // Used for formatting Dec, Bin, and Hex.
    // Dec looks like 1,234,567. Bin is 1010 1010. Hex is 0F 1F 2F.
    val decSeparatorDistance: Int = 3
    val binSeparatorDistance: Int = 4
    val hexSeparatorDistance: Int = 2

    val decimalPoint: Char
        get() = Constants.DECIMAL_POINT

    val decSeparator: Char
        get() = Constants.DECIMAL_SEPARATOR

    val binSeparator: Char
        get() = Constants.BINARY_SEPARATOR

    val hexSeparator: Char
        get() = Constants.HEXADECIMAL_SEPARATOR

    val matrixSeparator: Char
        get() = Constants.MATRIX_SEPARATOR
}
