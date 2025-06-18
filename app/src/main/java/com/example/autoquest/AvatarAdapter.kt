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
        // колво элементов списка
    override fun getCount(): Int {
        return mBitmaps.size
    }

    // позиция объяекта в списке
    override fun getItem(position: Int): Any {
        return mBitmaps[position]
    }
// id элемента
    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    // получение view
    override fun getView(position: Int, convertView: View, parent: ViewGroup): View {
        val imageView: ImageView

        if (convertView == null) {
            // если нет уже view, то делается новый
            val inflater =
                mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            imageView = ImageView(mContext)
            // размеры картинки 250
            imageView.layoutParams =
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    250
                ) // установка скейла
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        } else {
            imageView = convertView as ImageView
        }
    // установка картинки в imageView
        imageView.setImageBitmap(mBitmaps[position])
        return imageView
    }
}
