package com.android.calculator2.util;

import android.content.Context;
import android.media.MediaPlayer;

/**
 * Created by GSC on 12/11/2016.
 */
public class PlayerUtil {

    public static void playRawFile(Context context, String rawFile) {
        int rawId = context.getResources().getIdentifier(rawFile, "raw", context.getPackageName());
        if (rawId != 0) {
            MediaPlayer mediaPlayer = MediaPlayer.create(context, rawId);
            mediaPlayer.start();
        }
    }
}
