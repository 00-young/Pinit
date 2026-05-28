package com.example.pinit.data;

import android.content.Context;
import android.content.SharedPreferences;

public class MyFollow {
    public static final String USER_PEACH = "peach_user";
    public static final String USER_MINT = "mint_user";

    private static final String PREFS_NAME = "FollowPrefs";
    private static final String KEY_FOLLOWING_PREFIX = "following_";
    private static final String[] KNOWN_USER_IDS = {USER_PEACH, USER_MINT};

    private MyFollow() {
    }

    public static boolean isFollowing(Context context, String userId) {
        return getPrefs(context).getBoolean(KEY_FOLLOWING_PREFIX + userId, false);
    }

    public static void setFollowing(Context context, String userId, boolean following) {
        getPrefs(context).edit().putBoolean(KEY_FOLLOWING_PREFIX + userId, following).apply();
    }

    public static int getFollowingCount(Context context) {
        int count = 0;
        for (String userId : KNOWN_USER_IDS) {
            if (isFollowing(context, userId)) count++;
        }
        return count;
    }

    public static int getFollowerCount() {
        return 2;
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
