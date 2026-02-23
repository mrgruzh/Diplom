package com.example.diplom.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "medical_records",
    indices = [
        Index(value = ["soldierId"]),
        Index(value = ["status"]),
        Index(value = ["destination"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = SoldierEntity::class,
            parentColumns = ["id"],
            childColumns = ["soldierId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MedicalRecordEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,              // Внутренний ID записи

    val soldierId: Long,            // Ссылка на SoldierEntity.id

    val status: String,             // "RANEN" / "POGIB" (или потом сделаем enum)

    val location: String?,          // Местоположение: координаты, описание точки и т.п.

    val injuryType: String?,        // Вид травмы (огнестрельное, осколочное, ожог и т.д.)

    val medicine: String?,          // Введённый препарат / первая помощь

    val destination: String?,       // Дальнейшее распределение: название эвакопункта / госпиталя

    val stage: String,              // На каком этапе создано: "FIELD", "EVAC", "HOSPITAL"

    val rawText: String?,           // Исходный текст диктовки / описания (если был голосовой ввод)

    val createdAt: Long = System.currentTimeMillis()   // Время создания записи (для фильтрации по датам)
)
