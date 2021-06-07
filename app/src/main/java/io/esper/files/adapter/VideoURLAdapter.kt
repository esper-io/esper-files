package io.esper.files.adapter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.GlideDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import io.esper.files.R
import io.esper.files.fragment.ListItemsFragment
import io.esper.files.model.VideoURL
import org.greenrobot.eventbus.EventBus
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.ArrayList


class VideoURLAdapter(
        ctx: Context?,
        private var mItemVideoList: MutableList<VideoURL>
) :
        RecyclerView.Adapter<VideoURLAdapter.MyViewHolder>(), Filterable {

    private var prevCharLength: Int = 0
    private var mContext: Context? = null
    private val inflater: LayoutInflater = LayoutInflater.from(ctx)
    private var mItemListFilteredDialog: MutableList<VideoURL>? = ArrayList()
    private var mItemListOriginalDialog: MutableList<VideoURL>? = ArrayList()
    private var mItemPrevListDialog: MutableList<VideoURL>? = ArrayList()
    private var mItemReadyForPrevDialog: MutableList<VideoURL>? = ArrayList()

    override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
    ): MyViewHolder {
        mContext = parent.context
        val view: View = inflater.inflate(R.layout.item_file, parent, false)
        mItemListOriginalDialog = mItemVideoList
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(
            holder: MyViewHolder,
            position: Int
    ) {
        holder.name.text = mItemVideoList[position].name
        holder.url.text = mItemVideoList[position].url

        val ytthumb = "http://img.youtube.com/vi/" + getVideoId(mItemVideoList[position].url) + "/0.jpg"
        Glide.with(mContext).load(ytthumb).listener(object :
                RequestListener<String?, GlideDrawable?> {
            override fun onException(
                    e: Exception?,
                    model: String?,
                    target: Target<GlideDrawable?>?,
                    isFirstResource: Boolean
            ): Boolean {
                holder.imgThumbnail.setImageResource(R.drawable.video)
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
        }).into(holder.imgThumbnail)
    }

    override fun getItemCount(): Int {
        return mItemVideoList.size
    }

    private fun getVideoId(url: String?): String? {
        val idgroup = 6
        var videoId: String? = ""
        if (url != null && url.trim { it <= ' ' }.isNotEmpty()) {
            val expression =
                    "(http:|https:|)\\/\\/(player.|www.)?(vimeo\\.com|youtu(be\\" +
                            ".com|\\.be|be\\.googleapis\\.com))\\/(video\\/|embed\\/|watch\\?v=|v\\/)?" +
                            "([A-Za-z0-9._%-]*)(\\&\\S+)?"
            val pattern = Pattern.compile(
                    expression,
                    Pattern.CASE_INSENSITIVE
            )
            val matcher = pattern.matcher(url)
            if (matcher.find()) {
                val groupIndex = matcher.group(idgroup)
                if (groupIndex != null) {
                    videoId = groupIndex
                }
            }
        }
        return videoId
    }

    inner class MyViewHolder(itemView: View) :
            RecyclerView.ViewHolder(itemView) {
        var name: TextView = itemView.findViewById<View>(R.id.txt_item_name) as TextView
        var url: TextView = itemView.findViewById<View>(R.id.txt_item_info) as TextView
        var imgThumbnail = itemView.findViewById<View>(R.id.img_item_thumbnail) as ImageView

        init {
            itemView.setOnClickListener {
                if (getVideoId(url.text.toString())!!.isEmpty())
                    EventBus.getDefault().post(ListItemsFragment.NormalVideoFile(url.text.toString()))
                else
                    EventBus.getDefault().post(ListItemsFragment.YTVideoFile(getVideoId(url.text.toString())!!))
                ListItemsFragment.dialog!!.dismiss()
            }
        }
    }

    override fun getFilter(): Filter? {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): FilterResults? {
                if (charSequence.toString().isEmpty() || charSequence.toString() == "")
                    mItemVideoList = mItemListOriginalDialog!!
                else if (charSequence.toString().length < prevCharLength)
                    mItemVideoList = mItemPrevListDialog!!
                val filteredList: MutableList<VideoURL> = ArrayList()
                for (row in mItemVideoList) {
                    if (row.name!!.toLowerCase(Locale.getDefault())
                                    .contains(charSequence.toString().toLowerCase(Locale.getDefault())) || row.url!!
                                    .contains(charSequence.toString().toLowerCase(Locale.getDefault()))
                    )
                        filteredList.add(row)
                }
                mItemListFilteredDialog = filteredList

                prevCharLength = charSequence.toString().length
                val filterResults = FilterResults()
                filterResults.values = mItemListFilteredDialog
                return filterResults
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(
                    charSequence: CharSequence?,
                    filterResults: FilterResults
            ) {
                mItemPrevListDialog = if (mItemListFilteredDialog!!.size <= mItemPrevListDialog!!.size)
                    mItemReadyForPrevDialog
                else
                    mItemListFilteredDialog

                mItemReadyForPrevDialog = mItemPrevListDialog
                mItemListFilteredDialog = filterResults.values as MutableList<VideoURL>?
                EventBus.getDefault().post(ListItemsFragment.NewUpdatedVideoMutableList(mItemListFilteredDialog!!))
                mItemVideoList = mItemListFilteredDialog!!

                notifyDataSetChanged()
            }
        }
    }

}