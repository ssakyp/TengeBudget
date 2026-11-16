package com.tenge.budget.data.local

import androidx.room.TypeConverter
import java.time.LocalDate

/** Room can't store [LocalDate] natively; it is persisted as an epoch-day number. (Enums are handled by Room itself.) */
class Converters {
    @TypeConverter
    fun fromEpochDay(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun toEpochDay(date: LocalDate?): Long? = date?.toEpochDay()
}
