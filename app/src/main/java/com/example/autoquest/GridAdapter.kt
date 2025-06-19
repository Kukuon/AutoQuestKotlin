package com.example.autoquest

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ListResult

// адаптер для элемента грида
class GridAdapter(private val context: Context?, private var offerList: MutableList<Offer>) :
    RecyclerView.Adapter<GridAdapter.ViewHolder>() {
    fun clear() {
        offerList.clear()
        notifyDataSetChanged()
    }

    fun addItem(item: Offer) {
        offerList.add(item)
        notifyDataSetChanged()
    }

    fun setItems(items: MutableList<Offer>) {
        offerList = items
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_grid, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val offer = offerList[position]


        holder.textViewTitle.text = offer.brand + " " + offer.model + " " + offer.generation
        holder.textViewPrice.text = offer.price + " P"
        holder.textViewYear.text = offer.year

        // Загрузка изображения из Firebase Storage
        val storageReference =
            FirebaseStorage.getInstance().getReference("uploads/offer_images/" + offer.offerId)
        storageReference.listAll().addOnSuccessListener { listResult: ListResult ->
            if (listResult.items.isNotEmpty()) {
                val firstImageRef = listResult.items[0]
                firstImageRef.downloadUrl.addOnSuccessListener { url: Uri? ->
                    Glide.with(
                        context!!
                    )
                        .load(url)
                        .placeholder(R.drawable.image_svg)
                        .error(R.drawable.image_svg)
                        .centerCrop()
                        .into(holder.imageView)
                }.addOnFailureListener {
                    holder.imageView.setImageResource(R.drawable.image_svg)
                }
            } else {
                holder.imageView.setImageResource(R.drawable.image_svg)
            }
        }.addOnFailureListener {
            holder.imageView.setImageResource(R.drawable.image_svg)
        }

        holder.itemView.setOnClickListener {
            val selectedOffer = offerList[position]
            val intent = Intent(context, OfferActivity::class.java)
            intent.putExtra("offerId", selectedOffer.offerId)
            context!!.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return offerList.size
    }

    // обновление грида офферов
    fun updateOffers(offers: List<Offer>?) {
        offerList.clear()
        offerList.addAll(offers!!)
        notifyDataSetChanged()
    }


    // привязка макета к объектам
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var textViewTitle: TextView = itemView.findViewById(R.id.titleTV)
        var textViewPrice: TextView = itemView.findViewById(R.id.priceTV)
        var textViewYear: TextView = itemView.findViewById(R.id.yearTV)
        var imageView: ImageView = itemView.findViewById(R.id.imageView)
    }
}