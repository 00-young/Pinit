package com.example.pinit.model.post

import com.example.pinit.model.map.MapData

data class ContentBlock(

    val blockId: String = "",

    val type: String = "",

    val textContent: String = "",

    val imageUrl: String = "",

    val mapData: MapData? = null,

    val sortOrder: Int = 0
)