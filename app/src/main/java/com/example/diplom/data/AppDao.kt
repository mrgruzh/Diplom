package com.example.diplom.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    // ---- SOLDIERS ----

    // Наблюдать список всех солдат (для списков/сводных таблиц)
    @Query("SELECT * FROM soldiers ORDER BY fullName")
    fun observeAllSoldiers(): Flow<List<SoldierEntity>>

    // Наблюдать одного солдата по id (для экрана карточки)
    @Query("SELECT * FROM soldiers WHERE id = :id LIMIT 1")
    fun observeSoldier(id: Long): Flow<SoldierEntity?>

    // Найти солдата по номеру жетона (для обновления при повторном сохранении)
    @Query("SELECT * FROM soldiers WHERE tagNumber = :tagNumber LIMIT 1")
    suspend fun getSoldierByTagNumber(tagNumber: String): SoldierEntity?

    // Сосчитать, сколько солдат в БД
    @Query("SELECT COUNT(*) FROM soldiers")
    suspend fun countSoldiers(): Int

    // Создать или обновить солдата
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSoldier(soldier: SoldierEntity): Long

    // Удалить солдата (медзаписи удалятся каскадно из-за ForeignKey)
    @Query("DELETE FROM soldiers WHERE id = :id")
    suspend fun deleteSoldier(id: Long)

    // ---- MEDICAL RECORDS ----

    // Все записи по одному солдату (история бойца)
    @Query(
        "SELECT * FROM medical_records " +
                "WHERE soldierId = :soldierId " +
                "ORDER BY createdAt DESC"
    )
    fun observeRecordsForSoldier(
        soldierId: Long
    ): Flow<List<MedicalRecordEntity>>

    // Записи для конкретного места назначения (эвакопункт/госпиталь) за диапазон дат
    @Query(
        "SELECT * FROM medical_records " +
                "WHERE destination = :destination " +
                "AND createdAt BETWEEN :fromTime AND :toTime " +
                "ORDER BY createdAt DESC"
    )
    fun observeRecordsForDestination(
        destination: String,
        fromTime: Long,
        toTime: Long
    ): Flow<List<MedicalRecordEntity>>

    // Создать/обновить медзапись
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: MedicalRecordEntity): Long

    // Все записи (для сводок/аналитики)
    @Query("SELECT * FROM medical_records ORDER BY createdAt DESC")
    suspend fun getAllRecords(): List<MedicalRecordEntity>

    // Удалить медзапись
    @Query("DELETE FROM medical_records WHERE id = :id")
    suspend fun deleteRecord(id: Long)
}
