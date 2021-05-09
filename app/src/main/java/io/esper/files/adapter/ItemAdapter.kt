package io.esper.files.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.GlideDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import io.esper.files.R
import io.esper.files.model.Item
import java.lang.Exception

class ItemAdapter(private val mItemList: MutableList<Item>, private val clickListener: ClickListener) : SelectableAdapter<ItemAdapter.ItemViewHolder?>() {
    private var mContext: Context? = null
    private var isActionModeEnabled = false
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        mContext = parent.context
        val view: View = LayoutInflater.from(mContext).inflate(R.layout.item_file, parent, false)
        return ItemViewHolder(view, clickListener)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val currentItem = mItemList[position]
        if(currentItem.name!!.startsWith(".", ignoreCase = true))
        {
            holder.background.visibility = GONE
        }
        when {
            currentItem.isDirectory -> {
                holder.imgThumbnail.setImageResource(R.drawable.folder)
                //To Hide sub-folders
                //holder.background.visibility = GONE
            }
            currentItem.name!!.endsWith(".apk", ignoreCase = true) -> {
                holder.imgThumbnail.setImageResource(R.drawable.apk)
            }
            currentItem.name!!.endsWith(".zip", ignoreCase = true) || currentItem.name!!.endsWith(".rar", ignoreCase = true)-> {
                holder.imgThumbnail.setImageResource(R.drawable.zip)
            }
            currentItem.name!!.endsWith(".pdf", ignoreCase = true) -> {
                holder.imgThumbnail.setImageResource(R.drawable.pdf)
            }
            currentItem.name!!.endsWith(".xls", ignoreCase = true)||currentItem.name!!.endsWith(".xlsx", ignoreCase = true)||currentItem.name!!.endsWith(".csv", ignoreCase = true) -> {
                holder.imgThumbnail.setImageResource(R.drawable.xls)
            }
            currentItem.name!!.endsWith(".ppt", ignoreCase = true)||currentItem.name!!.endsWith(".pptx", ignoreCase = true) -> {
                holder.imgThumbnail.setImageResource(R.drawable.ppt)
            }
            currentItem.name!!.endsWith(".doc", ignoreCase = true)||currentItem.name!!.endsWith(".docx", ignoreCase = true) -> {
                holder.imgThumbnail.setImageResource(R.drawable.doc)
            }
            else -> {
                Glide.with(mContext).load(currentItem.path).listener(object : RequestListener<String?, GlideDrawable?> {
                    override fun onException(
                            e: Exception?,
                            model: String?,
                            target: Target<GlideDrawable?>?,
                            isFirstResource: Boolean
                    ): Boolean {
                        holder.imgThumbnail.setImageResource(R.drawable.file)
                        return true
                    }

                    override fun onResourceReady(resource: GlideDrawable?, model: String?, target: Target<GlideDrawable?>, isFromMemoryCache: Boolean, isFirstResource: Boolean): Boolean {
                        return false
                    }
                }).into(holder.imgThumbnail)
            }
        }
        holder.txtTitle.text = currentItem.name
        holder.txtItems.text = currentItem.data
        holder.background.isSelected = isSelected(position)
    }

    override fun getItemCount(): Int {
        return mItemList.size
    }

    inner class ItemViewHolder(itemView: View, private val listener: ClickListener?) : RecyclerView.ViewHolder(itemView), View.OnClickListener, OnLongClickListener {
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

    fun setActionModeEnabled(isEnabled: Boolean) {
        isActionModeEnabled = isEnabled
    }

    interface ClickListener {
        fun onItemClicked(position: Int)
        fun onItemLongClicked(position: Int): Boolean
    }
}