package com.example.thryftapp

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration
import java.util.Date
//PLEASE NOTE THAT WE LEFT THIS AS IS AND BUILD UPON THIS PROJECT TO NOW USER FIRESTORE AND FIRRBASE AUTH
@Database(
    entities = [User::class, Transaction::class, Category::class], //define database tables
    version = 7, //current database version
    exportSchema = false
)
@TypeConverters(Converters::class) //enable converters
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao //dao for user
    abstract fun transactionDao(): TransactionDao //dao for transactions
    abstract fun categoryDao(): CategoryDao //dao for categories

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null //singleton instance

        // Migration from version 1 to 2
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // This is just a placeholder example
                database.execSQL("ALTER TABLE transactions ADD COLUMN new_column INTEGER DEFAULT 0") //add new column
            }
        }

        //Migration from version 2 to 3 (adding minBudget and maxBudget)
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE categories ADD COLUMN minBudget REAL NOT NULL DEFAULT 0.0") //add minBudget column
                database.execSQL("ALTER TABLE categories ADD COLUMN maxBudget REAL NOT NULL DEFAULT 0.0") //add maxBudget column
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) { //ensure thread safety
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "thryft_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3) //add migrations
                    .fallbackToDestructiveMigration() // Only use this in dev!
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) } //convert long to date
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time //convert date to long
    }
}
