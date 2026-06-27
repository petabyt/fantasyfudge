package dev.danielc.common

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import dev.danielc.fudge.AndroidRuntime
import kotlinx.coroutines.flow.Flow

@Suppress("ArrayInDataClass")
@Entity(tableName = "saved_devices")
data class SavedDeviceEntity(
    @PrimaryKey val uniqueIdentifier: String,
    val lastSeenTimestamp: Long = System.currentTimeMillis(),
    val name: String,
    val manifestName: String,
    val targetIndex: Int,
    val setupOption: String? = null,
    val bluetoothMacAddress: String? = null,
    val privateData: ByteArray? = null,
)

@Dao
interface DeviceDao {
    @Query("SELECT * FROM saved_devices ORDER BY lastSeenTimestamp DESC")
    fun getAllDevices(): Flow<List<SavedDeviceEntity>>

    @Query("SELECT * FROM saved_devices ORDER BY lastSeenTimestamp DESC")
    fun getAllDevicesList(): List<SavedDeviceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDevice(device: SavedDeviceEntity)

    @Delete
    suspend fun deleteDevice(device: SavedDeviceEntity)
}

@Entity(tableName = "app_settings")
data class AppSettingEntity(
    @PrimaryKey val id: Int = 1,
    val downloadsLocation: String = AndroidRuntime.getDefaultDownloadDirectory(),
)

@Dao
interface SettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(config: AppSettingEntity)
    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun get(): AppSettingEntity?
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun getFlow(): Flow<AppSettingEntity?>
}

@Database(entities = [SavedDeviceEntity::class, AppSettingEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
    abstract fun settingsDao(): SettingsDao
}