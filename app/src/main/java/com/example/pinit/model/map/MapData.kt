package com.example.pinit.model.map

data class MapData(

    val polyline: String = "",

    val markers: List<MapMarker> = emptyList()
)