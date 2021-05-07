package io.esper.files.callback

import io.esper.files.model.Item

interface OnLoadDoneCallback {
    fun onLoadDone(itemList: MutableList<Item>)
}