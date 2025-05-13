package com.android.calculator2.util

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener

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
