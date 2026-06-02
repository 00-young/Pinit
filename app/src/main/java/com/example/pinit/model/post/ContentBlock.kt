package com.example.pinit.model.post

import com.example.pinit.model.map.MapData

/**
 * 타입: text | image | place | map
 * 이미지는 Storage 에만 올리고 imageUrl(다운로드 URL)만 저장.
 *
 * [DAY 가 펼쳐지는 형태]
 *   map(지도+요약리스트+date) → place(장소헤더) → text/image(자유) → place → ...
 *
 *   하루 동선/장소/날짜는 map 블록의 mapData + date 에 모두 모여 있어,
 *   "내 일정에 담기" 시 map 블록 하나만 읽어 DailySchedule 로 복원하면 된다.
 */
data class ContentBlock(

    val blockId: String = "",

    val type: String = "",

    val textContent: String = "",       // text 블록 본문 / image 캡션

    val imageUrl: String = "",          // image 블록: Storage 다운로드 URL

    // place 블록 (장소 헤더)
    val placeName: String = "",
    val placeAddress: String = "",

    // map 블록 (지도 + 요약 리스트). 하루 동선/핀이 markers 에 모두 들어있다.
    val mapData: MapData? = null,
    val date: String = "",              // map 블록의 날짜 (담기 시 사용)
    val dayTitle: String = "",          // map 블록의 "DAY 1" 등

    val sortOrder: Int = 0
) {
    companion object {
        const val TYPE_TEXT = "text"
        const val TYPE_IMAGE = "image"
        const val TYPE_PLACE = "place"
        const val TYPE_MAP = "map"

        /** 작성용 EditorBlock → 저장용 ContentBlock 변환 */
        fun from(editor: EditorBlock): ContentBlock = ContentBlock(
            blockId = editor.id,
            type = editor.type,
            textContent = editor.text,
            imageUrl = editor.imageUrl,
            placeName = editor.placeName,
            placeAddress = editor.placeAddress,
            mapData = editor.mapData,
            date = editor.date,
            dayTitle = editor.dayTitle,
            sortOrder = editor.sortOrder
        )
    }
}
