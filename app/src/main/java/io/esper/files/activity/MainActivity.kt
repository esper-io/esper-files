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
import io.esper.files.constants.Constants.ExternalRootFolder
import io.esper.files.constants.Constants.InternalCheckerString
import io.esper.files.constants.Constants.InternalRootFolder
import io.esper.files.constants.Constants.MainActivityTag
import io.esper.files.constants.Constants.SHARED_EXTERNAL_STORAGE_VALUE
import io.esper.files.constants.Constants.SHARED_LAST_PREFERRED_STORAGE
import io.esper.files.constants.Constants.SHARED_MANAGED_CONFIG_APP_NAME
import io.esper.files.constants.Constants.SHARED_MANAGED_CONFIG_SHOW_SCREENSHOTS
import io.esper.files.constants.Constants.SHARED_MANAGED_CONFIG_VALUES
import io.esper.files.constants.Constants.storagePermission
import io.esper.files.fragment.ListItemsFragment
import org.greenrobot.eventbus.EventBus
import java.io.File

class MainActivity : AppCompatActivity() {

    private var toolbar: Toolbar? = null
    private var searched: Boolean = false
    private var sharedPref: SharedPreferences? = null
    private var sharedPrefManaged: SharedPreferences? = null
    private var sdCardAvailable: Boolean = false
    private var externalStoragePaths: Array<String>? = null
    private var mCurrentPath: String = InternalRootFolder
    private lateinit var searchView: SimpleSearchView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init()
    }

    private fun init() {
        //Used for Glide Image Loader
        val builder = VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())

        sharedPrefManaged = getSharedPreferences(SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE)
        getManagedConfigValues()

        val text: String = sharedPrefManaged!!.getString(SHARED_MANAGED_CONFIG_APP_NAME, R.string.app_name.toString())!!

        toolbar = findViewById(R.id.toolbar)
        toolbar!!.title = text
        setSupportActionBar(toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayShowTitleEnabled(true)
        }

        externalStoragePaths = StorageUtil.getStorageDirectories(this)
        if (externalStoragePaths!!.size > 1)
            sdCardAvailable = true

        sharedPref = getSharedPreferences(SHARED_LAST_PREFERRED_STORAGE, Context.MODE_PRIVATE)
        if (sharedPref!!.getBoolean(SHARED_EXTERNAL_STORAGE_VALUE, false)) {
            mCurrentPath = if (externalStoragePaths!![0] == InternalCheckerString)
                externalStoragePaths!![1] + ExternalRootFolder else externalStoragePaths!![0] + ExternalRootFolder
        }

        if (SDK_INT >= Build.VERSION_CODES.M && !checkPermission())
            requestPermission() else createDir()

        initFileListFragment()
        setSearchView()
        setSwipeRefresher()
    }

    private fun initFileListFragment() {
        val listItemsFragment: ListItemsFragment = ListItemsFragment.newInstance(mCurrentPath)
        supportFragmentManager.beginTransaction().replace(R.id.layout_content, listItemsFragment)
                .commit()
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
                this, arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        ), storagePermission
        )
    }

    private fun createDir() {
        val fileDirectory = File(mCurrentPath)
        if (!fileDirectory.exists())
            fileDirectory.mkdir()
    }

    private fun setSwipeRefresher() {
        val swipeContainer = findViewById<View>(R.id.swipeContainer) as SwipeRefreshLayout
        swipeContainer.setOnRefreshListener {
            getManagedConfigValues()
            refreshItems()
            swipeContainer.isRefreshing = false
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        val item = menu!!.findItem(R.id.toggle_switch)
        item.setActionView(R.layout.actionbar_service_toggle)

        val mySwitch = item.actionView.findViewById<SwitchCompat>(R.id.switchForActionBar)
        mySwitch.visibility = if (sdCardAvailable) View.VISIBLE else View.GONE

        if (sharedPref!!.getBoolean("ExtStorage", false)) {
            mySwitch.isChecked = true
            mySwitch.text = getString(R.string.external_storage)
        }

        mySwitch.setOnCheckedChangeListener { _, isChecked ->
            val storageExt: Boolean
            if (isChecked) {
                mySwitch.text = getString(R.string.external_storage)
                storageExt = true
                if (externalStoragePaths!!.size > 1) {
                    mCurrentPath = if (externalStoragePaths!![0] == InternalCheckerString)
                        externalStoragePaths!![1] + ExternalRootFolder else externalStoragePaths!![0] + ExternalRootFolder
                }
            } else {
                mySwitch.text = getString(R.string.internal_storage)
                storageExt = false
                mCurrentPath = InternalRootFolder
            }
            sharedPref!!.edit().putBoolean(SHARED_EXTERNAL_STORAGE_VALUE, storageExt).apply()
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

    private fun setSearchView() {
        searchView = findViewById(R.id.searchView)
        searchView.enableVoiceSearch(true)
        //searchView.setKeepQuery(true)
        searchView.setOnQueryTextListener(object : SimpleSearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                Log.e(MainActivityTag, "Changed$newText")
                searched = true
                EventBus.getDefault().post(ListItemsFragment.SearchText(newText))
                return false
            }

            override fun onQueryTextSubmit(query: String): Boolean {
                Log.e(MainActivityTag, "Submitted$query")
                searched = true
                return false
            }

            override fun onQueryTextCleared(): Boolean {
                Log.e(MainActivityTag, "Cleared")
                searched = false
                EventBus.getDefault().post(ListItemsFragment.SearchText(""))
                return false
            }
        })
        //Spacing for Search Bar
        val revealCenter = searchView.revealAnimationCenter
        revealCenter!!.x -= convertDpToPx(40, this)
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
            searchView.onBackPressed() ->
                return
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
            Log.e(MainActivityTag, e.message.toString())
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
                createDir()
            }
        }
    }

//    Managed Config Example Values
//    {
//        "app_name": "Company Name",
//        "show_screenshots_folder": true/false
//    }

    private fun getManagedConfigValues() {
        var restrictionsBundle: Bundle?
        val userManager =
                getSystemService(Context.USER_SERVICE) as UserManager
        restrictionsBundle = userManager.getApplicationRestrictions(packageName)
        if (restrictionsBundle == null) {
            restrictionsBundle = Bundle()
        }

        val newAppName = if (restrictionsBundle.containsKey(SHARED_MANAGED_CONFIG_APP_NAME))
            restrictionsBundle.getString(SHARED_MANAGED_CONFIG_APP_NAME).toString() else getString(R.string.app_name)

        val showScreenshotsFolder = if (restrictionsBundle.containsKey(SHARED_MANAGED_CONFIG_SHOW_SCREENSHOTS))
            restrictionsBundle.getBoolean(SHARED_MANAGED_CONFIG_SHOW_SCREENSHOTS) else false

        if (toolbar != null)
            toolbar!!.title = newAppName

        sharedPrefManaged!!.edit().putString(SHARED_MANAGED_CONFIG_APP_NAME, newAppName).apply()
        sharedPrefManaged!!.edit().putBoolean(SHARED_MANAGED_CONFIG_SHOW_SCREENSHOTS, showScreenshotsFolder).apply()
    }
}