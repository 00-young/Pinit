package com.example.pinit.service;

// [모델] 서울 열린데이터광장 API에서 파싱한 혼잡도 정보
// CongestionService가 생성해서 CongestionCallback.onSuccess로 전달
public class CongestionData {
    private String areaName;          // 지역명 (예: "강남역")
    private String congestionLevel;   // 현재 혼잡도: "여유" | "보통" | "붐빔" | "매우붐빔"
    private String congestionMessage; // 혼잡도 안내 메시지 (API 제공 문자열)
    private String forecastTime;      // 혼잡도가 완화될 예상 시각 (붐빔 이상일 때만 의미 있음), 또는 "현재 쾌적"
    private String forecastLevel;     // 예상 혼잡도 수준 ("보통" 또는 "여유")

    public CongestionData(String areaName, String congestionLevel, String congestionMessage,
                          String forecastTime, String forecastLevel) {
        this.areaName = areaName;
        this.congestionLevel = congestionLevel;
        this.congestionMessage = congestionMessage;
        this.forecastTime = forecastTime;
        this.forecastLevel = forecastLevel;
    }

    public String getAreaName() { return areaName; }
    public String getCongestionLevel() { return congestionLevel; }
    public String getCongestionMessage() { return congestionMessage; }
    public String getForecastTime() { return forecastTime; }
    public String getForecastLevel() { return forecastLevel; }
}
