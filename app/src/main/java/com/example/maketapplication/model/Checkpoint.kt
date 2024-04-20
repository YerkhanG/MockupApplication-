package com.example.maketapplication.model

import com.yandex.mapkit.geometry.Point

data class Checkpoint (
    val id : Int,
    val name : String,
    val imageResource: String,
    val point : Point,
    var isPassed : Boolean = false ,
)