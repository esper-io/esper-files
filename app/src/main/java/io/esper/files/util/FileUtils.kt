@file:Suppress(
        "RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS",
        "NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS"
)

package io.esper.files.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import io.esper.files.constants.Constants.FileUtilsTag
import io.esper.files.model.Item
import io.esper.files.util.InstallUtil.install
import java.io.*
import java.text.DateFormat
import java.text.DecimalFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

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
        val directoryList: MutableList<Item> = ArrayList()
        val fileList: MutableList<Item> = ArrayList()
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
                    if (!currentItem.emptySubFolder)
                        directoryList.add(currentItem)

                } else {
                    currentItem = getDataFromFile(currentFile)
                    currentItem.date = formattedDate
                    if (!currentItem.name!!.startsWith(".", ignoreCase = true))
                        fileList.add(currentItem)
                }
            }
        } catch (e: Exception) {
            Log.d(FileUtilsTag, e.toString())
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
            if (childsItems[i]?.name!!.startsWith(".")) {
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
        if (childsItems.isEmpty()) {
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
            file.length() > 1073741823 -> fileItem.data =
                    precision.format(file.length() / 1073741824.toFloat()) + " GB"
            file.length() > 1048575 -> fileItem.data =
                    precision.format(file.length() / 1048576.toFloat()) + " MB"
            file.length() > 1023 -> fileItem.data =
                    precision.format(file.length() / 1024.toFloat()) + " KB"
            else -> fileItem.data = file.length().toString() + " Bytes"
        } // x Bytes
        return fileItem
    }

    fun openFile(context: Context, file: File) {
        try {
            val type = getMimeType(file)
            val intent = Intent(Intent.ACTION_VIEW)
            var data = Uri.fromFile(file)
            if (file.name.endsWith(
                            ".apk",
                            false
                    ) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
            ) {
                data = FileProvider.getUriForFile(
                        context, context.packageName + ".provider",
                        file
                )
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            intent.setDataAndType(data, type)
            context.startActivity(intent)
        } catch (e: Exception) {
            //if(e.message.toString().contains("No Activity found to handle Intent", false))
            Toast.makeText(
                    context,
                    "No Application Available to Open this File. Please Contact your Administrator.",
                    Toast.LENGTH_LONG
            ).show()
        } finally {

        }
    }

    private fun getMimeType(file: File): String? {
        var mimeType: String? = ""
        val extension: String = getExtension(file.name)
        if (MimeTypeMap.getSingleton().hasExtension(extension)) {
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        }
        return mimeType
    }

    private fun getExtension(fileName: String): String {
        val arrayOfFilename = fileName.toCharArray()
        for (i in arrayOfFilename.size - 1 downTo 1) {
            if (arrayOfFilename[i] == '.') {
                return fileName.substring(i + 1, fileName.length)
            }
        }
        return ""
    }

//    fun checkIfExists(filePath: String?): Boolean {
//        val file = File(filePath)
//        return file.exists()
//    }

    fun deleteFile(filePath: String?): Boolean {
        val file = File(filePath)
        return file.delete()
    }

    fun deleteFolder(dirPath: String?): Boolean {
        return try {
            val children = File(dirPath).list()
            for (i in children.indices) {
                if (File(File(dirPath), children[i]).isDirectory)
                    deleteFolder(File(File(dirPath), children[i]).path)
                else
                    deleteFile(File(File(dirPath), children[i]).path)
            }
            true
        } catch (e: Exception) {
            false
        } finally {

        }
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
            if (f.isDirectory) if (getAllEmptyFoldersOfDir(f)) if (f.delete()) Log.w(
                    "DELETED FOLDER (EMPTY)",
                    f.path
            )
        }
    }

    fun unzip(sourceFile: String?, destinationFolder: String?): Boolean {
        var zis: ZipInputStream? = null
        try {
            zis = ZipInputStream(BufferedInputStream(FileInputStream(sourceFile)))
            var ze: ZipEntry
            var count: Int
            val buffer = ByteArray(8192)
            while (zis.nextEntry.also { ze = it } != null) {
                if (ze.name != null) {
                    var fileName: String = ze.name
                    fileName = fileName.substring(fileName.indexOf("/") + 1)
                    val file = File(destinationFolder, fileName)
                    val dir = if (ze.isDirectory) file else file.parentFile
                    if (!dir.isDirectory && !dir.mkdirs()) throw FileNotFoundException("Invalid path: " + dir.absolutePath)
                    if (ze.isDirectory) continue
                    FileOutputStream(file).use { fout ->
                        while (zis.read(buffer).also { count = it } != -1) fout.write(
                                buffer,
                                0,
                                count
                        )
                    }
                } else
                    return true
            }
        } catch (ioe: Exception) {
            Log.d(FileUtilsTag, ioe.toString())
            return false
        } finally {
            if (zis != null) try {
                zis.close()

            } catch (e: IOException) {
            }
        }
        return true
    }

    fun unzipFromSync(
        context: Context,
        sourceFile: String?,
        destinationFolder: String?
    ): Boolean {
        var zis: ZipInputStream? = null
        try {
            zis = ZipInputStream(BufferedInputStream(FileInputStream(sourceFile)))
            var ze: ZipEntry
            var count: Int
            val buffer = ByteArray(8192)
            while (zis.nextEntry.also { ze = it } != null) {
                if (ze.name != null) {
                    var fileName: String = ze.name
                    fileName = fileName.substring(fileName.indexOf("/") + 1)
                    val file = File(destinationFolder, fileName)
                    val dir = if (ze.isDirectory) file else file.parentFile
                    if (!dir.isDirectory && !dir.mkdirs()) throw FileNotFoundException("Invalid path: " + dir.absolutePath)
                    if (ze.isDirectory) continue
                    FileOutputStream(file).use { fout ->
                        while (zis.read(buffer).also { count = it } != -1) fout.write(
                            buffer,
                            0,
                            count
                        )
                    }
                    if (fileName.endsWith(".apk"))
                        install(context, File(destinationFolder + fileName))
                    if (fileName.contains("qrcp"))
                        File(destinationFolder + fileName).delete()
                } else
                    return true
            }
        } catch (ioe: Exception) {
            Log.d(FileUtilsTag, ioe.toString())
            return false
        } finally {
            if (zis != null) try {
                zis.close()

            } catch (e: IOException) {
            }
        }
        return true
    }
}