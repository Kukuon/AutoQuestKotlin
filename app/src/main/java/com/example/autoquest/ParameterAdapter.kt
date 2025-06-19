package com.example.autoquest

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView


//адаптер для отдельного параметра оффера
class ParameterAdapter(private val context: Context, private val parameters: List<Parameter>) :
    BaseAdapter() {
    override fun getCount(): Int {
        return parameters.size
    }

    override fun getItem(position: Int): Any {
        return parameters.get(position)
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.item_parameter, parent, false)
        }

        val parameter = parameters[position]
        val nameTextView: TextView = view!!.findViewById(R.id.parameterName)
        val valueTextView: TextView = view.findViewById(R.id.parameterValue)

        nameTextView.text = parameter.name
        valueTextView.text = parameter.value

        return view
    }

}


