package com.example.autoquest

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.autoquest.databinding.FragmentFavoritesBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class FavoritesFragment : Fragment() {
    var binding: FragmentFavoritesBinding? = null

    private val firebaseUser = FirebaseAuth.getInstance().currentUser

    private val databaseReferenceOffers = FirebaseDatabase.getInstance().getReference("offers")
    private var databaseReferenceUser: DatabaseReference? = null
    private var gridAdapter: GridAdapter? = null

    private val favoriteOfferList: MutableList<Offer> = ArrayList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFavoritesBinding.inflate(layoutInflater)
        gridAdapter = GridAdapter(activity, ArrayList())
        binding!!.recyclerView.layoutManager = GridLayoutManager(context, 2)
        binding!!.recyclerView.adapter = gridAdapter

        if (firebaseUser != null) {
            databaseReferenceUser = FirebaseDatabase.getInstance().getReference("users")
                .child(firebaseUser.uid)
                .child("favorites_offers")
            loadFavoriteOffers()
// gfg
            return binding!!.root
        } else {
            val rootView = inflater.inflate(R.layout.fragment_unlogged, container, false)
            val goToLoginButton = rootView.findViewById<Button>(R.id.goToLoginButton)

            goToLoginButton.setOnClickListener { v: View? ->
                startActivity(
                    Intent(
                        context, LoginActivity::class.java
                    )
                )
            }

            return rootView
        }
    }


    private fun loadFavoriteOffers() {
        databaseReferenceUser!!.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val favoriteOfferIds: MutableList<String> = ArrayList()

                for (snapshot in dataSnapshot.children) {
                    val offerId =
                        snapshot.getValue(String::class.java) // Получаем значение, а не ключ
                    if (offerId != null) {
                        favoriteOfferIds.add(offerId)
                    }
                }
                fetchOffers(favoriteOfferIds)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("Firebase", "Failed to read value.", databaseError.toException())
            }
        })
    }


    private fun fetchOffers(offerIds: List<String>) {
        favoriteOfferList.clear()
        if (offerIds.isEmpty()) {
            gridAdapter!!.updateOffers(favoriteOfferList)
            return
        }

        val loadedCount = intArrayOf(0)
        for (offerId in offerIds) {
            databaseReferenceOffers.child(offerId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val offer = snapshot.getValue(Offer::class.java)
                        if (offer != null) {
                            offer.offerId = snapshot.key
                            favoriteOfferList.add(offer)
                        }

                        loadedCount[0]++
                        if (loadedCount[0] == offerIds.size) {
                            gridAdapter!!.updateOffers(favoriteOfferList)
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        loadedCount[0]++
                        if (loadedCount[0] == offerIds.size) {
                            gridAdapter!!.updateOffers(favoriteOfferList)
                        }
                        Log.e("Firebase", "Failed to read value.", databaseError.toException())
                    }
                })
        }
    }
}