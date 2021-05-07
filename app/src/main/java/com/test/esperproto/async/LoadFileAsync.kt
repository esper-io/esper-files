@file:Suppress("DEPRECATION")

package com.test.esperproto.async

import android.os.AsyncTask
import com.test.esperproto.callback.OnLoadDoneCallback
import com.test.esperproto.model.Item
import com.test.esperproto.util.FileUtils
import java.io.File

class LoadFileAsync(dir: String, callbackOnDone: OnLoadDoneCallback) :
    AsyncTask<Void?, Void?, MutableList<Item>>() {
    private val mCallbackDoneLoad: OnLoadDoneCallback = callbackOnDone
    private val mFileDir: String = dir
    override fun doInBackground(vararg params: Void?): MutableList<Item> {
        val currentDir = File(mFileDir)
        return FileUtils.getDirectoryContents(currentDir)
    }

    override fun onPostExecute(itemList: MutableList<Item>) {
        super.onPostExecute(itemList)
        mCallbackDoneLoad.onLoadDone(itemList)
    }

}