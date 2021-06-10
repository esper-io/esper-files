@file:Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "DEPRECATION")

package io.esper.files.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.GlideDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.perfomer.blitz.getTimeAgo
import io.esper.files.R
import io.esper.files.fragment.ListItemsFragment
import io.esper.files.model.Item
import org.greenrobot.eventbus.EventBus
import java.text.DateFormat
import java.util.*
import kotlin.collections.ArrayList

class ItemAdapter(
    private var mItemList: MutableList<Item>,
    private val clickListener: ClickListener
) : SelectableAdapter<ItemAdapter.ItemViewHolder?>(), Filterable {

    private var prevCharLength: Int = 0
    private var mContext: Context? = null
    private var isActionModeEnabled = false
    private var mItemListFiltered: MutableList<Item>? = ArrayList()
    private var mItemListOriginal: MutableList<Item>? = ArrayList()
    private var mItemPrevList: MutableList<Item>? = ArrayList()
    private var mItemReadyForPrev: MutableList<Item>? = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        mContext = parent.context
        val view: View = LayoutInflater.from(mContext).inflate(R.layout.item_file, parent, false)
        mItemListOriginal = mItemList
        return ItemViewHolder(view, clickListener)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val currentItem = mItemList[position]
        when {
            currentItem.isDirectory -> {
                holder.imgThumbnail.setImageResource(R.drawable.folder)
                //To Hide sub-folders
                //holder.background.visibility = GONE
            }
            currentItem.name!!.endsWith(".apk", ignoreCase = true) -> {
                holder.imgThumbnail.setImageResource(R.drawable.apk)
            }
            currentItem.name!!.endsWith(".zip", ignoreCase = true) || currentItem.name!!.endsWith(
                ".rar",
                ignoreCase = true
            ) -> {
                holder.imgThumbnail.setImageResource(R.drawable.zip)
            }
            currentItem.name!!.endsWith(".pdf", ignoreCase = true) -> {
                holder.imgThumbnail.setImageResource(R.drawable.pdf)
            }
            currentItem.name!!.endsWith(".xls", ignoreCase = true) || currentItem.name!!.endsWith(
                ".xlsx",
                ignoreCase = true
            ) || currentItem.name!!.endsWith(".csv", ignoreCase = true) -> {
                holder.imgThumbnail.setImageResource(R.drawable.xls)
            }
            currentItem.name!!.endsWith(".ppt", ignoreCase = true) || currentItem.name!!.endsWith(
                ".pptx",
                ignoreCase = true
            ) -> {
                holder.imgThumbnail.setImageResource(R.drawable.ppt)
            }
            currentItem.name!!.endsWith(".doc", ignoreCase = true) || currentItem.name!!.endsWith(
                ".docx",
                ignoreCase = true
            ) -> {
                holder.imgThumbnail.setImageResource(R.drawable.doc)
            }
            currentItem.name!!.endsWith(".vcf", ignoreCase = true) -> {
                holder.imgThumbnail.setImageResource(R.drawable.vcf)
            }
            currentItem.name!!.endsWith(".json", ignoreCase = true) -> {
                holder.imgThumbnail.setImageResource(R.drawable.json)
            }
            else -> {
                Glide.with(mContext).load(currentItem.path).listener(object :
                    RequestListener<String?, GlideDrawable?> {
                    override fun onException(
                        e: Exception?,
                        model: String?,
                        target: Target<GlideDrawable?>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        holder.imgThumbnail.setImageResource(R.drawable.file)
                        return true
                    }

                    override fun onResourceReady(
                        resource: GlideDrawable?,
                        model: String?,
                        target: Target<GlideDrawable?>,
                        isFromMemoryCache: Boolean,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }
                }).centerCrop().into(holder.imgThumbnail)
            }
        }
        holder.txtTitle.text = currentItem.name
        val f = DateFormat.getDateTimeInstance()
        val d: Date = f.parse(currentItem.date)
        val milliseconds: Long = d.time
//        try {
//            if(currentItem.isDirectory)
//                holder.txtItems.setTimeAgo(
//                    time = milliseconds,
//                    showSeconds = true
//                )
//            else {
//                val contextString: String = mContext!!.getTimeAgo(time = milliseconds, showSeconds = false)
//                holder.txtItems.text = currentItem.data + ", " + contextString
//            }
//        } catch (e: ParseException) {
//            e.printStackTrace()
//        }
        val timeString: String = mContext!!.getTimeAgo(time = milliseconds, showSeconds = false)
        holder.txtItems.text = currentItem.data + ", " + timeString
        holder.background.isSelected = isSelected(position)
    }

    override fun getItemCount(): Int {
        return mItemList.size
    }

    inner class ItemViewHolder(itemView: View, private val listener: ClickListener?) :
        RecyclerView.ViewHolder(
            itemView
        ), View.OnClickListener, OnLongClickListener {
        var txtTitle: TextView
        var txtItems: TextView
        var imgThumbnail: ImageView
        var background: RelativeLayout
        override fun onClick(v: View) {
            if (listener != null) {
                listener.onItemClicked(adapterPosition)
                if (isActionModeEnabled) {
                    background.isSelected = !isSelected(adapterPosition)
                }
            }
        }

        override fun onLongClick(v: View): Boolean {
            return listener?.onItemLongClicked(adapterPosition) ?: false
        }

        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
            background = itemView.findViewById<View>(R.id.item_background) as RelativeLayout
            imgThumbnail = itemView.findViewById<View>(R.id.img_item_thumbnail) as ImageView
            txtTitle = itemView.findViewById<View>(R.id.txt_item_name) as TextView
            txtItems = itemView.findViewById<View>(R.id.txt_item_info) as TextView
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): FilterResults {
                if (charSequence.toString().isEmpty() || charSequence.toString() == "")
                    mItemList = mItemListOriginal!!
                else if (charSequence.toString().length < prevCharLength)
                    mItemList = mItemPrevList!!
                val filteredList: MutableList<Item> = ArrayList()
                for (row in mItemList) {
                    if (row.name!!.toLowerCase(Locale.getDefault())
                            .contains(
                                charSequence.toString().toLowerCase(Locale.getDefault())
                            ) || row.data!!
                            .contains(charSequence.toString().toLowerCase(Locale.getDefault()))
                    )
                        filteredList.add(row)
                }
                mItemListFiltered = filteredList

                prevCharLength = charSequence.toString().length
                val filterResults = FilterResults()
                filterResults.values = mItemListFiltered
                return filterResults
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(
                charSequence: CharSequence?,
                filterResults: FilterResults
            ) {
                mItemPrevList = if (mItemListFiltered!!.size <= mItemPrevList!!.size)
                    mItemReadyForPrev
                else
                    mItemListFiltered

                mItemReadyForPrev = mItemPrevList
                mItemListFiltered = filterResults.values as MutableList<Item>
                EventBus.getDefault()
                    .post(ListItemsFragment.NewUpdatedMutableList(mItemListFiltered!!))
                mItemList = mItemListFiltered!!

                notifyDataSetChanged()
            }
        }
    }

    fun setActionModeEnabled(isEnabled: Boolean) {
        isActionModeEnabled = isEnabled
    }

    interface ClickListener {
        fun onItemClicked(position: Int)
        fun onItemLongClicked(position: Int): Boolean
    }
}