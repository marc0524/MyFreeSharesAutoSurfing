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
        if (groupPosition != 2) {
            return convertView!!
        }

        val view: View
        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.list_child_setting, null)
        } else {
            view = convertView
        }

        if (childPosition == 0) {
            return getAddAdChildView(view, groupPosition, childPosition)
        }

        return getIgnoredAdsChildView(view, groupPosition, childPosition)
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long {
        return childPosition.toLong()
    }

    override fun getGroupCount(): Int {
        return groupViews.size
    }

    private fun getAddAdChildView(view: View, groupPosition: Int, childPosition: Int): View {
        val txtAdId = view.findViewById<TextView>(R.id.txtAdId)
        val edtAdId = view.findViewById<EditText>(R.id.edtAdId)
        val imgAction = view.findViewById<ImageView>(R.id.imgAction)

        txtAdId.visibility = View.VISIBLE
        txtAdId.text = childTexts.get(groupPosition).get(childPosition)

        edtAdId.visibility = View.GONE
        edtAdId.text = null

        imgAction.visibility = View.GONE
        imgAction.setBackgroundResource(R.drawable.icon_add)

        val layout = view.findViewById<RelativeLayout>(R.id.relativeLayout)
        layout.setOnClickListener {
            txtAdId.visibility = View.GONE
            edtAdId.visibility = View.VISIBLE
            imgAction.visibility = View.VISIBLE
            edtAdId.requestFocus()

            imgAction.setOnClickListener {
                Toast.makeText(context, "Add", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun getIgnoredAdsChildView(view: View, groupPosition: Int, childPosition: Int): View {
        val txtAdId = view.findViewById<TextView>(R.id.txtAdId)
        val imgAction = view.findViewById<ImageView>(R.id.imgAction)

        txtAdId.visibility = View.VISIBLE
        txtAdId.text = childTexts.get(groupPosition).get(childPosition)

        imgAction.visibility = View.VISIBLE
        imgAction.setBackgroundResource(R.drawable.icon_delete)

        imgAction.setOnClickListener {
            Toast.makeText(context, "Delete", Toast.LENGTH_SHORT).show()
        }

        return view
    }

}