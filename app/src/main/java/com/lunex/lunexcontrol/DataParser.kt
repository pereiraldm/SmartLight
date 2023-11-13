package com.lunex.lunexcontrol

fun parseData(data: String): Pair<String, String>? {
    val tempRegex = "T:([\\d.]+º)".toRegex()
    val humidRegex = "U:([\\d.]+%)".toRegex()

    val tempMatch = tempRegex.find(data)
    val humidMatch = humidRegex.find(data)

    if (tempMatch != null && humidMatch != null) {
        return Pair(tempMatch.groupValues[1], humidMatch.groupValues[1])
    }
    return null
}
