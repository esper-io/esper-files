@file:Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "DEPRECATION")

package io.esper.files.fragment

import android.annotation.TargetApi
import android.app.Activity
import android.app.AlertDialog
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.esper.files.R
import io.esper.files.adapter.ItemAdapter
import io.esper.files.adapter.ItemAdapter.ClickListener
import io.esper.files.async.LoadFileAsync
import io.esper.files.callback.OnLoadDoneCallback
import io.esper.files.model.Item
import io.esper.files.util.FileUtils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.util.*


class ListItemsFragment : Fragment(), ClickListener {
    private var mGridLayoutManager: GridLayoutManager? = null
    private var mRecyclerItems: RecyclerView? = null
    private var mItemAdapter: ItemAdapter? = null
    private var mItemList: MutableList<Item>? = null
    private var mEmptyView: RelativeLayout? = null
    private var mCurrentPath: String? = null
    private var mActionMode: ActionMode? = null
    private val mActionModeCallback: ActionModeCallback = ActionModeCallback()

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mItemList = ArrayList()
        mCurrentPath = arguments!!.getString(KEY_CURRENT_PATH)!!
    }

    @Nullable
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val itemsView: View = inflater.inflate(R.layout.fragment_items, container, false)
        mGridLayoutManager = GridLayoutManager(context, 1)
        mRecyclerItems = itemsView.findViewById<View>(R.id.recycler_view_items) as RecyclerView
        mEmptyView = itemsView.findViewById<View>(R.id.layout_empty_view) as RelativeLayout
        mRecyclerItems!!.layoutManager = mGridLayoutManager
        return itemsView
    }

    override fun onViewCreated(view: View, @Nullable savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mCurrentPath?.let { loadDirectoryContentsAsync(it) }
    }

    private fun loadDirectoryContentsAsync(mCurrentPath: String) {
        LoadFileAsync(mCurrentPath, object : OnLoadDoneCallback {
            override fun onLoadDone(itemList: MutableList<Item>) {
                mItemList = itemList
                setRecyclerAdapter()
            }
        }).execute()
    }

    private fun setRecyclerAdapter() {
        try {
            if(mItemList!!.isEmpty())
            {
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
        }
        catch (e: Exception)
        {
            Log.e("TAG", e.message.toString())
        }
        finally {
        }

        mItemAdapter = ItemAdapter(mItemList!!, this)
        mRecyclerItems!!.adapter = mItemAdapter
        mItemAdapter!!.notifyDataSetChanged()
        mRecyclerItems!!.itemAnimator = DefaultItemAnimator()
    }

    private fun openItem(selectedItem: Item) {
        val imm =
            activity!!.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        var view = activity!!.currentFocus
        if (view == null) {
            view = View(activity)
        }
        imm.hideSoftInputFromWindow(view.windowToken, 0)

        if (selectedItem.isDirectory) {
            openDirectory(selectedItem)
        } else {
//            if(selectedItem.name!!.contains(".json")) {
//                //readFromFile(selectedItem.path)
//                Log.d("Tag", readFromFile(selectedItem.path))
//            }
//            else
                openFile(selectedItem)
        }
    }

//    private fun readFromFile(path: String?): String? {
//        var ret = ""
//        try {
//            val inputStream: InputStream = FileInputStream(File(path))
//            val inputStreamReader = InputStreamReader(inputStream)
//            val bufferedReader = BufferedReader(inputStreamReader)
//            var receiveString: String? = ""
//            val stringBuilder = StringBuilder()
//            while (bufferedReader.readLine().also { receiveString = it } != null) {
//                stringBuilder.append(receiveString)
//            }
//            inputStream.close()
//            ret = stringBuilder.toString()
//        } catch (e: FileNotFoundException) {
//            Log.e("FileToJson", "File not found: " + e.toString())
//        } catch (e: IOException) {
//            Log.e("FileToJson", "Can not read file: " + e.toString())
//        }
//        return ret
//    }

    private fun openFile(selectedItem: Item) {
        val file = File(selectedItem.path)
        context?.let { FileUtils.openFile(it, file) }
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
            mGridLayoutManager!!.spanCount = SIZE_GRID
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
        if (mActionMode == null) {
            mActionMode =
                (activity as AppCompatActivity?)!!.startActionMode(mActionModeCallback)
        }
        toggleSelection(position)
        return true
    }

    private fun toggleSelection(position: Int) {
        mItemAdapter!!.toggleSelection(position)
        val count = mItemAdapter!!.selectedItemCount
        if (count == 0) {
            mActionMode!!.finish()
        }
        else {
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
        val selectedItems = mItemAdapter!!.getSelectedItems()
        var successful = true
        for (currentPosition in selectedItems) {
            try {
                val currentFile: String = mItemList!![currentPosition].path!!
                val removed: Boolean = FileUtils.deleteFile(currentFile)
                if (!removed) {
                    successful = false
                } else {
                    val currentItem: Item = mItemList!![currentPosition]
                    mItemList!!.remove(currentItem)
                }
            }
            catch (e: java.lang.Exception)
            {
                Log.e("Tag", e.message.toString())
            }
            finally {

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
        if(event.refreshStack) {
            fragmentManager!!.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: SearchText) {
        if(event.newText=="")
        {
            mCurrentPath?.let { loadDirectoryContentsAsync(it) }
        }
        else
            mItemAdapter!!.filter!!.filter(event.newText)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: newArray) {
        mItemList = event.newArray
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    class RefreshStackEvent(val refreshStack: Boolean)

    class SearchText(val newText: String)

    class newArray(val newArray: MutableList<Item>)

    companion object {
        private const val KEY_CURRENT_PATH = "current_path"
        private const val SIZE_GRID = 4
        fun newInstance(currentDir: String?): ListItemsFragment {
            val itemsBundle = Bundle()
            itemsBundle.putString(KEY_CURRENT_PATH, currentDir)
            val itemsFragment = ListItemsFragment()
            itemsFragment.arguments = itemsBundle
            return itemsFragment
        }
    }
}