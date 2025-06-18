package com.example.autoquest

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.autoquest.databinding.ActivityMyOffersBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MyOffersActivity() : AppCompatActivity() {
    private val currentUser: FirebaseUser? = FirebaseAuth.getInstance().currentUser
    private var binding: ActivityMyOffersBinding? = null
    private var userOffers: MutableList<Offer>? = null
    private var gridAdapter: GridAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyOffersBinding.inflate(layoutInflater)
        setContentView(binding!!.getRoot())

        // Инициализация RecyclerView
        binding!!.gridOffers.setLayoutManager(GridLayoutManager(this, 2))

        userOffers = ArrayList()
        gridAdapter = GridAdapter(this, userOffers as ArrayList<Offer>)
        binding!!.gridOffers.setAdapter(gridAdapter)

        if (currentUser != null) {
            val userId: String = currentUser.uid
            val offersRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("offers")

            offersRef.orderByChild("ownerId").equalTo(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    @SuppressLint("NotifyDataSetChanged")
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        (userOffers as ArrayList<Offer>).clear()
                        for (offerSnapshot: DataSnapshot in dataSnapshot.getChildren()) {
                            val offer: Offer? = offerSnapshot.getValue(Offer::class.java)
                            if (offer != null) {
                                offer.offerId = offerSnapshot.key
                                (userOffers as ArrayList<Offer>).add(offer)
                            }

                        }
                        gridAdapter!!.notifyDataSetChanged() // Обновляем адаптер
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Log.e(
                            "MyOffersActivity",
                            "Error loading offers",
                            databaseError.toException()
                        )
                    }
                })
        }

        binding!!.returnButton.setOnClickListener(View.OnClickListener { v: View? -> finish() }) // Просто закрываем активити
    }
}
