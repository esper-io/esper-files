@file:Suppress("DEPRECATION")

package io.esper.files.async

import android.annotation.SuppressLint
import android.content.Context
import android.os.AsyncTask
import io.esper.files.callback.OnLoadDoneCallback
import io.esper.files.model.Item
import io.esper.files.util.FileUtils
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