package com.example.thryftapp

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE //delete user = delete transactions
        ),
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL //delete category = null categoryId
        )
    ],
    indices = [
        Index(value = ["userId"]), //index for userId
        Index(value = ["categoryId"]) //index for categoryId
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0, //primary key
    val userId: String, //linked user
    val categoryId: String?, //linked category
    val amount: Double, //amount of transaction
    val type: String, // "INCOME" or "EXPENSE"
    val description: String?, //optional note
    val photoUri: String?, //store uri of uploaded photo
    val date: Date, //transaction date
    val createdAt: Date //record creation timestamp
)
