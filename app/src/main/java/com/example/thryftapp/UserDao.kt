package com.example.thryftapp

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

//dao for user table
@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.ABORT) //insert new user, abort if email exists
    fun insertUser(user: User)

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1") //get user by email
    fun getUserByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE email = :email AND password = :password LIMIT 1") //login using email and password
    fun loginUser(email: String, password: String): User?

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1") //get user by id
    fun getUserById(userId: Int): User?
}
