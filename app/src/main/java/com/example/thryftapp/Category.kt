package com.example.thryftapp

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int,
    val name: String,
    val type: String, // "INCOME" or "EXPENSE"
    val minBudget: Double,
    val maxBudget: Double,
    val iconId: String // e.g. "ic_baseline_food_bank_24"
)
