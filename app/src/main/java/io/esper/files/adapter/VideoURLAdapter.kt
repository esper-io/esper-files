package io.esper.files.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.GlideDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import io.esper.files.R
import io.esper.files.fragment.ListItemsFragment
import io.esper.files.model.VideoURL
import io.esper.files.util.BottomSheetDialog
import org.greenrobot.eventbus.EventBus
import java.util.regex.Pattern


class VideoURLAdapter(
    ctx: Context?,
    private var mItemVideoList: MutableList<VideoURL>
) :
    RecyclerView.Adapter<VideoURLAdapter.MyViewHolder>() {

    private var mContext: Context? = null
    private val inflater: LayoutInflater = LayoutInflater.from(ctx)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        mContext = parent.context
        val view: View = inflater.inflate(R.layout.item_file, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: MyViewHolder,
        position: Int
    ) {
        holder.name.text = mItemVideoList[position].name
        holder.url.text = mItemVideoList[position].url

        val ytthumb = "http://img.youtube.com/vi/" + getVideoId(mItemVideoList[position].url) + "/0.jpg"
        Log.d("Tag", ytthumb)
        Glide.with(mContext).load(ytthumb).listener(object :
            RequestListener<String?, GlideDrawable?> {
            override fun onException(
                e: Exception?,
                model: String?,
                target: Target<GlideDrawable?>?,
                isFirstResource: Boolean
            ): Boolean {
                holder.imgThumbnail.setImageResource(R.drawable.yt)
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
                //Toast.makeText(mContext, name.text, Toast.LENGTH_LONG).show()
//                mContext!!.startActivity(
//                    Intent(
//                        Intent.ACTION_VIEW,
//                        Uri.parse(url.text.toString())
//                    )
//                )
                //EventBus.getDefault().post(MainActivity.YTItemClick(getVideoId(url.text.toString())!!))

                EventBus.getDefault().post(ListItemsFragment.YTVideo(getVideoId(url.text.toString())!!))
                ListItemsFragment.dialog!!.dismiss()
            }
        }
    }

}