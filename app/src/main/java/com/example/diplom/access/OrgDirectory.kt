package com.example.diplom.access

object OrgDirectory {
    val evacPoints: List<String> = listOf(
        "Эвакопункт №1",
        "Эвакопункт №2",
        "Эвакопункт №3",
        "Эвакопункт №4"
    )

    val hospitals: List<String> = listOf(
        "Госпиталь №1",
        "Госпиталь №2",
        "Госпиталь №3",
        "Госпиталь №4"
    )

    private val evacToHospitals: Map<String, List<String>> = mapOf(
        "Эвакопункт №1" to listOf("Госпиталь №1", "Госпиталь №2"),
        "Эвакопункт №2" to listOf("Госпиталь №2", "Госпиталь №3"),
        "Эвакопункт №3" to listOf("Госпиталь №3", "Госпиталь №4"),
        "Эвакопункт №4" to listOf("Госпиталь №1", "Госпиталь №4")
    )

    fun hospitalsForEvacPoint(evacPoint: String?): List<String> {
        val key = evacPoint?.trim().orEmpty()
        return evacToHospitals[key] ?: hospitals
    }
}
