@file:Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "DEPRECATION")

package io.esper.files.fragment

import android.annotation.TargetApi
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ferfalk.simplesearchview.SimpleSearchView
import com.ferfalk.simplesearchview.utils.DimensUtils
import com.rajat.pdfviewer.PdfViewerActivity
import io.esper.files.R
import io.esper.files.activity.ImageViewerActivity
import io.esper.files.activity.VideoViewerActivity
import io.esper.files.adapter.ItemAdapter
import io.esper.files.adapter.ItemAdapter.ClickListener
import io.esper.files.adapter.VideoURLAdapter
import io.esper.files.async.LoadFileAsync
import io.esper.files.callback.OnLoadDoneCallback
import io.esper.files.constants.Constants.EsperScreenshotFolder
import io.esper.files.constants.Constants.InternalCheckerString
import io.esper.files.constants.Constants.InternalScreenshotFolderDCIM
import io.esper.files.constants.Constants.InternalScreenshotFolderPictures
import io.esper.files.constants.Constants.ListItemsFragmentTag
import io.esper.files.constants.Constants.ORIGINAL_SCREENSHOT_STORAGE_PREF_KEY
import io.esper.files.constants.Constants.ORIGINAL_SCREENSHOT_STORAGE_VALUE
import io.esper.files.constants.Constants.SHARED_MANAGED_CONFIG_DELETION_ALLOWED
import io.esper.files.constants.Constants.SHARED_MANAGED_CONFIG_SHOW_SCREENSHOTS
import io.esper.files.constants.Constants.SHARED_MANAGED_CONFIG_VALUES
import io.esper.files.constants.Constants.imageFileFormats
import io.esper.files.constants.Constants.videoAudioFileFormats
import io.esper.files.model.Item
import io.esper.files.model.VideoURL
import io.esper.files.util.FileUtils
import io.esper.files.util.FileUtils.Decompress
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.nio.channels.FileChannel


class ListItemsFragment : Fragment(), ClickListener {

