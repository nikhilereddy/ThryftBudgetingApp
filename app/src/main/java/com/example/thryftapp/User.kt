package com.example.thryftapp

import androidx.room.Entity
import androidx.room.PrimaryKey

//define user entity with room
@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String, // <-- use Firebase UID as ID
    val name: String, //user full name
    val email: String, //user email
    val password: String, //user password
    val createdAt: String //timestamp when user was created
)
