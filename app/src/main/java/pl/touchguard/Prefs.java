package pl.touchguard;

import android.content.Context;
import android.content.SharedPreferences;

final class Prefs {
    static final int DEFAULT_SECONDS = 60;
    static final int MIN_SECONDS = 15;
    static final int MAX_SECONDS = 1800;
    private static final String NAME = "touchguard_settings";
    private static final String KEY_SECONDS = "timeout_seconds";

    private Prefs() { }

    static int getSeconds(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
        int value = prefs.getInt(KEY_SECONDS, DEFAULT_SECONDS);
        return Math.max(MIN_SECONDS, Math.min(MAX_SECONDS, value));
    }

    static void setSeconds(Context context, int seconds) {
        int clamped = Math.max(MIN_SECONDS, Math.min(MAX_SECONDS, seconds));
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
                .edit().putInt(KEY_SECONDS, clamped).apply();
    }

    static String formatDuration(int seconds) {
        if (seconds < 60) return seconds + " s";
        int minutes = seconds / 60;
        int remainder = seconds % 60;
        return remainder == 0 ? minutes + " min" : minutes + " min " + remainder + " s";
    }
}
