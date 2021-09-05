package io.esper.files.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.rajat.pdfviewer.PdfViewerActivity
import io.esper.files.R
import io.esper.files.activity.VideoViewerActivity
import io.esper.files.fragment.ListItemsFragment
import io.esper.files.model.VideoURL
import org.greenrobot.eventbus.EventBus
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.ArrayList

class VideoURLAdapter(
        private var ctx: Context?,
        private var mItemVideoList: MutableList<VideoURL>,
        private var path: String?
) : RecyclerView.Adapter<VideoURLAdapter.MyViewHolder>(), Filterable {

    private var prevCharLength: Int = 0
    private var mContext: Context? = null
    private var dirPath: String? = null
    private val inflater: LayoutInflater = LayoutInflater.from(ctx)
    private var mItemListFilteredDialog: MutableList<VideoURL>? = ArrayList()
    private var mItemListOriginalDialog: MutableList<VideoURL>? = ArrayList()
    private var mItemPrevListDialog: MutableList<VideoURL>? = ArrayList()
    private var mItemReadyForPrevDialog: MutableList<VideoURL>? = ArrayList()

    override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
    ): MyViewHolder {
        mContext = ctx
        dirPath = path
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

        when {
            mItemVideoList[position].url!!.endsWith("pdf", false) -> {
                Glide.with(mContext!!)
                        .load(R.drawable.pdf)
                        .placeholder(R.drawable.video).priority(Priority.HIGH).into(holder.imgThumbnail)
            }
            mItemVideoList[position].url!!.contains("youtube") -> Glide.with(mContext!!)
                    .load("http://img.youtube.com/vi/" + getVideoId(mItemVideoList[position].url) + "/0.jpg")
                    .placeholder(R.drawable.video).priority(Priority.HIGH).into(holder.imgThumbnail)
            else -> Glide.with(mContext!!).load(mItemVideoList[position].url).placeholder(R.drawable.video)
                    .priority(Priority.HIGH).into(holder.imgThumbnail)
        }
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
                when {
                    url.text.toString().endsWith("pdf", false) -> {
                        try {
                            mContext!!.startActivity(
                                    PdfViewerActivity.launchPdfFromUrl(
                                            mContext,
                                            url.text.toString(),
                                            name.text.toString(),
                                            path,
                                            enableDownload = false
                                    )
                            )
                        } catch (e: Exception) {
                            Toast.makeText(mContext, "Sorry, Couldn't open up PDF!", Toast.LENGTH_SHORT).show()
                        }

                    }
                    getVideoId(url.text.toString())!!.isEmpty() -> {
                        val intent = Intent(mContext, VideoViewerActivity::class.java)
                        intent.putExtra("videoPath", url.text.toString())
                        intent.putExtra("isYT", false)
                        mContext!!.startActivity(intent)
                    }
                    else -> {
                        val intent = Intent(mContext, VideoViewerActivity::class.java)
                        intent.putExtra("videoPath", getVideoId(url.text.toString()))
                        intent.putExtra("isYT", true)
                        mContext!!.startActivity(intent)
                    }
                }
            }
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): FilterResults {
                if (charSequence.toString().isEmpty() || charSequence.toString() == "")
                    mItemVideoList = mItemListOriginalDialog!!
                else if (charSequence.toString().length < prevCharLength)
                    mItemVideoList = mItemPrevListDialog!!
                val filteredList: MutableList<VideoURL> = ArrayList()
                for (row in mItemVideoList) {
                    if (row.name!!.toLowerCase(Locale.getDefault())
                                    .contains(
                                            charSequence.toString().toLowerCase(Locale.getDefault())
                                    ) || row.url!!
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
                mItemPrevListDialog =
                        if (mItemListFilteredDialog!!.size <= mItemPrevListDialog!!.size)
                            mItemReadyForPrevDialog
                        else
                            mItemListFilteredDialog

                mItemReadyForPrevDialog = mItemPrevListDialog
                mItemListFilteredDialog = filterResults.values as MutableList<VideoURL>?
                EventBus.getDefault()
                        .post(ListItemsFragment.NewUpdatedVideoMutableList(mItemListFilteredDialog!!))
                mItemVideoList = mItemListFilteredDialog!!

                notifyDataSetChanged()
            }
        }
    }
}