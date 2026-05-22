package com.example.pinit.service;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

// [서비스] 서울 실시간 도시 혼잡도 조회 서비스
// Retrofit으로 서울 열린데이터광장 API를 호출하고 결과를 CongestionData로 파싱
// PlaceAdapter에서 장소별 혼잡도 배지 표시에 사용
public class CongestionService {

    private static final String BASE_URL = "http://openapi.seoul.go.kr:8088/"; // 서울 공공데이터 API 서버
    private static final String API_KEY = com.example.pinit.BuildConfig.SEOUL_API_KEY;

    private final SeoulApiService api;

    public CongestionService() {
        // Retrofit 클라이언트 초기화 (Gson으로 JSON → Map<String,Object> 자동 변환)
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        api = retrofit.create(SeoulApiService.class);
    }

    // area에 해당하는 지역 혼잡도를 비동기로 조회
    // 응답 구조: CITYDATA → LIVE_PPLTN_STTS[0] → AREA_CONGEST_LVL, AREA_CONGEST_MSG, FCST_PPLTN
    public void getCongestionData(String area, CongestionCallback callback) {
        api.getCongestionData(API_KEY, area).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                try {
                    Map<String, Object> result = (Map<String, Object>) response.body();
                    Map<String, Object> cityData = (Map<String, Object>) result.get("CITYDATA");
                    List<Object> peopleList = (List<Object>) cityData.get("LIVE_PPLTN_STTS");
                    Map<String, Object> peopleData = (Map<String, Object>) peopleList.get(0);

                    String areaName = peopleData.get("AREA_NM").toString();
                    String congestionLevel = peopleData.get("AREA_CONGEST_LVL").toString();
                    String congestionMessage = peopleData.get("AREA_CONGEST_MSG").toString();

                    // 현재 붐빔/매우붐빔일 때: 예보 목록에서 '보통' 또는 '여유'가 되는 첫 시각 추출
                    List<Object> forecastList = (List<Object>) peopleData.get("FCST_PPLTN");
                    String forecastTime = "현재 쾌적";
                    String forecastLevel = congestionLevel;

                    if (!congestionLevel.equals("여유") && !congestionLevel.equals("보통")) {
                        for (Object obj : forecastList) {
                            Map<String, Object> forecastData = (Map<String, Object>) obj;
                            String level = forecastData.get("FCST_CONGEST_LVL").toString();
                            if (level.equals("보통") || level.equals("여유")) {
                                forecastTime = forecastData.get("FCST_TIME").toString();
                                forecastLevel = level;
                                break;
                            }
                        }
                    }

                    callback.onSuccess(new CongestionData(
                            areaName, congestionLevel, congestionMessage, forecastTime, forecastLevel));
                } catch (Exception e) {
                    callback.onFailure(e.getMessage());
                }
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                callback.onFailure(t.getMessage());
            }
        });
    }
}
