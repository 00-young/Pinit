package com.example.pinit.model;

public class UserPreference {

    private String ageGroup;

    private String companion;

    private String theme;

    private String budgetType;

    public UserPreference(){
    }

    public UserPreference(
            String ageGroup,
            String companion,
            String theme,
            String budgetType
    ) {

        this.ageGroup = ageGroup;

        this.companion = companion;

        this.theme = theme;

        this.budgetType = budgetType;
    }

    public String getAgeGroup() {
        return ageGroup;
    }

    public String getCompanion() {
        return companion;
    }

    public String getTheme() {
        return theme;
    }

    public String getBudgetType() {
        return budgetType;
    }
}