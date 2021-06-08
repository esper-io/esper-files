@file:Suppress("DEPRECATION")

package io.esper.files.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Environment.getExternalStorageDirectory
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.os.UserManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.ferfalk.simplesearchview.SimpleSearchView
import com.ferfalk.simplesearchview.utils.DimensUtils.convertDpToPx
import hendrawd.storageutil.library.StorageUtil
import io.esper.files.R
import io.esper.files.fragment.ListItemsFragment
import org.greenrobot.eventbus.EventBus
import java.io.File

class MainActivity : AppCompatActivity() {

    private var toolbar: Toolbar? = null
    private var searched: Boolean = false
    private var sharedPref: SharedPreferences? = null
    private var sdCardAvailable: Boolean = false
    private var externalStoragePaths: Array<String>? = null
    private var storageext: Boolean = false
    private val storagePermission = 100
    private var mCurrentPath: String = getExternalStorageDirectory()
        .path + File.separator + "esperfiles" + File.separator
    private lateinit var searchView: SimpleSearchView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val builder = VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        externalStoragePaths = StorageUtil.getStorageDirectories(this)
        if (externalStoragePaths!!.size > 1)
            sdCardAvailable = true

        sharedPref = getSharedPreferences("LastPrefStorage", Context.MODE_PRIVATE)
        if (sharedPref!!.getBoolean("ExtStorage", false)) {
            mCurrentPath = if (externalStoragePaths!![0] == "/storage/emulated/0/")
                externalStoragePaths!![1] + "android/data/io.shoonya.shoonyadpc/cache/esperfiles" + File.separator
            else
                externalStoragePaths!![0] + "android/data/io.shoonya.shoonyadpc/cache/esperfiles" + File.separator
        }

        if (SDK_INT >= Build.VERSION_CODES.M) {
            if (!checkPermission()) {
                ActivityCompat.requestPermissions(
                    this@MainActivity, arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ), storagePermission
                )
            } else {
                createdir()
            }
        } else {
            createdir()
        }
        setupSearchView()
    }

    private fun createdir() {
        val fileDirectory = File(mCurrentPath)

        if (!fileDirectory.exists())
            fileDirectory.mkdir()

        initToolbar()
        initFileListFragment()

        val swipeContainer = findViewById<View>(R.id.swipeContainer) as SwipeRefreshLayout
        swipeContainer.setOnRefreshListener {
            getManagedConfigValues()
            refreshItems()
            swipeContainer.isRefreshing = false
        }
    }

    private fun initToolbar() {
        toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayShowTitleEnabled(true)
        }
    }

    private fun initFileListFragment() {
        val listItemsFragment: ListItemsFragment = ListItemsFragment.newInstance(mCurrentPath)
        supportFragmentManager.beginTransaction().replace(R.id.layout_content, listItemsFragment)
            .commit()
    }

//    override fun onConfigurationChanged(newConfig: Configuration) {
//        super.onConfigurationChanged(newConfig)
//
//        // Checks the orientation of the screen
//        if (newConfig.orientation === Configuration.ORIENTATION_LANDSCAPE) {
//            Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show()
//        } else if (newConfig.orientation === Configuration.ORIENTATION_PORTRAIT) {
//            Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show()
//        }
//    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        val item = menu!!.findItem(R.id.toggle_switch)
        item.setActionView(R.layout.actionbar_service_toggle)

        val mySwitch = item.actionView.findViewById<SwitchCompat>(R.id.switchForActionBar)
        if (sdCardAvailable)
            mySwitch.visibility = View.VISIBLE
        else
            mySwitch.visibility = View.GONE

        if (sharedPref!!.getBoolean("ExtStorage", false)) {
            mySwitch.isChecked = true
            mySwitch.text = getString(R.string.external_storage)
        }

        mySwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                mySwitch.text = getString(R.string.external_storage)
                storageext = true
                if (externalStoragePaths!!.size > 1) {
                    mCurrentPath = if (externalStoragePaths!![0] == "/storage/emulated/0/")
                        externalStoragePaths!![1] + "android/data/io.shoonya.shoonyadpc/cache/esperfiles" + File.separator
                    else
                        externalStoragePaths!![0] + "android/data/io.shoonya.shoonyadpc/cache/esperfiles" + File.separator
                }
                sharedPref!!.edit().putBoolean("ExtStorage", true).apply()
            } else {
                mySwitch.text = getString(R.string.internal_storage)
                storageext = false
                mCurrentPath = getExternalStorageDirectory()
                    .path + File.separator + "esperfiles" + File.separator
                sharedPref!!.edit().putBoolean("ExtStorage", false).apply()
            }
            refreshItems()
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.action_search -> {
                searchView.showSearch()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupSearchView() {
        searchView = findViewById(R.id.searchView)
        searchView.enableVoiceSearch(true)
        //searchView.setKeepQuery(true)
        searchView.setOnQueryTextListener(object : SimpleSearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                Log.e("Tag", "Changed$newText")
                searched = true
                EventBus.getDefault().post(ListItemsFragment.SearchText(newText))
                return false
            }

            override fun onQueryTextSubmit(query: String): Boolean {
                Log.e("Tag", "Submitted$query")
                searched = true
                //EventBus.getDefault().post(ListItemsFragment.SearchText(query))
                return false
            }

            override fun onQueryTextCleared(): Boolean {
                Log.e("Tag", "Cleared")
                searched = false
                EventBus.getDefault().post(ListItemsFragment.SearchText(""))
                return false
            }
        })

        val revealCenter = searchView.revealAnimationCenter
        revealCenter!!.x -= convertDpToPx(40, this@MainActivity)
    }

    private fun refreshItems() {
        searchView.closeSearch()
        EventBus.getDefault().post(ListItemsFragment.RefreshStackEvent(true))
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

    override fun onBackPressed() {
        when {
            searchView.onBackPressed() -> {
                return
            }
            searched -> {
                EventBus.getDefault().post(ListItemsFragment.SearchText(""))
                searched = false
            }
            else -> super.onBackPressed()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        try {
            if (searchView.onActivityResult(requestCode, resultCode, data!!)) {
                return
            }
        } catch (e: Exception) {
//            Log.e("Tag", e.message)
        } finally {

        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == storagePermission) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                createdir()
            }
        }
    }

    private fun getManagedConfigValues() {
        var restrictionsBundle: Bundle?
        val userManager =
            getSystemService(Context.USER_SERVICE) as UserManager
        restrictionsBundle = userManager.getApplicationRestrictions(packageName)
        if (restrictionsBundle == null) {
            restrictionsBundle = Bundle()
        }
        if (restrictionsBundle.containsKey("app_name"))
            toolbar!!.title = restrictionsBundle.getString("app_name").toString()
        else
            toolbar!!.title = getString(R.string.app_name)
    }
}