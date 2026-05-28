package com.example.pinit.service;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

// [API 인터페이스] 서울 열린데이터광장 도시 실시간 혼잡도 API
// Retrofit이 이 인터페이스를 구현체로 자동 생성
// 엔드포인트: http://openapi.seoul.go.kr:8088/{API_KEY}/json/citydata/1/5/{AREA}
public interface SeoulApiService {
    // AREA 예: "강남역", "홍대" 등 서울시 지정 주요 지역명
    @GET("{API_KEY}/json/citydata/1/5/{AREA}")
    Call<Object> getCongestionData(
            @Path("API_KEY") String apiKey,
            @Path("AREA") String area
    );
}
