package com.test.esperproto.callback

import com.test.esperproto.model.Item

interface OnLoadDoneCallback {
    fun onLoadDone(itemList: MutableList<Item>)
}