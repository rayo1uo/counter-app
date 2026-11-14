package com.example.counter.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "counters")
data class CounterEntity(
    @PrimaryKey val id: String,
    val name: String,
    val color: Long,
    val icon: String = "",
    val step: Double = 1.0,
    val initialValue: Double = 0.0,
    val target: Double = 0.0,
    val category: String = "默认",
    val sortOrder: Int = 0,
    val archived: Boolean = false,
    val createdAt: Long = 0L
)

@Entity(
    tableName = "entries",
    foreignKeys = [
        ForeignKey(
            entity = CounterEntity::class,
            parentColumns = ["id"],
            childColumns = ["counterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["counterId", "localDate"]),
        Index(value = ["occurredAt"])
    ]
)
data class EntryEntity(
    @PrimaryKey val id: String,
    val counterId: String,
    val delta: Double,
    val valueAfter: Double = 0.0,
    val occurredAt: Long,
    val localDate: String,
    val tzOffsetSeconds: Int = 0,
    val note: String = "",
    val createdAt: Long = 0L
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val name: String,
    val sortOrder: Int = 0,
    val color: Long = 0xFF9B9B9B
)
