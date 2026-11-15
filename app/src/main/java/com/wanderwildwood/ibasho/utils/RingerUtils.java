package com.wanderwildwood.ibasho.utils;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

public class RingerUtils {

    /**
     * The chosen sound, or the phone's alarm sound when the chosen one is gone (a file deleted, a
     * card out), or null when the phone has neither: the ring screen then says it cannot sound.
     */
    @androidx.annotation.Nullable
    public static Ringtone getRingtone(Context context, String ringtone) {
        Ringtone r = null;
        try {
            r = RingtoneManager.getRingtone(context, Uri.parse(ringtone));
        } catch (Exception ignored) {
        }
        if (r == null) {
            r = RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
        }
        if (r == null) {
            return null;
        }
        AudioAttributes aa = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                .build();
        r.setAudioAttributes(aa);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            r.setLooping(true);
        }
        return r;
    }

    public static String getDefaultRingtoneAsString() {
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString();
    }

}