    private var mGridLayoutManager: GridLayoutManager? = null
    private var mRecyclerItems: RecyclerView? = null
    private var mItemAdapter: ItemAdapter? = null
    private var mItemList: MutableList<Item>? = null
    private var mEmptyView: LinearLayout? = null
    private var mCurrentPath: String? = null
    private var mActionMode: ActionMode? = null
    private val mActionModeCallback: ActionModeCallback = ActionModeCallback()
    private var mItemListFromJson: MutableList<VideoURL>? = ArrayList()
    private var mVideoItemAdapter: VideoURLAdapter? = null
    private var mRecyclerDialogItems: RecyclerView? = null
    private var mEmptyDialogView: RelativeLayout? = null
    private var sharedPrefStorage: SharedPreferences? = null
    private var sharedPref: SharedPreferences? = null
    private var dialog: Dialog? = null
    private lateinit var fragmentCallback: UpdateViewOnScroll

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mItemList = ArrayList()
        mCurrentPath = requireArguments().getString(KEY_CURRENT_PATH)!!
    }

    @Nullable
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        val itemsView: View = inflater.inflate(R.layout.fragment_items, container, false)
        mGridLayoutManager =
                if (requireActivity().resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
                    GridLayoutManager(context, 1)
                else
                    GridLayoutManager(context, 4)
        mRecyclerItems = itemsView.findViewById<View>(R.id.recycler_view_items) as RecyclerView
        mEmptyView = itemsView.findViewById<View>(R.id.layout_empty_view) as LinearLayout
        mRecyclerItems!!.layoutManager = mGridLayoutManager
        return itemsView
    }

    override fun onViewCreated(view: View, @Nullable savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (mCurrentPath!!.contains(InternalCheckerString)) {
            sharedPrefStorage = requireContext().getSharedPreferences(
                    ORIGINAL_SCREENSHOT_STORAGE_PREF_KEY,
                    Context.MODE_PRIVATE
            )
            sharedPref = requireContext().getSharedPreferences(
                    SHARED_MANAGED_CONFIG_VALUES,
                    Context.MODE_PRIVATE
            )
            if (sharedPref!!.getBoolean(SHARED_MANAGED_CONFIG_SHOW_SCREENSHOTS, false)) {
                if (loadDirectoryContents(InternalScreenshotFolderDCIM)) {
                    moveScreenshotDirectoryContents(InternalScreenshotFolderDCIM)
                } else if (loadDirectoryContents(InternalScreenshotFolderPictures)) {
                    moveScreenshotDirectoryContents(InternalScreenshotFolderPictures)
                }
            } else
                moveScreenshotDirectoryContentsBack(
                        sharedPrefStorage!!.getString(
                                ORIGINAL_SCREENSHOT_STORAGE_VALUE,
                                null
                        ).toString()
                )
        }

        mRecyclerItems!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0)
                    fragmentCallback.verticalScroll()
            }
        })

        mCurrentPath?.let { loadDirectoryContentsAsync(it) }
    }

    private fun loadDirectoryContents(mScreenshotPath: String): Boolean {
        FileUtils.getDirectoryContents(File(mScreenshotPath), context).size
        return FileUtils.getDirectoryContents(File(mScreenshotPath), context).isNotEmpty()
    }

    private fun moveScreenshotDirectoryContents(mOriginalScreenshotPath: String) {
        sharedPrefStorage!!.edit()
                .putString(ORIGINAL_SCREENSHOT_STORAGE_VALUE, mOriginalScreenshotPath).apply()
        if (!File(EsperScreenshotFolder).exists())
            File(EsperScreenshotFolder).mkdir()
        for (i in FileUtils.getDirectoryContents(File(mOriginalScreenshotPath), context)) {
            moveFile(File(i.path), File(EsperScreenshotFolder))
        }
    }

    private fun moveScreenshotDirectoryContentsBack(mUpdatedScreenshotPath: String) {
        for (i in FileUtils.getDirectoryContents(File(EsperScreenshotFolder), context)) {
            moveFile(File(i.path), File(mUpdatedScreenshotPath))
        }
    }

    private fun loadDirectoryContentsAsync(mCurrentPath: String) {
        LoadFileAsync(mCurrentPath, requireContext(), object : OnLoadDoneCallback {
            override fun onLoadDone(itemList: MutableList<Item>) {
                mItemList = itemList
                setRecyclerAdapter()
            }
        }).execute()
    }

    private fun moveFile(file: File, dir: File) {
        val newFile = File(dir, file.name)
        var outputChannel: FileChannel? = null
        var inputChannel: FileChannel? = null
        try {
            outputChannel = FileOutputStream(newFile).channel
            inputChannel = FileInputStream(file).channel
            inputChannel.transferTo(0, inputChannel.size(), outputChannel)
            inputChannel.close()
            file.delete()
        } catch (e: java.lang.Exception) {
        } finally {
            inputChannel?.close()
            outputChannel?.close()
        }
    }

    private fun setRecyclerAdapter() {
        try {
            if (mItemList!!.isEmpty()) {
                mRecyclerItems!!.visibility = View.GONE
                mEmptyView!!.visibility = View.VISIBLE
            }
//            else if (mItemList!!.size <= 1 && mItemList!![0].name!!.contentEquals(".Esper_Empty_File.txt")) {
//                mRecyclerItems!!.visibility = View.GONE
//                mEmptyView!!.visibility = View.VISIBLE
//            }
            else {
                mRecyclerItems!!.visibility = View.VISIBLE
                mEmptyView!!.visibility = View.GONE
            }
        } catch (e: Exception) {
            Log.e(ListItemsFragmentTag, e.message.toString())
        } finally {
        }

        mItemAdapter = ItemAdapter(mItemList!!, this)
        mRecyclerItems!!.adapter = mItemAdapter
        mItemAdapter!!.notifyDataSetChanged()
        mRecyclerItems!!.itemAnimator = DefaultItemAnimator()
    }

    private fun openItem(selectedItem: Item) {
        var isVideoAudio = false
        var isImage = false
        var isPdf = false
        var isZip = false
        hideKeyboard(requireActivity())
        var check = false
        if (selectedItem.isDirectory) {
            openDirectory(selectedItem)
        } else {
            for (i in videoAudioFileFormats)
                if (selectedItem.name!!.endsWith(i, true)) {
                    isVideoAudio = true
                    val intent = Intent(context, VideoViewerActivity::class.java)
                    intent.putExtra("videoPath", selectedItem.path!!)
                    intent.putExtra("isYT", false)
                    startActivity(intent)
                }
            for (i in imageFileFormats)
                if (selectedItem.name!!.endsWith(i, true)) {
                    isImage = true
                    val intent = Intent(context, ImageViewerActivity::class.java)
                    intent.putExtra("imagePath", selectedItem.path)
                    intent.putExtra("imageName", selectedItem.name)
                    startActivity(intent)
                }
            if (selectedItem.name!!.endsWith(".pdf", true)) {
                isPdf = true
                if (!FileUtils.openFile(requireContext(), File(selectedItem.path))) {
                    try {
                        startActivity(
                                PdfViewerActivity.launchPdfFromPath(
                                        context,
                                        selectedItem.path,
                                        selectedItem.name,
                                        selectedItem.name,
                                        enableDownload = false
                                )
                        )
                    } catch (e: Exception) {
                        Toast.makeText(context, "Sorry, Couldn't open up PDF!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            if (selectedItem.name!!.endsWith(".zip", true)) {
                isZip = true
                Decompress(
                        requireContext(),
                        selectedItem.path!!,
                        File(selectedItem.path).parent
                ).execute()
//                FileUtils.unzip(selectedItem.path, File(selectedItem.path).parent)
                loadDirectoryContentsAsync(File(selectedItem.path).parent)
            }
            if (!isVideoAudio && !isImage && !isPdf && !isZip) {
                if (selectedItem.name!!.endsWith(".json")) {
                    mItemListFromJson!!.clear()
                    check = addItemsFromJSON(selectedItem.path)
                }
                if (check)
                    showDialog(
                            activity,
                            selectedItem.name!!.substring(0, selectedItem.name!!.lastIndexOf("."))
                    )
                else
                    FileUtils.openFile(requireContext(), File(selectedItem.path))
            }
        }
    }

    private fun showDialog(activity: Activity?, name: String) {
        dialog = Dialog(requireActivity())
        dialog!!.setContentView(R.layout.fragment_dialog)
        val dialogTitle = dialog!!.findViewById(R.id.dialog_title) as TextView
        dialogTitle.text = name
        mRecyclerDialogItems = dialog!!.findViewById(R.id.dialog_recycler_view)
        mEmptyDialogView = dialog!!.findViewById(R.id.layout_empty_view_dialog)
        mVideoItemAdapter = VideoURLAdapter(activity, mItemListFromJson!!)
        mRecyclerDialogItems!!.adapter = mVideoItemAdapter
        mRecyclerDialogItems!!.layoutManager = LinearLayoutManager(
                context,
                LinearLayoutManager.VERTICAL,
                false
        )
        setupDialogSearchView(dialog!!)
        dialog!!.setCancelable(true)
        dialog!!.setCanceledOnTouchOutside(true)
        dialog!!.show()
    }

    private fun setupDialogSearchView(dialog: Dialog) {
        val btn1: ImageView = dialog.findViewById(R.id.search_btn)
        val searchView: SimpleSearchView = dialog.findViewById(R.id.searchView)
        btn1.setOnClickListener {
            searchView.showSearch()
        }

        searchView.setOnQueryTextListener(object : SimpleSearchView.OnQueryTextListener {
            private var searcheddialog: Boolean = false
            override fun onQueryTextChange(newText: String): Boolean {
                Log.d(ListItemsFragmentTag, "Changed$newText")
                searcheddialog = true
                mVideoItemAdapter!!.filter.filter(newText)
                return false
            }

            override fun onQueryTextSubmit(query: String): Boolean {
                Log.d(ListItemsFragmentTag, "Submitted$query")
                searcheddialog = true
                return false
            }

            override fun onQueryTextCleared(): Boolean {
                Log.d(ListItemsFragmentTag, "Cleared")
                searcheddialog = false
                mVideoItemAdapter!!.filter.filter("")
                return false
            }
        })
        //Dialog Box Search Spacing
        val revealCenter = searchView.revealAnimationCenter
        revealCenter!!.x -= DimensUtils.convertDpToPx(40, dialog.context)
    }

    private fun addItemsFromJSON(path: String?): Boolean {
        var isInFormat = false
        try {
            val jsonDataString: String = readJSONDataFromFile(path)
            val jsonArray = JSONArray(jsonDataString)
            for (i in 0 until jsonArray.length()) {
                val itemObj: JSONObject = jsonArray.getJSONObject(i)
                val name: String = itemObj.getString("name")
                val url: String = itemObj.getString("url")
                val videos = VideoURL(name, url)
                mItemListFromJson!!.add(videos)
                isInFormat = true
            }
        } catch (e: JSONException) {
            Log.e(ListItemsFragmentTag, "addItemsFromJSON: ", e)
            isInFormat = false
        } catch (e: IOException) {
            Log.e(ListItemsFragmentTag, "addItemsFromJSON: ", e)
            isInFormat = false
        }
        return isInFormat
    }

    private fun readJSONDataFromFile(path: String?): String {
        var inputStream: InputStream? = null
        val builder = java.lang.StringBuilder()
        try {
            var jsonString: String?
            inputStream = FileInputStream(File(path))
            val bufferedReader = BufferedReader(
                    InputStreamReader(inputStream, "UTF-8")
            )
            while (bufferedReader.readLine().also { jsonString = it } != null) {
                builder.append(jsonString)
            }
        } finally {
            inputStream?.close()
        }
        return String(builder)
    }

    private fun openDirectory(selectedItem: Item) {
        val listItemsFragment = newInstance(selectedItem.path)
        fragmentManager
                ?.beginTransaction()
                ?.setCustomAnimations(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left,
                        R.anim.slide_in_left,
                        R.anim.slide_out_right
                )
                ?.replace(R.id.layout_content, listItemsFragment)
                ?.addToBackStack(mCurrentPath)!!.commit()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mGridLayoutManager!!.spanCount = 4
            mRecyclerItems!!.layoutManager = mGridLayoutManager
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            mGridLayoutManager!!.spanCount = 1
            mRecyclerItems!!.layoutManager = mGridLayoutManager
        }
    }

    override fun onItemClicked(position: Int) {
        if (mActionMode != null) {
            toggleSelection(position)
        } else {
            val currentItem: Item = mItemList!![position]
            openItem(currentItem)
        }
    }

    override fun onItemLongClicked(position: Int): Boolean {
        if (sharedPref == null)
            sharedPref = requireContext().getSharedPreferences(
                    SHARED_MANAGED_CONFIG_VALUES,
                    Context.MODE_PRIVATE
            )
        if (sharedPref!!.getBoolean(SHARED_MANAGED_CONFIG_DELETION_ALLOWED, true)) {
            if (mActionMode == null) {
                mActionMode =
                        (activity as AppCompatActivity).startActionMode(mActionModeCallback)
            }
            toggleSelection(position)
        } else
            Toast.makeText(context, getString(R.string.deletion_not_allowed), Toast.LENGTH_LONG)
                    .show()
        return true
    }

    private fun toggleSelection(position: Int) {
        mItemAdapter!!.toggleSelection(position)
        val count = mItemAdapter!!.selectedItemCount
        if (count == 0) {
            mActionMode!!.finish()
        } else {
            mActionMode!!.title = count.toString()
            mActionMode!!.invalidate()
        }
    }

    private inner class ActionModeCallback : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu?): Boolean {
            mode.menuInflater.inflate(R.menu.toolbar_cab, menu)
            mItemAdapter!!.setActionModeEnabled(true)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.action_delete -> {
                    val dialogBuilder = AlertDialog.Builder(activity)
                    dialogBuilder.setTitle(R.string.dialog_delete_files_title)
                    dialogBuilder.setMessage(R.string.dialog_delete_files_message)
                    dialogBuilder.setPositiveButton(
                            R.string.yes
                    ) { dialog, _ ->
                        removeSelectedItems()
                        dialog.dismiss()
                        mode.finish()
                    }
                    dialogBuilder.setNegativeButton(
                            R.string.no
                    ) { dialog, _ -> dialog.dismiss() }
                    dialogBuilder.show()
                    true
                }
                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            mItemAdapter!!.clearSelection()
            mActionMode = null
            mItemAdapter!!.setActionModeEnabled(false)
        }
    }

    private fun removeSelectedItems() {
        var removed: Boolean
        val selectedItems = mItemAdapter!!.getSelectedItems()
        var successful = true
        for (currentPosition in selectedItems) {
            try {
                val currentFile: String = mItemList!![currentPosition].path!!
                removed = if (File(currentFile).isDirectory)
                    FileUtils.deleteFolder(currentFile)
                else
                    FileUtils.deleteFile(currentFile)

                if (!removed) {
                    successful = false
                } else {
                    val currentItem: Item = mItemList!![currentPosition]
                    mItemList!!.remove(currentItem)
                }
            } catch (e: java.lang.Exception) {
                Log.e(ListItemsFragmentTag, e.message.toString())
            } finally {

            }
        }
        if (!successful) {
            Toast.makeText(context, R.string.file_not_removed, Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, R.string.file_removed, Toast.LENGTH_SHORT).show()
        }
        mItemAdapter!!.notifyDataSetChanged()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: RefreshStackEvent) {
        if (event.refreshStack) {
            requireFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: SearchText) {
        if (event.newText == "") {
            mCurrentPath?.let { loadDirectoryContentsAsync(it) }
        } else
            mItemAdapter!!.filter.filter(event.newText)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: NewUpdatedMutableList) {
        if (event.newArray.size == 0) {
            mRecyclerItems!!.visibility = View.GONE
            mEmptyView!!.visibility = View.VISIBLE
        } else {
            mItemList = event.newArray
            mRecyclerItems!!.visibility = View.VISIBLE
            mEmptyView!!.visibility = View.GONE
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: NewUpdatedVideoMutableList) {
        if (event.newArray.size == 0) {
            mRecyclerDialogItems!!.visibility = View.GONE
            mEmptyDialogView!!.visibility = View.VISIBLE
        } else {
            mItemListFromJson = event.newArray
            mRecyclerDialogItems!!.visibility = View.VISIBLE
            mEmptyDialogView!!.visibility = View.GONE
        }
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        fragmentCallback = context as UpdateViewOnScroll
    }

    private fun hideKeyboard(activity: Activity) {
        val imm = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        var view = activity.currentFocus
        if (view == null)
            view = View(activity)
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    interface UpdateViewOnScroll {
        fun verticalScroll()
    }

    class RefreshStackEvent(val refreshStack: Boolean)
    class SearchText(val newText: String)
    class NewUpdatedMutableList(val newArray: MutableList<Item>)
    class NewUpdatedVideoMutableList(val newArray: MutableList<VideoURL>)

    companion object {
        private const val KEY_CURRENT_PATH = "current_path"
        fun newInstance(currentDir: String?): ListItemsFragment {
            val itemsBundle = Bundle()
            itemsBundle.putString(KEY_CURRENT_PATH, currentDir)
            val itemsFragment = ListItemsFragment()
            itemsFragment.arguments = itemsBundle
            return itemsFragment
        }
    }
}