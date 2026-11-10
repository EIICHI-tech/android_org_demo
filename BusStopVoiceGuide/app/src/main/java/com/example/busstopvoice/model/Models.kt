package com.example.busstopvoice.model

data class BusRoute(
    val destination: String,
    val aliases: List<String>,
    val platform: Int,
    val departureTime: String,
    val busName: String,
    val durationMinutes: Int,
    val fareYen: Int
)

enum class ConversationMode { WAITING_WAKE, WAITING_DESTINATION, SHOWING_RESULT }

data class GuideUiState(
    val status: String = "起動準備中",
    val heardText: String = "",
    val message: String = "『HI BASUTEI』と話してください",
    val mode: ConversationMode = ConversationMode.WAITING_WAKE,
    val route: BusRoute? = null,
    val departureLabel: String = "",
    val minutesUntil: Int? = null,
    val micReady: Boolean = false,
    val error: String? = null
)
