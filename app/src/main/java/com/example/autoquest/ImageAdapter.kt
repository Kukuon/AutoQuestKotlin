package com.example.autoquest

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.autoquest.ImageAdapter.ImageViewHolder
import com.google.firebase.storage.FirebaseStorage


// адаптер для картинкок оффера
class ImageAdapter(private val context: Context, private val imageUrls: List<String>?) :
    RecyclerView.Adapter<ImageViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageUrl = imageUrls!![position]
        ImageLoadTask(imageUrl, holder.imageView).execute()
    }

    override fun getItemCount(): Int {
        return imageUrls!!.size
    }

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var imageView: ImageView = itemView.findViewById(R.id.imageView)
    }

    private class ImageLoadTask(private val url: String, private val imageView: ImageView) :
        AsyncTask<Void?, Void?, Bitmap?>() {

        override fun doInBackground(vararg params: Void?): Bitmap? {
            return try {
                val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(url)
                val ONE_MEGABYTE = (1024 * 1024).toLong()

                var bitmap: Bitmap? = null
                val task = storageReference.getBytes(ONE_MEGABYTE)

                val bytes = com.google.android.gms.tasks.Tasks.await(task)
                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                bitmap
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        override fun onPostExecute(bitmap: Bitmap?) {
            bitmap?.let {
                imageView.setImageBitmap(it)
            }
        }
    }
}
