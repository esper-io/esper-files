@file:Suppress(
        "RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS",
        "NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS"
)

package io.esper.files.util

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import com.rajat.pdfviewer.PdfViewerActivity
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
                    if(!currentItem.emptySubFolder)
                        directoryList.add(currentItem)

                } else {
                    currentItem = getDataFromFile(currentFile)
                    currentItem.date = formattedDate
                    if(!currentItem.name!!.startsWith(".", ignoreCase = true))
                        fileList.add(currentItem)
                }
            }
        } catch (e: Exception) {
            Log.d("Tag", e.toString())
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
        var childDirs: Int = childsItems.size

        for (i in 0 until childDirs) {
            if(childsItems[i]?.name!!.contentEquals(".Esper_Empty_File.txt")) {
                childsItems[i].delete()
                childDirs -= 1
            }
        }

        directoryItem.path = directory.absolutePath
        directoryItem.name = directory.name
        directoryItem.isDirectory = true
//        if(childsItems!!.size <= 1 && childsItems[0]?.name!!.contentEquals(".Esper_Empty_File.txt")) {
//            childsItems[0].delete()
//            directoryItem.emptySubFolder = true
//        }
        if(childsItems.isEmpty()) {
            directoryItem.emptySubFolder = true
            //childDirs -= 1
        }

        getAllEmptyFoldersOfDir(directory)

        var numItems = childDirs.toString()
        numItems = if (childDirs == 1) {
            "$numItems Item"
        } else {
            "$numItems Items"
        }

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
            val type = getMimeType(Uri.fromFile(file), context)
            Log.d("Tag", type)
            var intent = Intent(Intent.ACTION_VIEW)
            val data = Uri.fromFile(file)
            if(type=="application/pdf")
                intent = PdfViewerActivity.launchPdfFromPath(context, file.path, file.name, file.name, enableDownload = false)
            intent.setDataAndType(data, type)
            context.startActivity(intent)
        }
        catch (e: Exception)
        {
            if(e.message.toString().contains("No Activity found to handle Intent", false))
                Toast.makeText(
                        context,
                        "No Application Available to Open this File. Please Contact your Administrator.",
                        Toast.LENGTH_LONG
                ).show()
        }
        finally {

        }
    }

    private fun getMimeType(uri: Uri, context: Context): String? {
        val mimeType: String?
        mimeType = if (ContentResolver.SCHEME_CONTENT == uri.scheme) {
            val cr: ContentResolver = context.contentResolver
            cr.getType(uri)
        } else {
            val fileExtension = MimeTypeMap.getFileExtensionFromUrl(
                uri
                    .toString()
            )
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                fileExtension.toLowerCase(Locale.getDefault())
            )
        }
        return mimeType
    }

//    fun checkIfExists(filePath: String?): Boolean {
//        val file = File(filePath)
//        return file.exists()
//    }

    fun deleteFile(filePath: String?): Boolean {
        val file = File(filePath)
        return file.delete()
    }

    private fun getAllEmptyFoldersOfDir(current: File): Boolean {
        if (current.isDirectory) {
            val files = current.listFiles()
            if (files.isEmpty()) { //There is no file in this folder - safe to delete
                println("Safe to delete - empty folder: " + current.absolutePath)
                deleteEmptyFolders(current.absoluteFile)
                return true
            } else {
                var totalFolderCount = 0
                var emptyFolderCount = 0
                for (f in files) {
                    if (f.isDirectory) {
                        totalFolderCount++
                        if (getAllEmptyFoldersOfDir(f)) { //safe to delete
                            emptyFolderCount++
                        }
                    }
                }
                if (totalFolderCount == files.size && emptyFolderCount == totalFolderCount) { //only if all folders are safe to delete then this folder is also safe to delete
                    println("Safe to delete - all subfolders are empty: " + current.absolutePath)
                    deleteEmptyFolders(current.absoluteFile)
                    return true
                }
            }
        }
        return false
    }

    private fun deleteEmptyFolders(pathToClear: File) {
        val files = pathToClear.listFiles()
        for (f in files) {
            if (f.isDirectory) if (getAllEmptyFoldersOfDir(f)) if (f.delete()) Log.w("DELETED FOLDER (EMPTY)", f.path)
        }
    }
}