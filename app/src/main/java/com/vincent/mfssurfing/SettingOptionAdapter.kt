package com.vincent.mfssurfing

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.apache.commons.lang3.StringUtils

class SettingOptionAdapter (
    private val context: Context,
    private val groupViews: List<SettingOptionView>,
    private val childTexts: List<MutableList<String>>) : BaseExpandableListAdapter() {

    override fun getGroup(groupPosition: Int): Any {
        return groupViews[groupPosition]
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
        return true
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    override fun getGroupView(groupPosition: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_setting, null)

        val txtTitle = view.findViewById<TextView>(R.id.txtTitle)
        val txtDescription = view.findViewById<TextView>(R.id.txtDescription)
        val widgetView = view.findViewById<FrameLayout>(R.id.frameLayout)

        val optionView = groupViews[groupPosition]
        txtTitle.text = optionView.title
        txtDescription.text = optionView.description

        widgetView.removeAllViews()
        widgetView.addView(optionView.view)

        return view
    }

    override fun getChildrenCount(groupPosition: Int): Int {
        return childTexts[groupPosition].size
    }

    override fun getChild(groupPosition: Int, childPosition: Int): Any {
        return childTexts[groupPosition][childPosition]
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

        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_child_setting, null)

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
        displayAddingSection(view, false)

        val txtAdId = view.findViewById<TextView>(R.id.txtAdId)
        val edtAdId = view.findViewById<EditText>(R.id.edtAdId)
        val imgAction = view.findViewById<ImageView>(R.id.imgAction)

        txtAdId.text = childTexts[groupPosition][childPosition]
        edtAdId.text = null
        imgAction.setBackgroundResource(R.drawable.icon_add)

        val layout = view.findViewById<RelativeLayout>(R.id.relativeLayout)
        layout.setOnClickListener {
            displayAddingSection(view, true)

            imgAction.setOnClickListener {
                val adNumber = edtAdId.text.toString()

                if (StringUtils.isNotEmpty(adNumber)) {
                    PreferencesHelper(context).addIgnoredAdNumber(adNumber)
                    childTexts[2].removeAt(0)
                    childTexts[2].add(adNumber)
                    childTexts[2].sort()
                    childTexts[2].add(0, context.getString(R.string.label_add_ad))
                    notifyDataSetChanged()
                    displayAddingSection(view, false)
                    Toast.makeText(context, context.getString(R.string.ignored), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, context.getString(R.string.hint_enter_ad_id), Toast.LENGTH_SHORT).show()
                }
            }
        }

        return view
    }

    private fun getIgnoredAdsChildView(view: View, groupPosition: Int, childPosition: Int): View {
        val txtAdId = view.findViewById<TextView>(R.id.txtAdId)
        val imgAction = view.findViewById<ImageView>(R.id.imgAction)
        val adNumber = childTexts[groupPosition][childPosition]

        txtAdId.visibility = View.VISIBLE
        txtAdId.text = childTexts[groupPosition][childPosition]

        imgAction.visibility = View.VISIBLE
        imgAction.setBackgroundResource(R.drawable.icon_delete)

        imgAction.setOnClickListener {
            AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.title_ignore_ad))
                .setMessage("${context.getString(R.string.recover_ad)}$adNumber${context.getString(R.string.will_be_browse)}")
                .setPositiveButton(context.getString(R.string.yes)
                ) { dialog, which ->
                    PreferencesHelper(context).deleteIgnoredAdNumber(adNumber)
                    childTexts[2].remove(adNumber)
                    notifyDataSetChanged()
                    Toast.makeText(context, context.getString(R.string.recovered), Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(context.getString(R.string.no), null)
                .show()
        }

        return view
    }

    private fun displayAddingSection(addingSectionView: View, show: Boolean) {
        val txtAdId = addingSectionView.findViewById<TextView>(R.id.txtAdId)
        val edtAdId = addingSectionView.findViewById<EditText>(R.id.edtAdId)
        val imgAction = addingSectionView.findViewById<ImageView>(R.id.imgAction)

        if (show) {
            txtAdId.visibility = View.GONE
            edtAdId.visibility = View.VISIBLE
            imgAction.visibility = View.VISIBLE
            edtAdId.requestFocus()
        } else {
            txtAdId.visibility = View.VISIBLE
            edtAdId.visibility = View.GONE
            imgAction.visibility = View.GONE
        }
    }

}