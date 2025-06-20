package com.example.autoquest

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import java.util.Locale

class SearchFragment : Fragment() {
    private val databaseReference: DatabaseReference =
        FirebaseDatabase.getInstance().getReference("offers")
    private var gridAdapter: GridAdapter? = null
    private val offerList: MutableList<Offer> = ArrayList()
    private val filteredList: MutableList<Offer> = ArrayList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_search, container, false)

        val searchView: SearchView = view.findViewById(R.id.searchView)
        val recyclerView: RecyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.setLayoutManager(GridLayoutManager(context, 2))

        // Инициализируем адаптер с пустым списком
        gridAdapter = GridAdapter(context, ArrayList())
        recyclerView.setAdapter(gridAdapter)

        loadOffersFromFirebase()

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                Log.d(TAG, "Query submitted: $query")
                filterOffers(query)
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                Log.d(TAG, "Query changed: $newText")
                filterOffers(newText)
                return true
            }
        })

        return view
    }

    // загрузка оффером из бд
    private fun loadOffersFromFirebase() {
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                offerList.clear()
                for (snapshot: DataSnapshot in dataSnapshot.getChildren()) {
                    val offer: Offer? = snapshot.getValue<Offer>()
                    if (offer != null) {
                        offer.offerId = snapshot.key;
                        offerList.add(offer)
                        Log.d(TAG, "Loaded offer: " + offer.brand + " " + offer.model)
                    }
                }
                Log.d(TAG, "Total offers loaded: " + offerList.size)
                // При первой загрузке показываем все офферы
                filteredList.clear()
                filteredList.addAll(offerList)
                updateAdapter()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e(TAG, "Failed to load offers", databaseError.toException())
                Toast.makeText(context, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show()
            }
        })
    }


    // поиск по названию офферов
    private fun filterOffers(query: String?) {
        Log.d(TAG, "Filtering with query: $query")

        filteredList.clear()

        if (query == null || query.trim { it <= ' ' }.isEmpty()) {
            filteredList.addAll(offerList)
        } else {
            val lowerCaseQuery: String = query.lowercase(Locale.getDefault()).trim { it <= ' ' }
            for (offer: Offer in offerList) {
                val matches: Boolean = ((offer.brand) != null && offer.brand!!.lowercase(
                    Locale.getDefault()
                ).contains(lowerCaseQuery)) ||
                        (offer.model != null && offer.model!!.lowercase(Locale.getDefault())
                            .contains(lowerCaseQuery)) ||
                        (offer.generation != null && offer.generation!!
                            .lowercase(Locale.getDefault()).contains(lowerCaseQuery))

                if (matches) {
                    filteredList.add(offer)
                }
            }
        }

        Log.d(TAG, "Filtered results: " + filteredList.size)
        updateAdapter()
    }

    // обновление данных адаптера
    private fun updateAdapter() {
        if (gridAdapter != null) {
            gridAdapter!!.updateOffers(filteredList)
        } else {
            Log.e(TAG, "GridAdapter is null!")
        }
    }

    companion object {
        private const val TAG: String = "SearchFragment"
    }
}