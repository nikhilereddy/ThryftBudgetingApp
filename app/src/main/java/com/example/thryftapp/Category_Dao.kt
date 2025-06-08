/*
package com.example.thryftapp

import androidx.room.Dao import androidx.room.Insert import androidx.room.Query

@Dao interface CategoryDao {

    @Insert
    fun insertCategory(category: Category) //insert a new category

    @Query("SELECT * FROM categories WHERE userId = :userId")
    fun getAllCategories(userId: Int): List<Category> //get all categories for a user

    @Query("SELECT * FROM categories WHERE userId = :userId AND type = :type")
    fun getCategoriesByType(userId: Int, type: String): List<Category> //get categories filtered by type

    @Query("SELECT * FROM categories WHERE userId = :userId AND name = :name LIMIT 1")
    fun getCategoryByName(userId: Int, name: String): Category? //get a category by name

    @Query("SELECT * FROM categories WHERE id = :categoryId")
    fun getCategoryById(categoryId: Int): Category? //get a category by id

    @Query("SELECT name FROM categories WHERE id = :categoryId LIMIT 1")
    fun getCategoryNameById(categoryId: Int): String? //get category name by id

}
*/
package com.example.thryftapp

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface CategoryDao {

    @Insert
    fun insertCategory(category: Category) //insert a new category

    @Query("SELECT * FROM categories WHERE userId = :userId")
    fun getAllCategories(userId: String): List<Category> //get all categories for a user

    @Query("SELECT * FROM categories WHERE userId = :userId AND type = :type")
    fun getCategoriesByType(userId: String, type: String): List<Category> //get categories filtered by type

    @Query("SELECT * FROM categories WHERE userId = :userId AND name = :name LIMIT 1")
    fun getCategoryByName(userId: String, name: String): Category? //get a category by name

    @Query("SELECT * FROM categories WHERE id = :categoryId")
    fun getCategoryById(categoryId: Int): Category? //get a category by id

    @Query("SELECT name FROM categories WHERE id = :categoryId LIMIT 1")
    fun getCategoryNameById(categoryId: Int): String? //get category name by id
}
