@file:Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")

package io.esper.files.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import io.esper.files.model.Item
import java.io.File
import java.text.DateFormat
import java.text.DecimalFormat
import java.util.*

object FileUtils {
    /**
     * This method is used to fetch all contents of a received directory.
     *
     * @param currentDir
     * @return
     */
    fun getDirectoryContents(currentDir: File): MutableList<Item> {

        // list all files from the current dir
        val dirs = currentDir.listFiles()
        val directoryList: MutableList<Item> = ArrayList<Item>()
        val fileList: MutableList<Item> = ArrayList<Item>()
        try {
            for (currentFile in dirs) {
                var currentItem: Item
                val lastModified = Date(currentFile.lastModified())
                val dateFormatter = DateFormat.getDateTimeInstance()
                val formattedDate = dateFormatter.format(lastModified)
                if (currentFile.isDirectory) {
                    currentItem = getDataFromDirectory(currentFile)
                    //fileList.addAll(getDirectoryContents(currentFile))
                    currentItem.date = formattedDate
                    directoryList.add(currentItem)
                } else {
                    currentItem = getDataFromFile(currentFile)
                    currentItem.date = formattedDate
                    fileList.add(currentItem)
                }
            }
        } catch (e: Exception) {
            Log.d("LOG", e.toString())
        }

        // sort both lists and then add the file list on directory list
        // this way the directories will be listed first and later the files
        directoryList.sort()
        fileList.sort()

        // show directories on top and files on bottom
        directoryList.addAll(fileList)
        return directoryList
    }

    private fun getDataFromDirectory(directory: File): Item {
        val directoryItem = Item()
        val childsItems = directory.listFiles()
        val childDirs: Int = childsItems?.size ?: 0
        var numItems = childDirs.toString()
        numItems = if (childDirs == 0) {
            "$numItems item"
        } else {
            "$numItems items"
        }
        directoryItem.path = directory.absolutePath
        directoryItem.name = directory.name
        directoryItem.isDirectory = true
        directoryItem.data = numItems
        return directoryItem
    }

    private fun getDataFromFile(file: File): Item {
        val fileItem = Item()
        fileItem.path = file.absolutePath
        fileItem.name = file.name
        fileItem.isDirectory = false
        val precision = DecimalFormat("0.00")
        when {
            file.length()>1073741823 -> fileItem.data = precision.format(file.length() / 1073741824.toFloat()) + " GB"
            file.length()>1048575 -> fileItem.data = precision.format(file.length() / 1048576.toFloat()) + " MB"
            file.length()>1023 -> fileItem.data = precision.format(file.length() / 1024.toFloat()) + " KB"
            else -> fileItem.data = file.length().toString() + " Bytes"
        } // x Bytes
        return fileItem
    }

    fun openFile(context: Context, file: File) {
        try {
            val type = getFileType(file)
            val intent = Intent(Intent.ACTION_VIEW)
            val data = Uri.fromFile(file)
            intent.setDataAndType(data, type)
            context.startActivity(intent)
        }
        catch (e: Exception)
        {
            Toast.makeText(context, e.message.toString(), Toast.LENGTH_LONG).show()
        }
        finally {

        }
    }

    private fun getFileType(file: File): String {
        val map = MimeTypeMap.getSingleton()
        val ext = MimeTypeMap.getFileExtensionFromUrl(file.name)
        var type = map.getMimeTypeFromExtension(ext)
        if (type == null) {
            type = "*/*"
        }
        return type
    }

    fun checkIfExists(filePath: String?): Boolean {
        val file = File(filePath)
        return file.exists()
    }

    fun deleteFile(filePath: String?): Boolean {
        val file = File(filePath)
        return file.delete()
    }
}