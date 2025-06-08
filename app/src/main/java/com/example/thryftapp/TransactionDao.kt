package com.example.thryftapp

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TransactionDao {

    @Insert
    fun insertTransaction(transaction: Transaction) //insert new transaction

    @Query("SELECT * FROM transactions WHERE userId = :userId")
    fun getAllTransactions(userId: String): List<Transaction> //get all user transactions

    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId AND userId = :userId ORDER BY date DESC")
    fun getTransactionsByCategory(categoryId: Int, userId: String): List<Transaction> //filter by category

    @Query("SELECT * FROM transactions WHERE date BETWEEN :fromDate AND :toDate AND userId = :userId")
    fun getTransactionsBetweenDates(fromDate: Long, toDate: Long, userId: String): List<Transaction> //filter by date range for user

    @Delete
    fun deleteTransaction(transaction: Transaction) //delete transaction
}
