package com.example.autoquest


import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offers")
data class OfferEntity(
    @PrimaryKey val offerId: String,
    val brand: String?,
    val model: String?,
    val generation: String?,
    val price: String?,
    val year: String?,
    val description: String?,
    val enginePower: String?,
    val fuelConsumption: String?,
    val ownerId: String?,
    val ownerPhoneNumber: String?
)