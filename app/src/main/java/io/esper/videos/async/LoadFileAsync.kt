@file:Suppress("DEPRECATION")

package io.esper.videos.async

import android.annotation.SuppressLint
import android.content.Context
import android.os.AsyncTask
import io.esper.videos.callback.OnLoadDoneCallback
import io.esper.videos.model.Item
import io.esper.videos.util.FileUtils
import java.io.File

class LoadFileAsync(dir: String, context: Context, callbackOnDone: OnLoadDoneCallback) :
    AsyncTask<Void?, Void?, MutableList<Item>>() {
    private val mCallbackDoneLoad: OnLoadDoneCallback = callbackOnDone
    private val mFileDir: String = dir

    @SuppressLint("StaticFieldLeak")
    private val mContext: Context = context
    override fun doInBackground(vararg params: Void?): MutableList<Item> {
        val currentDir = File(mFileDir)
        return FileUtils.getDirectoryContents(currentDir, mContext)
    }

    override fun onPostExecute(itemList: MutableList<Item>) {
        super.onPostExecute(itemList)
        mCallbackDoneLoad.onLoadDone(itemList)
    }
}