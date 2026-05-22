package com.example.pinit.service;

// [콜백] 혼잡도 API 결과를 비동기로 받는 인터페이스
// CongestionService.getCongestionData() 호출 시 구현체를 넘겨서 결과 수신
public interface CongestionCallback {
    void onSuccess(CongestionData data); // 정상 응답: 파싱된 혼잡도 데이터 전달
    void onFailure(String error);        // 네트워크 오류 또는 JSON 파싱 실패 시 호출
}
