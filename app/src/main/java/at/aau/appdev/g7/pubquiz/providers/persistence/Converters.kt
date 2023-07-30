package at.aau.appdev.g7.pubquiz.providers.persistence

import androidx.room.TypeConverter
import java.util.UUID

class Converters {
    @TypeConverter
    fun fromUUID(uuid: UUID?) : String? {
        return uuid?.toString()
    }

    @TypeConverter
    fun toUUID(value: String?) : UUID? {
        return if (value != null) UUID.fromString(value) else null
    }
}