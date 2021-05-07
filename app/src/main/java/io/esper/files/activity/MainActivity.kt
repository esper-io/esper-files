@file:Suppress("DEPRECATION")

package io.esper.files.activity

import android.Manifest
import android.content.pm.PackageManager
import android.os.*
import android.os.Build.VERSION.SDK_INT
import android.os.Environment.getExternalStorageDirectory
import android.os.StrictMode.VmPolicy
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import hendrawd.storageutil.library.StorageUtil
import io.esper.files.R
import io.esper.files.fragment.ListItemsFragment
import io.esper.files.fragment.ListItemsFragment.MessageEvent
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.util.*


class MainActivity : AppCompatActivity() {

    private var sdCardAvailable: Boolean = false
    private var externalStoragePaths: Array<String>? = null
    private var storageext: Boolean = false
    private val STORAGE_PERMISSION = 100
    private var DEFAULT_FILE_DIRECTORY: String = getExternalStorageDirectory()
        .path + File.separator + "esperfiles" + File.separator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        externalStoragePaths = StorageUtil.getStorageDirectories(this)
        Log.d("Tag", externalStoragePaths!!.size.toString())
        if(externalStoragePaths!!.size>1)
            sdCardAvailable = true

        val builder = VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())

        if (SDK_INT >= Build.VERSION_CODES.M) {
            if (!checkPermission()) {
                ActivityCompat.requestPermissions(
                        this@MainActivity, arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                ), STORAGE_PERMISSION
                )
            } else {
                createdir()
            }
        } else {
            createdir()
        }
    }

    private fun createdir() {
        val writeDirectory: Boolean
        val fileDirectory = File(DEFAULT_FILE_DIRECTORY)
        //Log.i("Tag", DEFAULT_FILE_DIRECTORY)

        // Make file directory if it does not exist
        writeDirectory = if (fileDirectory.exists()) {
            true
        } else {
            fileDirectory.mkdir()
        }

//        if (writeDirectory) {
//            Toast.makeText(
//                this@MainActivity,
//                "Folder Created/Present, Path: $DEFAULT_FILE_DIRECTORY",
//                Toast.LENGTH_LONG
//            )
//                .show()

            initToolbar()
            initFileListFragment()
//        } else {
//            Toast.makeText(
//                    this@MainActivity,
//                    "Couldn't Create Directory / No Such Directory Exists",
//                    Toast.LENGTH_LONG
//            )
//                .show()
//        }

        val swipeContainer = findViewById<View>(R.id.swipeContainer) as SwipeRefreshLayout
        swipeContainer.setOnRefreshListener {
            refreshItems()
            swipeContainer.isRefreshing = false
        }
    }

    private fun initToolbar() {
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayShowTitleEnabled(true)
        }
    }

    private fun initFileListFragment() {
        val listItemsFragment: ListItemsFragment = ListItemsFragment.newInstance(DEFAULT_FILE_DIRECTORY)
        supportFragmentManager.beginTransaction().replace(R.id.layout_content, listItemsFragment)
            .commit()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)


            val item = menu!!.findItem(R.id.toggle_switch)
            item.setActionView(R.layout.actionbar_service_toggle)

            val mySwitch = item.actionView.findViewById<SwitchCompat>(R.id.switchForActionBar)
            if(sdCardAvailable)
                mySwitch.visibility = View.VISIBLE
            else
                mySwitch.visibility = View.GONE

            mySwitch.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    mySwitch.text = "External Storage"
                    storageext = true
                    if (externalStoragePaths!!.size > 1) {
                        DEFAULT_FILE_DIRECTORY = if (externalStoragePaths!![0] == "/storage/emulated/0/")
                            externalStoragePaths!![1] + "android/data/io.shoonya.shoonyadpc/cache/esperfiles" + File.separator
                        else
                            externalStoragePaths!![0] + "android/data/io.shoonya.shoonyadpc/cache/esperfiles" + File.separator
                        Log.d("TAG", DEFAULT_FILE_DIRECTORY)
                    }
                    EventBus.getDefault().post(MessageEvent(DEFAULT_FILE_DIRECTORY))
                } else {
                    mySwitch.text = "Internal Storage"
                    storageext = false
                    DEFAULT_FILE_DIRECTORY = getExternalStorageDirectory()
                            .path + File.separator + "esperfiles" + File.separator
                    EventBus.getDefault().post(MessageEvent(DEFAULT_FILE_DIRECTORY))
                    Log.d("TAG", DEFAULT_FILE_DIRECTORY)
                }
            }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.action_refresh -> {
                refreshItems()
                return true
            }
//            R.id.toggle_switch -> {
//                if (!storageext) {
//                    storageext = true
//                    if (externalStoragePaths!!.size > 1) {
//                        DEFAULT_FILE_DIRECTORY = if (externalStoragePaths!![0].equals("/storage/emulated/0/"))
//                            externalStoragePaths!![1] + "esperfiles" + File.separator
//                        else
//                            externalStoragePaths!![0] + "esperfiles" + File.separator
//                    }
//                    Toast.makeText(this, "Ext", Toast.LENGTH_SHORT).show()
//                    refreshItems()
//                } else {
//                    storageext = false
//                    DEFAULT_FILE_DIRECTORY = getExternalStorageDirectory()
//                            .path + File.separator + "esperfiles" + File.separator
//                    Toast.makeText(this, "Int", Toast.LENGTH_SHORT).show()
//                    refreshItems()
//                }
//                return true
//            }

        }
        return super.onOptionsItemSelected(item)
    }

    private fun refreshItems() {
        //initFileListFragment()
        EventBus.getDefault().post(MessageEvent(DEFAULT_FILE_DIRECTORY))
    }

    private fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                Toast.makeText(this@MainActivity, "Storage Permission Granted", Toast.LENGTH_SHORT).show()
                createdir()
            } else {
//                Toast.makeText(this@MainActivity, "Storage Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}