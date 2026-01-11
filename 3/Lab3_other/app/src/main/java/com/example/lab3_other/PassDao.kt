package com.example.lab3_other

import androidx.room.*


@Dao
interface PassDao {


    @Query("SELECT * FROM entries")
    fun getAll(): List<PasswordEntry>


    @Insert
    fun insert(entry: PasswordEntry)


    @Update
    fun update(entry: PasswordEntry)


    @Delete
    fun delete(entry: PasswordEntry)
}