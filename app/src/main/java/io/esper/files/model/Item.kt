package io.esper.files.model

import java.util.*

class Item : Comparable<Item?> {
    var name: String? = null
    var data: String? = null
    var date: String? = null
    var path: String? = null
    private var image: String? = null
    var emptySubFolder: Boolean = false
    var isDirectory = false

    constructor()
    constructor(
        name: String?,
        data: String?,
        date: String?,
        path: String?,
        image: String?,
        emptySubFolder: Boolean,
        isDirectory: Boolean
    ) {
        this.name = name
        this.data = data
        this.date = date
        this.path = path
        this.image = image
        this.emptySubFolder = emptySubFolder
        this.isDirectory = isDirectory
    }

    override fun compareTo(other: Item?): Int {
        return if (name != null) {
            name!!.toLowerCase(Locale.getDefault()).compareTo(other!!.name!!.toLowerCase(Locale.getDefault()))
        } else {
            throw IllegalArgumentException()
        }
    }
}