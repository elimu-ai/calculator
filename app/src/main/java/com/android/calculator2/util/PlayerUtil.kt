package com.android.calculator2.util

import com.android.calculator2.util.PlayerUtil.NUMBER_RAW_FILES

/**
 * Created by GSC on 12/11/2016.
 */
object PlayerUtil {
    var RAW_FILE_EQUALS: String = "equals"

    val NUMBER_RAW_FILES: Array<String> = arrayOf<String>(
        "digit_0",
        "digit_1",
        "digit_2",
        "digit_3",
        "digit_4",
        "digit_5",
        "digit_6",
        "digit_7",
        "digit_8",
        "digit_9"
    )
}

fun String.tagToSpokenText(): String {
    return when (this) {
        NUMBER_RAW_FILES[0] -> "0"
        NUMBER_RAW_FILES[1] -> "1"
        NUMBER_RAW_FILES[2] -> "2"
        NUMBER_RAW_FILES[3] -> "3"
        NUMBER_RAW_FILES[4] -> "4"
        NUMBER_RAW_FILES[5] -> "5"
        NUMBER_RAW_FILES[6] -> "6"
        NUMBER_RAW_FILES[7] -> "7"
        NUMBER_RAW_FILES[8] -> "8"
        NUMBER_RAW_FILES[9] -> "9"
        else -> this
    }
}
