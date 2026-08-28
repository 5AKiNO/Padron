package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "voters",
    indices = [
        Index(value = ["cedula"]),
        Index(value = ["fullName"])
    ]
)
data class Voter(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cedula: String,
    val fullName: String,
    val votingPlace: String = "",
    val tableNumber: String = "",
    val orderNumber: String = "",
    val address: String = "",
    val cityOrZone: String = "",
    val voted: Boolean = false,
    val phone: String = "",
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
