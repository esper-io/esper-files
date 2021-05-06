package com.test.esperproto.activity

import android.Manifest
import android.content.pm.PackageManager
import android.os.*
import android.os.StrictMode.VmPolicy
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.test.esperproto.R
import com.test.esperproto.fragment.ListItemsFragment
import java.io.File
import java.util.*


class MainActivity : AppCompatActivity() {

    private val STORAGE_PERMISSION = 100
    private val DEFAULT_FILE_DIRECTORY: String = Environment.getExternalStorageDirectory()
            .path + File.separator + "esperfiles" + File.separator
    private var mCurrentFolder = DEFAULT_FILE_DIRECTORY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val builder = VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())

        val androidVersion = Build.VERSION.SDK_INT
        if (androidVersion >= Build.VERSION_CODES.M) {
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
        val FILE_DIRECTORY = DEFAULT_FILE_DIRECTORY

        val writeDirectory: Boolean
        val fileDirectory = File(FILE_DIRECTORY)
        Log.i("TAG", FILE_DIRECTORY)

        // Make file directory if it does not exist
        writeDirectory = if (fileDirectory.exists()) {
            true
        } else {
            fileDirectory.mkdir()
        }

        if (writeDirectory) {
//            Toast.makeText(
//                this@MainActivity,
//                "Folder Created/Present, Path: $DEFAULT_FILE_DIRECTORY",
//                Toast.LENGTH_LONG
//            )
//                .show()

            initToolbar()
            initFileListFragment()
        }
        else {
//            Toast.makeText(
//                this@MainActivity,
//                "Couldn't Create Directory / No Such Directory Exists",
//                Toast.LENGTH_LONG
//            )
//                    .show()
        }

        val swipeContainer = findViewById<View>(R.id.swipeContainer) as SwipeRefreshLayout
        swipeContainer.setOnRefreshListener(OnRefreshListener {
            refreshItems()
            swipeContainer.isRefreshing = false;
        })
    }

    private fun initToolbar() {
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayShowTitleEnabled(true)
        }
    }

    private fun initFileListFragment() {
        val listItemsFragment: ListItemsFragment = ListItemsFragment.newInstance(mCurrentFolder)
        supportFragmentManager.beginTransaction().replace(R.id.layout_content, listItemsFragment).commit()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_refresh -> {
                refreshItems()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun refreshItems() {
        initFileListFragment()
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
}