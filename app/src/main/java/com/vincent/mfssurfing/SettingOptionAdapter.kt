package com.vincent.mfssurfing

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*

class SettingOptionAdapter (
    private val context: Context,
    private val groupViews: List<SettingOptionView>,
    private val childTexts: List<List<String>>) : BaseExpandableListAdapter() {

    override fun getGroup(groupPosition: Int): Any {
        return groupViews.get(groupPosition)
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
        return true
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    override fun getGroupView(groupPosition: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup?): View {
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

        val optionView = groupViews.get(groupPosition)
        txtTitle.text = optionView.title
        txtDescription.text = optionView.description

        widgetView.removeAllViews()
        widgetView.addView(optionView.view)

        return view
    }

    override fun getChildrenCount(groupPosition: Int): Int {
        return childTexts.get(groupPosition).size
    }

    override fun getChild(groupPosition: Int, childPosition: Int): Any {
        return childTexts.get(groupPosition).get(childPosition)
    }

    override fun getGroupId(groupPosition: Int): Long {
        return groupPosition.toLong()
    }

    override fun getChildView(
        groupPosition: Int,
        childPosition: Int,
        isLastChild: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        val view: View

        if (convertView == null) {
            val inflater = LayoutInflater.from(context)
            view = inflater.inflate(R.layout.list_child_setting, null)
        } else {
            view = convertView
        }

        val txtAdId = view.findViewById<TextView>(R.id.txtAdId)
        txtAdId.text = childTexts.get(groupPosition).get(childPosition)

        if (childPosition == 0) {
            val layout = view.findViewById<RelativeLayout>(R.id.relativeLayout)
            layout.setOnClickListener {
                Toast.makeText(context, "Add", Toast.LENGTH_SHORT).show()
            }
        } else {
            val imgDelete = view.findViewById<ImageView>(R.id.imgDelete)
            imgDelete.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            imgDelete.setOnClickListener {
                Toast.makeText(context, "Delete", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long {
        return childPosition.toLong()
    }

    override fun getGroupCount(): Int {
        return groupViews.size
    }


}