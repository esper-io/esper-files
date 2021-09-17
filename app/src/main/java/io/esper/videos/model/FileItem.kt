package io.esper.videos.model

/**
 * Model object for a file item.
 */
class FileItem : Comparable<FileItem> {
    var name: String? = null
    var path: String? = null
    var isDirectory: Boolean? = null
    var isImage: Boolean? = null
    override fun compareTo(other: FileItem): Int {
        return if (isDirectory === other.isDirectory) {
            path!!.compareTo(other.path!!, ignoreCase = true)
        } else {
            if (isDirectory!!) -1 else 1
        }
    }

    override fun equals(other: Any?): Boolean {
        return other is FileItem && path == other.path
    }

    override fun hashCode(): Int {
        var result = name?.hashCode() ?: 0
        result = 31 * result + (path?.hashCode() ?: 0)
        result = 31 * result + (isDirectory?.hashCode() ?: 0)
        result = 31 * result + (isImage?.hashCode() ?: 0)
        return result
    }
}