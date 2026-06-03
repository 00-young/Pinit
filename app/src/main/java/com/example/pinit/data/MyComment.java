package com.example.pinit.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class MyComment {
    public static final String POST_ID_SHANGHAI = "shanghai_trip";

    private static final String PREFS_NAME = "CommentPrefs";
    private static final String KEY_PREFIX = "comments_";

    private MyComment() {
    }

    public static List<String> getComments(Context context, String postId) {
        String json = getPrefs(context).getString(KEY_PREFIX + postId, "[]");
        List<String> comments = new ArrayList<>();

        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                String comment = array.optString(i, "").trim();
                if (!comment.isEmpty()) {
                    comments.add(comment);
                }
            }
        } catch (JSONException ignored) {
            return comments;
        }

        return comments;
    }

    public static void addComment(Context context, String postId, String comment) {
        List<String> comments = getComments(context, postId);
        comments.add(comment);
        saveComments(context, postId, comments);
    }

    private static void saveComments(Context context, String postId, List<String> comments) {
        JSONArray array = new JSONArray();
        for (String comment : comments) {
            array.put(comment);
        }
        getPrefs(context).edit().putString(KEY_PREFIX + postId, array.toString()).apply();
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
