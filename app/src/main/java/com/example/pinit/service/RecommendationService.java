package com.example.pinit.service;

import android.util.Log;

import com.example.pinit.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RecommendationService {

    public List<String> getRecommendedCategories(
            User user
    ) {

        List<String> categories =
                new ArrayList<>();

        String theme =
                user.getTheme();

        if (theme == null) return categories;

        if (theme.equals("자연/힐링")) {

            categories.add("park");
            categories.add("aquarium");
            categories.add("zoo");
        }

        else if (theme.equals("맛집 탐방")) {

            categories.add("restaurant");
            categories.add("cafe");
            categories.add("bakery");
        }

        else if (theme.equals("문화/역사")) {

            categories.add("museum");
            categories.add("art_gallery");
            categories.add("tourist_attraction");
        }

        else if (theme.equals("액티비티")) {

            categories.add("amusement_park");
            categories.add("stadium");
        }

        else if (theme.equals("쇼핑")) {

            categories.add("shopping_mall");
            categories.add("department_store");
        }

        else if (theme.equals("포토스팟")) {

            categories.add("tourist_attraction");
            categories.add("cafe");
            categories.add("park");
        }

        return categories;
    }

    public double calculateScore(
            User user,
            Map<String, String> place
    ) {

        double themePoint = 0;

        double ratingPoint = 0;

        double budgetPoint = 0;

        double companionPoint = 0;

        double agePoint = 0;

        double score = 0;

        String types =
                place.get("types");

        // 테마 점수

        String theme = user.getTheme();
        String companion = user.getCompanion();
        String ageGroup = user.getAgeGroup();
        String budget = user.getBudgetType();
        if (types == null) types = "";

        if (theme != null && theme.equals("자연/힐링")) {

            if (types.contains("park")) {

                score += 5;
                themePoint += 5;
            }

            if (types.contains("aquarium")
                    || types.contains("zoo")) {

                score += 3;
                themePoint += 3;
            }
        }

        else if (theme.equals("맛집 탐방")) {

            if (types.contains("restaurant")) {

                score += 5;
                themePoint += 5;
            }

            if (types.contains("cafe")
                    || types.contains("bakery")) {

                score += 3;
                themePoint += 3;
            }
        }

        else if (theme.equals("문화/역사")) {

            if (types.contains("museum")
                    || types.contains("art_gallery")) {

                score += 5;
                themePoint += 5;
            }

            if (types.contains("tourist_attraction")) {

                score += 3;
                themePoint += 3;
            }
        }

        else if (theme.equals("액티비티")) {

            if (types.contains("amusement_park")
                    || types.contains("stadium")) {

                score += 5;
                themePoint += 5;
            }
        }

        else if (theme.equals("쇼핑")) {

            if (types.contains("shopping_mall")
                    || types.contains("department_store")) {

                score += 5;
                themePoint += 5;
            }
        }

        else if (theme.equals("포토스팟")) {

            if (types.contains("tourist_attraction")) {

                score += 5;
                themePoint += 5;
            }

            if (types.contains("cafe")
                    || types.contains("park")) {

                score += 3;
                themePoint += 3;
            }
        }

        // 평점 점수
        String ratingText =
                place.get("rating");

        if (ratingText != null
                && !ratingText.isEmpty()) {

            double rating =
                    Double.parseDouble(
                            ratingText
                    );

            score += rating;
            ratingPoint += rating;
        }

        //동행자 점수

        if ("연인".equals(companion)) {

            if (types.contains("cafe")) {

                score += 2;
                companionPoint += 2;
            }

            if (types.contains("tourist_attraction")) {

                score += 2;
                companionPoint += 2;
            }
        }

        else if ("친구".equals(companion)) {

            if (types.contains("amusement_park")) {

                score += 2;
                companionPoint += 2;
            }

            if (types.contains("stadium")) {

                score += 2;
                companionPoint += 2;
            }
        }

        else if ("가족".equals(companion)) {

            if (types.contains("park")
                    || types.contains("zoo")
                    || types.contains("aquarium")) {

                score += 2;
                companionPoint += 2;
            }
        }

        else if ("혼자".equals(companion)) {

            if (types.contains("museum")
                    || types.contains("art_gallery")) {

                score += 2;
                companionPoint += 2;
            }


        }

        else if ("단체".equals(companion)) {

            if (types.contains("amusement_park")) {

                score += 2;
                companionPoint += 2;
            }

            if (types.contains("park")) {

                score += 2;
                companionPoint += 2;
            }

            if (types.contains("stadium")) {

                score += 2;
                companionPoint += 2;
            }

            if (types.contains("tourist_attraction")) {

                score += 1;
                companionPoint += 1;
            }
        }

        //연령대 점수

        if ("10대".equals(ageGroup)) {

            if (types.contains("amusement_park")) {

                score += 2;
                agePoint += 2;
            }

            if (types.contains("shopping_mall")) {

                score += 2;
                agePoint += 2;
            }
        }

        else if ("20대".equals(ageGroup)) {

            if (types.contains("cafe")) {

                score += 2;
                agePoint += 2;
            }

            if (types.contains("tourist_attraction")) {

                score += 2;
                agePoint += 2;
            }
        }

        else if ("30대".equals(ageGroup)) {

            if (types.contains("restaurant")) {

                score += 2;
                agePoint += 2;
            }

            if (types.contains("tourist_attraction")) {

                score += 1;
                agePoint += 1;
            }
        }

        else if ("40대".equals(ageGroup)) {

            if (types.contains("park")) {

                score += 2;
                agePoint += 2;
            }

            if (types.contains("museum")) {

                score += 2;
                agePoint += 2;
            }
        }

        else if ("50대 이상".equals(ageGroup)) {

            if (types.contains("museum")) {

                score += 2;
                agePoint += 2;
            }

            if (types.contains("park")) {

                score += 2;
                agePoint += 2;
            }
        }

        // 예산 점수

        int priceLevel = 0;

        try {

            priceLevel =
                    Integer.parseInt(
                            place.get(
                                    "price_level"
                            )
                    );

        } catch (Exception e) {

            priceLevel = 0;
        }

        if ("1만원 이하".equals(budget)) {

            if (priceLevel <= 1) {

                score += 3;
                budgetPoint += 3;
            }
        }

        else if ("1-3만원".equals(budget)) {

            if (priceLevel <= 2) {

                score += 3;
                budgetPoint += 3;
            }
        }

        else if ("3-5만원".equals(budget)) {

            if (priceLevel == 2
                    || priceLevel == 3) {

                score += 3;
                budgetPoint += 3;
            }
        }

        else if ("5-10만원".equals(budget)) {

            if (priceLevel >= 3) {

                score += 3;
                budgetPoint += 3;
            }
        }

        else if ("10만원 이상".equals(budget)) {

            if (priceLevel == 4) {

                score += 3;
                budgetPoint += 3;
            }
        }
        Log.d(
                "SCORE_DETAIL",

                "테마점수: "
                        + themePoint

                        + " / 평점점수: "
                        + ratingPoint

                        + " / 예산점수: "
                        + budgetPoint

                        + " / 동행자점수: "
                        + companionPoint

                        + " / 연령대점수: "
                        + agePoint

                        + " / 총점: "
                        + score
        );

        return score;
    }

    public double calculateCongestionScore(
            String congestionLevel
    ) {

        if (congestionLevel == null) {

            return 0;
        }

        if (congestionLevel.equals("여유")) {

            return 2;
        }

        else if (
                congestionLevel.equals("보통")
        ) {

            return 1;
        }

        else if (
                congestionLevel.equals("약간 붐빔")
        ) {

            return -1;
        }

        else if (
                congestionLevel.equals("붐빔")
        ) {

            return -3;
        }

        return 0;
    }
}
