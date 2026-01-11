package com.example.lab3_other

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


@Database(entities = [PasswordEntry::class], version = 1)
abstract class PassDatabase : RoomDatabase() {
    abstract fun passDao(): PassDao


    companion object {
        @Volatile private var instance: PassDatabase? = null


        fun getDatabase(context: Context): PassDatabase {
            return instance ?: synchronized(this) {
                val inst = Room.databaseBuilder(
                    context.applicationContext,
                    PassDatabase::class.java,
                    "passwords.db"
                ).allowMainThreadQueries().build()
                instance = inst
                inst
            }
        }
    }
}