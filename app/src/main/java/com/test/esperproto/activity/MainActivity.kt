@file:Suppress("DEPRECATION")

package com.test.esperproto.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.os.Build.VERSION.SDK_INT
import android.os.StrictMode.VmPolicy
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.annotation.Nullable
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
import kotlin.system.exitProcess


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
        }
        else {
            createdir()
        }
        if (SDK_INT >= Build.VERSION_CODES.R) {
            if(!Environment.isExternalStorageManager())
                requestPermission()
            else
                createdir()
        }
        else {
            createdir()
        }
    }

    private fun createdir() {
        //requestPermission()
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
        } else {
            Toast.makeText(
                this@MainActivity,
                "Couldn't Create Directory / No Such Directory Exists",
                Toast.LENGTH_LONG
            )
                .show()
        }

        val swipeContainer = findViewById<View>(R.id.swipeContainer) as SwipeRefreshLayout
        swipeContainer.setOnRefreshListener({
            refreshItems()
            swipeContainer.isRefreshing = false
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
        supportFragmentManager.beginTransaction().replace(R.id.layout_content, listItemsFragment)
            .commit()
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

    private fun requestPermission() {
        if (SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                intent.data = Uri.parse(String.format("package:%s", applicationContext.packageName))
                startActivityForResult(intent, 2296)
            } catch (e: Exception) {
                val intent = Intent()
                intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                startActivityForResult(intent, 2296)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, @Nullable data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2296) {
            if (SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    // perform action when allow permission success
                        //finish()
                    val packageManager: PackageManager = packageManager
                    val intent = packageManager.getLaunchIntentForPackage(packageName)
                    val componentName = intent!!.component
                    val mainIntent = Intent.makeRestartActivityTask(componentName)
                    startActivity(mainIntent)
                    exitProcess(0)
                } else {
                    Toast.makeText(this, "Allow permission for storage access!", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }
}