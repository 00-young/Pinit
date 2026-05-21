package com.example.pinit.database;

import com.example.pinit.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PlacesApiHelper {

    public static final String API_KEY = BuildConfig.GOOGLE_MAPS_API_KEY;

    private static final String BASE_URL = "https://maps.googleapis.com/maps/api/place";
    private OkHttpClient client = new OkHttpClient();

    public interface PlacesCallback {
        void onSuccess(List<Map<String, String>> places);
        void onError(String error);
    }

    // 텍스트 검색
    public void searchPlaces(String query, String location, PlacesCallback callback) {
        new Thread(() -> {
            try {
                String url = BASE_URL + "/textsearch/json?query="
                        + java.net.URLEncoder.encode(query, "UTF-8")
                        + "&language=ko&key=" + API_KEY;
                if (location != null) url += "&location=" + location + "&radius=5000";
                callback.onSuccess(parsePlaces(get(url)));
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    // 주변 장소 검색
    public void searchNearby(double lat, double lng, String type, int radius, PlacesCallback callback) {
        new Thread(() -> {
            try {
                String url = BASE_URL + "/nearbysearch/json?location=" + lat + "," + lng
                        + "&radius=" + radius + "&type=" + type + "&language=ko&key=" + API_KEY;
                callback.onSuccess(parsePlaces(get(url)));
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    // 장소 상세 정보 (lat/lng 추가 추출)
    public void getPlaceDetail(String placeId, PlacesCallback callback) {
        new Thread(() -> {
            try {
                String url = BASE_URL + "/details/json?place_id=" + placeId
                        + "&fields=name,formatted_address,rating,formatted_phone_number,opening_hours,website,photos,reviews,geometry"
                        + "&language=ko&key=" + API_KEY;

                String response = get(url);
                JSONObject json = new JSONObject(response);
                JSONObject result = json.optJSONObject("result");

                List<Map<String, String>> list = new ArrayList<>();
                if (result != null) {
                    Map<String, String> detail = new HashMap<>();
                    detail.put("name", result.optString("name", ""));
                    detail.put("address", result.optString("formatted_address", ""));
                    detail.put("phone", result.optString("formatted_phone_number", ""));
                    detail.put("website", result.optString("website", ""));
                    detail.put("rating", result.optString("rating", ""));

                    // ★ lat/lng 추출
                    JSONObject geometry = result.optJSONObject("geometry");
                    if (geometry != null) {
                        JSONObject loc = geometry.optJSONObject("location");
                        if (loc != null) {
                            detail.put("lat", String.valueOf(loc.optDouble("lat", 0)));
                            detail.put("lng", String.valueOf(loc.optDouble("lng", 0)));
                        }
                    }

                    // 영업시간
                    JSONObject hours = result.optJSONObject("opening_hours");
                    if (hours != null) {
                        JSONArray weekday = hours.optJSONArray("weekday_text");
                        if (weekday != null) {
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < weekday.length(); i++) {
                                sb.append(weekday.getString(i)).append("\n");
                            }
                            detail.put("hours", sb.toString().trim());
                        }
                        detail.put("open_now", hours.optBoolean("open_now", false) ? "영업 중" : "영업 종료");
                    }

                    // 리뷰
                    JSONArray reviews = result.optJSONArray("reviews");
                    if (reviews != null && reviews.length() > 0) {
                        StringBuilder reviewText = new StringBuilder();
                        for (int i = 0; i < Math.min(3, reviews.length()); i++) {
                            JSONObject review = reviews.getJSONObject(i);
                            reviewText.append("⭐ ").append(review.optDouble("rating", 0))
                                    .append(" - ").append(review.optString("author_name", ""))
                                    .append("\n").append(review.optString("text", "")).append("\n\n");
                        }
                        detail.put("reviews", reviewText.toString().trim());
                    }
                    list.add(detail);
                }
                callback.onSuccess(list);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    // 음식점 추천
    public void searchRestaurants(double lat, double lng, PlacesCallback callback) {
        searchNearby(lat, lng, "restaurant", 2000, callback);
    }

    private List<Map<String, String>> parsePlaces(String json) throws Exception {
        List<Map<String, String>> list = new ArrayList<>();
        JSONObject obj = new JSONObject(json);
        JSONArray results = obj.optJSONArray("results");
        if (results == null) return list;

        for (int i = 0; i < results.length(); i++) {
            JSONObject place = results.getJSONObject(i);
            Map<String, String> map = new HashMap<>();
            map.put("place_id", place.optString("place_id", ""));
            map.put("name", place.optString("name", ""));
            map.put("address", place.optString("formatted_address",
                    place.optString("vicinity", "")));
            map.put("rating", place.optString("rating", ""));
            map.put("user_ratings_total", place.optString("user_ratings_total", "0"));
            map.put("types", place.optJSONArray("types") != null
                    ? place.optJSONArray("types").toString() : "");

            JSONObject geometry = place.optJSONObject("geometry");
            if (geometry != null) {
                JSONObject loc = geometry.optJSONObject("location");
                if (loc != null) {
                    map.put("lat", String.valueOf(loc.optDouble("lat", 0)));
                    map.put("lng", String.valueOf(loc.optDouble("lng", 0)));
                }
            }
            list.add(map);
        }
        return list;
    }

    private String get(String url) throws IOException {
        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        }
    }
}