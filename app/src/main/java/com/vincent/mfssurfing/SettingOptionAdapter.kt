package com.vincent.mfssurfing

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class SettingOptionAdapter(
    val context: Context,
    val optionViews: List<SettingOptionView>) : BaseAdapter() {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View

        if (convertView == null) {
            val inflater = LayoutInflater.from(context)
            view = inflater.inflate(R.layout.list_item_setting, null)
        } else {
            view = convertView
        }

        val txtTitle = view.findViewById<TextView>(R.id.txtTitle)
        val txtDescription = view.findViewById<TextView>(R.id.txtDescription)
        val widgetView = view.findViewById<FrameLayout>(R.id.frameLayout)

        val optionView = optionViews.get(position)
        txtTitle.text = optionView.title
        txtDescription.text = optionView.description
        widgetView.addView(optionView.view)

        return view
    }

    override fun getItem(position: Int): Any {
        return optionViews.get(position)
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun getCount(): Int {
        return optionViews.size
    }

}