package com.example.pinit.service;

import android.util.Log;

import com.example.pinit.database.PlacesApiHelper;
import com.example.pinit.service.CongestionData;
import com.example.pinit.model.RecommendedPlace;
import com.example.pinit.model.UserPreference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class RecommendationManager {

    private final RecommendationService recommendationService =
            new RecommendationService();

    private final CongestionService congestionService =
            new CongestionService();

    private final PlacesApiHelper placesApiHelper =
            new PlacesApiHelper();

    public interface RecommendationCallback {

        void onSuccess(
                List<RecommendedPlace> recommendedPlaces
        );

        void onFailure(
                String error
        );
    }

    public void getRecommendations(

            UserPreference user,
            double lat,
            double lng,
            RecommendationCallback callback
            ) {

        List<String> categories =
                recommendationService.getRecommendedCategories(user);

        if (categories == null || categories.isEmpty()) {

            callback.onFailure("카테고리가 없습니다.");
            return;
        }

        List<RecommendedPlace> recommendedPlaces =
                Collections.synchronizedList(new ArrayList<>());

        final int totalCategories = categories.size();

        final int[] completedCategories = {0};

        for (String category : categories) {

            Log.d("CATEGORY", "시작 : " + category);

            placesApiHelper.searchNearby(
                    lat,
                    lng,
                    category,

                    3000,

                    new PlacesApiHelper.PlacesCallback() {

                        @Override
                        public void onSuccess(
                                List<Map<String, String>> places
                        ) {

                            if (places == null || places.isEmpty()) {

                                finishCategory(
                                        completedCategories,
                                        totalCategories,
                                        recommendedPlaces,
                                        callback
                                );

                                return;
                            }

                            final int totalPlaces =
                                    places.size();

                            final int[] completedPlaces = {0};

                            for (Map<String, String> place : places) {

                                processPlace(

                                        user,

                                        place,

                                        recommendedPlaces,

                                        completedPlaces,

                                        totalPlaces,

                                        completedCategories,

                                        totalCategories,

                                        callback
                                );
                            }
                        }

                        @Override
                        public void onError(
                                String error
                        ) {

                            Log.e(
                                    "PLACES_ERROR",
                                    error
                            );

                            finishCategory(
                                    completedCategories,
                                    totalCategories,
                                    recommendedPlaces,
                                    callback
                            );
                        }
                    }
            );
        }
    }

    private void processPlace(

            UserPreference user,

            Map<String, String> place,

            List<RecommendedPlace> recommendedPlaces,

            int[] completedPlaces,

            int totalPlaces,

            int[] completedCategories,

            int totalCategories,

            RecommendationCallback callback
    ) {

        double baseScore =
                recommendationService.calculateScore(
                        user,
                        place
                );

        String placeName =
                place.get("name");

        double rating = 0;

        try {

            String ratingText =
                    place.get("rating");

            if (ratingText != null &&
                    !ratingText.isEmpty()) {

                rating =
                        Double.parseDouble(
                                ratingText
                        );
            }

        } catch (Exception e) {

            rating = 0;
        }

        final double finalRating = rating;

        congestionService.getCongestionData(

                placeName,

                new CongestionCallback() {

                    @Override
                    public void onSuccess(
                            CongestionData data
                    ) {

                        double congestionScore =
                                recommendationService
                                        .calculateCongestionScore(
                                                data.getCongestionLevel()
                                        );

                        double finalScore =
                                baseScore + congestionScore;
                        String detailScore =

                                "테마: "
                                        + user.getTheme()

                                        + " / 예산: "
                                        + user.getBudgetType()

                                        + " / 평점: +"
                                        + finalRating

                                        + " / 혼잡도: "
                                        + data.getCongestionLevel();

                        RecommendedPlace recommendedPlace =
                                new RecommendedPlace(

                                        place.get("name"),

                                        place.get("address"),

                                        place.get("types"),

                                        finalRating,

                                        data.getCongestionLevel(),

                                        finalScore,

                                        detailScore

                                );

                        recommendedPlaces.add(
                                recommendedPlace
                        );

                        finishPlace(

                                completedPlaces,

                                totalPlaces,

                                completedCategories,

                                totalCategories,

                                recommendedPlaces,

                                callback
                        );
                    }

                    @Override
                    public void onFailure(
                            String error
                    ) {
                        String detailScore =

                                "테마: "
                                        + user.getTheme()

                                        + " / 예산: "
                                        + user.getBudgetType()

                                        + " / 평점: "
                                        + finalRating

                                        + " / 혼잡도: 정보 없음";


                        RecommendedPlace recommendedPlace =
                                new RecommendedPlace(

                                        place.get("name"),

                                        place.get("address"),

                                        place.get("types"),

                                        finalRating,

                                        "정보 없음",

                                        baseScore,

                                        detailScore
                                );


                        recommendedPlaces.add(
                                recommendedPlace
                        );

                        finishPlace(

                                completedPlaces,

                                totalPlaces,

                                completedCategories,

                                totalCategories,

                                recommendedPlaces,

                                callback
                        );
                    }
                }
        );
    }

    private synchronized void finishPlace(

            int[] completedPlaces,

            int totalPlaces,

            int[] completedCategories,

            int totalCategories,

            List<RecommendedPlace> recommendedPlaces,

            RecommendationCallback callback
    ) {

        completedPlaces[0]++;

        Log.d(
                "PLACE_PROGRESS",

                completedPlaces[0]
                        + " / "
                        + totalPlaces
        );

        if (completedPlaces[0] == totalPlaces) {

            finishCategory(

                    completedCategories,

                    totalCategories,

                    recommendedPlaces,

                    callback
            );
        }
    }

    private synchronized void finishCategory(

            int[] completedCategories,

            int totalCategories,

            List<RecommendedPlace> recommendedPlaces,

            RecommendationCallback callback
    ) {

        completedCategories[0]++;

        Log.d(
                "CATEGORY_PROGRESS",

                completedCategories[0]
                        + " / "
                        + totalCategories
        );

        if (completedCategories[0] == totalCategories) {

            Collections.sort(

                    recommendedPlaces,

                    new Comparator<RecommendedPlace>() {

                        @Override
                        public int compare(

                                RecommendedPlace p1,

                                RecommendedPlace p2
                        ) {

                            return Double.compare(

                                    p2.getScore(),

                                    p1.getScore()
                            );
                        }
                    }
            );

            List<RecommendedPlace> topPlaces =
                    recommendedPlaces.subList(

                            0,

                            Math.min(
                                    10,
                                    recommendedPlaces.size()
                            )
                    );

            Log.d(
                    "FINAL_RECOMMEND",

                    "최종 추천 완료 : "
                            + topPlaces.size()
            );

            callback.onSuccess(
                    topPlaces
            );
        }
    }
}