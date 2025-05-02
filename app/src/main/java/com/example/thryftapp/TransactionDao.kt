package com.example.thryftapp

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import java.util.Date

@Dao
interface TransactionDao {

    @Insert
    fun insertTransaction(transaction: Transaction)

    @Query("SELECT * FROM transactions WHERE userId = :userId")
    fun getAllTransactions(userId: Int): List<Transaction>

    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId AND userId = :userId ORDER BY date DESC")
    fun getTransactionsByCategory(categoryId: Int, userId: Int): List<Transaction>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :fromDate AND :toDate")
    fun getTransactionsBetweenDates(fromDate: Long, toDate: Long): List<Transaction>
    @Delete
     fun deleteTransaction(transaction: Transaction)

}