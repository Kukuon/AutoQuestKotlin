package com.example.autoquest

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView

class AvatarAdapter(private val mContext: Context, private val mBitmaps: List<Bitmap>) :
    BaseAdapter() {
    override fun getCount(): Int {
        return mBitmaps.size
    }

    override fun getItem(position: Int): Any {
        return mBitmaps[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View, parent: ViewGroup): View {
        val imageView: ImageView

        if (convertView == null) {
            val inflater =
                mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            imageView = ImageView(mContext)
            imageView.layoutParams =
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    250
                ) // Установите размеры изображения
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        } else {
            imageView = convertView as ImageView
        }

        imageView.setImageBitmap(mBitmaps[position])
        return imageView
    }
}
