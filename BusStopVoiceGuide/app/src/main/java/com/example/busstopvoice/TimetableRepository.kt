package com.example.busstopvoice

import android.content.Context
import com.example.busstopvoice.model.BusRoute
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class TimetableRepository(context: Context) {
    private val routes: List<BusRoute> = context.assets.open("timetable.json").bufferedReader().use {
        Gson().fromJson(it, object : TypeToken<List<BusRoute>>() {}.type)
    }

    fun destinations(): List<String> = routes.map { it.destination }.distinct()

    fun findDestination(spoken: String): String? {
        val cleaned = normalize(spoken)
        return routes.firstOrNull { route ->
            (route.aliases + route.destination).any { normalize(cleaned).contains(normalize(it)) }
        }?.destination
    }

    fun nextRoute(destination: String, offset: Int = 0, now: LocalDateTime = LocalDateTime.now()): Pair<BusRoute, LocalDateTime>? {
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val candidates = routes.filter { it.destination == destination }.map { route ->
            val time = LocalTime.parse(route.departureTime, formatter)
            var dateTime = LocalDateTime.of(LocalDate.now(), time)
            if (dateTime.isBefore(now.minusMinutes(1))) dateTime = dateTime.plusDays(1)
            route to dateTime
        }.sortedBy { it.second }
        return candidates.getOrNull(offset)
    }

    private fun normalize(value: String): String = value
        .lowercase()
        .replace(" ", "")
        .replace("　", "")
        .replace("jr", "")
        .replace("に行きたいです", "")
        .replace("に行きたい", "")
        .replace("まで行きたい", "")
        .replace("へ行きたい", "")
        .replace("を案内して", "")
        .replace("お願いします", "")
}
