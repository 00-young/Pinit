package com.example.pinit.data;

import android.content.Context;
import android.content.SharedPreferences;

public class MyScrap {
    public static final String POST_ID_SHANGHAI = "shanghai_trip";

    private static final String PREFS_NAME = "ScrapPrefs";

    private MyScrap() {
    }

    public static boolean isScraped(Context context, String postId) {
        return getPrefs(context).getBoolean(postId, false);
    }

    public static void setScraped(Context context, String postId, boolean scraped) {
        getPrefs(context).edit().putBoolean(postId, scraped).apply();
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
