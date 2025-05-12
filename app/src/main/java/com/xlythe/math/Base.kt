package com.xlythe.math

/**
 * Represents changing the number of characters available when writing numbers.
 */
enum class Base(var quickSerializable: Int) {
    BINARY(2),
    DECIMAL(10),
    HEXADECIMAL(16)
}
