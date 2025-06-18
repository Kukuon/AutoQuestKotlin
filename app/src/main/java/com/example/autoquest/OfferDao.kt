package com.example.autoquest

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface OfferDao {
    @Query("SELECT * FROM offers")
    fun getAllOffers(): List<OfferEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOffers(offers: List<OfferEntity>)

    @Query("DELETE FROM offers")
    fun deleteAllOffers()
}
