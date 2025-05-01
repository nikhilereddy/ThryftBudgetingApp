package com.example.thryftapp

import androidx.room.Dao import androidx.room.Insert import androidx.room.Query

@Dao interface CategoryDao {

    @Insert
    fun insertCategory(category: Category)

    @Query("SELECT * FROM categories WHERE userId = :userId")
    fun getAllCategories(userId: Int): List<Category>

    @Query("SELECT * FROM categories WHERE userId = :userId AND type = :type")
    fun getCategoriesByType(userId: Int, type: String): List<Category>

    @Query("SELECT * FROM categories WHERE userId = :userId AND name = :name LIMIT 1")
    fun getCategoryByName(userId: Int, name: String): Category?

    @Query("SELECT * FROM categories WHERE id = :categoryId")
    fun getCategoryById(categoryId: Int): Category?

}

