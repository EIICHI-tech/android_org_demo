package com.ashida.carclimatevoice

data class ClimateState(
    val temperature: Float = 24f,
    val fanLevel: Int = 3,
    val powerOn: Boolean = true,
    val autoOn: Boolean = true,
    val acOn: Boolean = true,
    val recirculationOn: Boolean = false,
    val frontDefrostOn: Boolean = false,
    val rearDefrostOn: Boolean = false,
    val response: String = "マイクを押して話してください",
    val lastCommand: String = ""
)
