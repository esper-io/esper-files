package io.esper.videos.callback

import io.esper.videos.model.Item

interface OnLoadDoneCallback {
    fun onLoadDone(itemList: MutableList<Item>)
}