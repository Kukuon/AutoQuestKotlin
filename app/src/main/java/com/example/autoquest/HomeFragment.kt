package com.example.autoquest

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.autoquest.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HomeFragment : Fragment() {
    private var binding: FragmentHomeBinding? = null

    // объекты firebase
    private val databaseReference = FirebaseDatabase.getInstance().getReference("offers")
    // объект авторизации текущего пользователя
    private val firebaseUser = FirebaseAuth.getInstance().currentUser

    // объект адаптера грида
    private var gridAdapter: GridAdapter? = null
    private val offerList: MutableList<Offer> = ArrayList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        gridAdapter = GridAdapter(activity, ArrayList())
        binding!!.gridOffers.layoutManager = GridLayoutManager(context, 2)
        binding!!.gridOffers.adapter = gridAdapter

        loadOffers()

        binding!!.fab.setOnClickListener {
            if (firebaseUser != null) {
                startActivity(Intent(context, CreateOfferActivity::class.java))
            } else {
                Toast.makeText(
                    context,
                    "Чтобы добавить объявление необходимо войти",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        return binding!!.root
    }


    // загрузка офферов из firebase
    private fun loadOffers() {
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (snapshot in dataSnapshot.children) {
                    val offer = snapshot.getValue(Offer::class.java)
                    if (offer != null) {
                        offer.offerId = snapshot.key // Используем ключ узла как ID
                        offerList.add(offer)
                    }
                }
                // передача офферов в адаптер
                gridAdapter!!.updateOffers(offerList)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("Firebase", "Failed to read value.", databaseError.toException())
            }
        })
    }
}
