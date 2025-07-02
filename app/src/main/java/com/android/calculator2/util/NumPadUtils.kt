package com.android.calculator2.util

import com.android.calculator2.util.NumPadUtils.DIGIT_TAGS

/**
 * Created by GSC on 12/11/2016.
 */
object NumPadUtils {
    const val RAW_FILE_EQUALS: String = "equals"

    val DIGIT_TAGS: Array<String> = arrayOf<String>(
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
        DIGIT_TAGS[0] -> "0"
        DIGIT_TAGS[1] -> "1"
        DIGIT_TAGS[2] -> "2"
        DIGIT_TAGS[3] -> "3"
        DIGIT_TAGS[4] -> "4"
        DIGIT_TAGS[5] -> "5"
        DIGIT_TAGS[6] -> "6"
        DIGIT_TAGS[7] -> "7"
        DIGIT_TAGS[8] -> "8"
        DIGIT_TAGS[9] -> "9"
        else -> this
    }
}
