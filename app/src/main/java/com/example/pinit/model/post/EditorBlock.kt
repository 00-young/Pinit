package com.example.pinit.model.post

import android.net.Uri
import com.example.pinit.model.map.MapData

/**
 * 작성 화면(에디터)에서 블록의 편집 상태를 들고 있는 모델.
 * 저장 시 ContentBlock.from() 으로 변환된다.
 * type: "text" | "image" | "place" | "map"
 */
data class EditorBlock(

    val id: String,

    var type: String,

    // text 블록 본문 / image 캡션
    var text: String = "",

    // image 블록
    var localImageUri: Uri? = null,
    var imageUrl: String = "",

    // place 블록 (장소 헤더)
    var placeName: String = "",
    var placeAddress: String = "",

    // map 블록 (지도 + 요약 리스트)
    var mapData: MapData? = null,
    var date: String = "",
    var dayTitle: String = "",

    // budget 블록 (지출 예산, 만원 단위)
    var budgetFood: Int = 0,
    var budgetTransport: Int = 0,
    var budgetAccom: Int = 0,
    var budgetShopping: Int = 0,
    var budgetSightseeing: Int = 0,
    var budgetEtc: Int = 0,

    var sortOrder: Int = 0
)