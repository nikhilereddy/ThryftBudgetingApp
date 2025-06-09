package com.example.thryftapp

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories", //table name for categories
    foreignKeys = [
        ForeignKey(
            entity = User::class, //relation to user table
            parentColumns = ["id"], //parent column in user
            childColumns = ["userId"], //foreign key in this table
            onDelete = ForeignKey.CASCADE //delete categories if user is deleted
        )
    ]
)
data class Category(
    @PrimaryKey
    val id: String,
    val userId: String, //foreign key to user
    val name: String, //category name
    val type: String, // "INCOME" or "EXPENSE"
    val minBudget: Double, //minimum budget limit
    val maxBudget: Double, //maximum budget limit
    val iconId: String // e.g. "ic_baseline_food_bank_24"
)
//done