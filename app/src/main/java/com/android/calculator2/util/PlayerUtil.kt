package com.android.calculator2.util

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
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

    fun playRawFile(context: Context, rawFile: String?) {
        val rawId = context.resources.getIdentifier(rawFile, "raw", context.packageName)
        if (rawId != 0) {
            val mediaPlayer = MediaPlayer.create(context, rawId)
            mediaPlayer.start()
        }
    }

    fun playResult(context: Context, result: String?) {
        val rawId =
            context.resources.getIdentifier(RAW_FILE_EQUALS, "raw", context.packageName)
        if (rawId != 0) {
            val mediaPlayer = MediaPlayer.create(context, rawId)
            mediaPlayer.setOnCompletionListener(object : OnCompletionListener {
                override fun onCompletion(mp: MediaPlayer?) {
                    playRawFile(context, result)
                }
            })
            mediaPlayer.start()
        }
    }
}

fun String.tagToNumber(): String {
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
        else -> ""
    }
}
