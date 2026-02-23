package com.example.diplom.data

import android.content.Context
import androidx.room.*
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Entity(tableName = "persons")
data class PersonEntity(
    @PrimaryKey val id: String,
    val name: String
)

/** Визиты «привязаны» к personId. При удалении Person — его визиты удаляются каскадно. */
@Entity(
    tableName = "visits",
    indices = [Index("personId")],
    foreignKeys = [
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class VisitEntity(
    @PrimaryKey val id: String,
    val personId: String,
    val createdAt: Long,
    val note: String?
)

@Dao
interface PersonDao {
    @Query("SELECT * FROM persons ORDER BY name")
    fun observeAll(): kotlinx.coroutines.flow.Flow<List<PersonEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(vararg p: PersonEntity)

    @Query("DELETE FROM persons WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface VisitDao {
    @Query("SELECT * FROM visits WHERE personId = :pid ORDER BY createdAt DESC")
    fun observeByPerson(pid: String): kotlinx.coroutines.flow.Flow<List<VisitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(v: VisitEntity)

    @Query("DELETE FROM visits WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Database(
    entities = [
        PersonEntity::class,
        VisitEntity::class,
        SoldierEntity::class,
        MedicalRecordEntity::class
    ],
    version = 1
)
abstract class AppDb : RoomDatabase() {

    abstract fun personDao(): PersonDao
    abstract fun visitDao(): VisitDao

    abstract fun appDao(): AppDao
    companion object {
        @Volatile private var instance: AppDb? = null

        fun get(context: Context): AppDb =
            instance ?: synchronized(this) {
                // SQLCipher init
                SQLiteDatabase.loadLibs(context)
                val passphrase = com.example.diplom.sec.SecureStorage
                    .getOrCreateDbPassphrase(context)
                val factory = SupportFactory(passphrase)

                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDb::class.java,
                    "fieldmed.db"
                )
                    .openHelperFactory(factory) // шифрование
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
