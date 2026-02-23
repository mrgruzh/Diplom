package com.example.diplom.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "soldiers",
    indices = [
        Index(
            value = ["tagNumber"],
            unique = true
        )
    ]
)
data class SoldierEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,          // Внутренний ID в базе

    val fullName: String,       // ФИО

    val callsign: String?,      // Позывной (может быть неизвестен)

    val division: String?,      // Дивизия / подразделение

    val gender: String?,        // Пол: "М", "Ж" или другое значение

    val ageYears: Int?,         // Возраст, если известен (в годах)

    val weightKg: Float?,       // Вес в кг, если известен

    val tagNumber: String?      // Номер жетона (может быть null, но если есть — уникален)
)
