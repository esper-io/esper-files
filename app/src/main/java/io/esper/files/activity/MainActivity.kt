@file:Suppress("DEPRECATION")

package io.esper.files.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import android.os.Build.VERSION.SDK_INT
import android.os.StrictMode.VmPolicy
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
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
import io.esper.files.service.AutoInstallService
import io.esper.files.util.FileUtils
import io.esper.files.util.InstallUtil
import org.greenrobot.eventbus.EventBus
import java.io.*
import java.net.URL


@SuppressLint("StaticFieldLeak")
private lateinit var mActivity: Activity

@SuppressLint("StaticFieldLeak")
private lateinit var mContext: Context

class MainActivity : AppCompatActivity() {

    private var toolbar: Toolbar? = null
    private var searched: Boolean = false
    private var sharedPref: SharedPreferences? = null
    private var sharedPrefManaged: SharedPreferences? = null
    private var sdCardAvailable: Boolean = false
    private var externalStoragePaths: Array<String>? = null
    private var mCurrentPath: String = InternalRootFolder
    private var searchView: SimpleSearchView? = null

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

        toolbar = findViewById(R.id.toolbar)
        toolbar!!.title = sharedPrefManaged!!.getString(
            SHARED_MANAGED_CONFIG_APP_NAME,
            R.string.app_name.toString()
        )
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
            requestPermission() else createDir(mCurrentPath)

        initFileListFragment()
        setSearchView()
        setSwipeRefresher()
    }

    private fun initFileListFragment() {
        val listItemsFragment: ListItemsFragment = ListItemsFragment.newInstance(mCurrentPath)
        supportFragmentManager.beginTransaction().replace(R.id.layout_content, listItemsFragment)
            .commitAllowingStateLoss()
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ), storagePermission
        )
    }

    private fun createDir(path: String) {
        val fileDirectory = File(path)
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
                searchView!!.showSearch()
                return true
            }
            R.id.action_sync -> {
                if (!InstallUtil.isUnknownSourcesSettingOpen(
                        this
                    )
                )
                    InstallUtil.checkUnknownSourcesSetting(this)
                else if (!InstallUtil.isAccessibilitySettingOpen(
                        AutoInstallService::class.java,
                        this
                    )
                )
                    InstallUtil.checkAccessibilitySetting(this, AutoInstallService::class.java)
                else {
                    mActivity = this
                    mContext = this
                    DownloadFile().execute("http://192.168.0.123:51515/send/transfer")
                    refreshItems()
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setSearchView() {
        searchView = findViewById(R.id.searchView)
        searchView!!.enableVoiceSearch(true)
        searchView!!.setBackIconDrawable(null)
        //searchView.setKeepQuery(true)
        searchView!!.setOnQueryTextListener(object : SimpleSearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                Log.d(MainActivityTag, "Changed$newText")
                searched = true
                EventBus.getDefault().post(ListItemsFragment.SearchText(newText))
                return false
            }

            override fun onQueryTextSubmit(query: String): Boolean {
                Log.d(MainActivityTag, "Submitted$query")
                searched = true
                return false
            }

            override fun onQueryTextCleared(): Boolean {
                Log.d(MainActivityTag, "Cleared")
                searched = false
                EventBus.getDefault().post(ListItemsFragment.SearchText(""))
                return false
            }
        })
        //Spacing for Search Bar
        val revealCenter = searchView!!.revealAnimationCenter
        revealCenter!!.x -= convertDpToPx(40, this)
    }

    fun refreshItems() {
        if (searchView != null)
            searchView!!.closeSearch()
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
            searchView!!.onBackPressed() ->
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
            if (searchView!!.onActivityResult(requestCode, resultCode, data!!)) {
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
                createDir(mCurrentPath)
            }
        }
    }

//    Managed Config Example Values
//    {
//        "app_name": "Company Name",
//        "show_screenshots_folder": true/false
//    }

    private fun startManagedConfigValuesReciever() {
        val myRestrictionsMgr =
            getSystemService(Context.RESTRICTIONS_SERVICE) as RestrictionsManager
        val restrictionsFilter = IntentFilter(Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED)

        val restrictionsReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val appRestrictions = myRestrictionsMgr.applicationRestrictions

                val newAppName = if (appRestrictions.containsKey(SHARED_MANAGED_CONFIG_APP_NAME))
                    appRestrictions.getString(SHARED_MANAGED_CONFIG_APP_NAME)
                        .toString() else getString(R.string.app_name)

                val showScreenshotsFolder =
                    if (appRestrictions.containsKey(SHARED_MANAGED_CONFIG_SHOW_SCREENSHOTS))
                        appRestrictions.getBoolean(SHARED_MANAGED_CONFIG_SHOW_SCREENSHOTS) else false

                if (toolbar != null)
                    toolbar!!.title = newAppName

                sharedPrefManaged!!.edit().putString(SHARED_MANAGED_CONFIG_APP_NAME, newAppName)
                    .apply()
                if (showScreenshotsFolder != (sharedPrefManaged!!.getBoolean(
                        SHARED_MANAGED_CONFIG_SHOW_SCREENSHOTS,
                        false
                    ))
                ) {
                    sharedPrefManaged!!.edit()
                        .putBoolean(
                            SHARED_MANAGED_CONFIG_SHOW_SCREENSHOTS,
                            showScreenshotsFolder
                        ).apply()
                    refreshItems()
                }
            }
        }
        registerReceiver(restrictionsReceiver, restrictionsFilter)
    }

    private fun getManagedConfigValues() {
        var restrictionsBundle: Bundle?
        val userManager =
            getSystemService(Context.USER_SERVICE) as UserManager
        restrictionsBundle = userManager.getApplicationRestrictions(packageName)
        if (restrictionsBundle == null) {
            restrictionsBundle = Bundle()
        }

        val newAppName = if (restrictionsBundle.containsKey(SHARED_MANAGED_CONFIG_APP_NAME))
            restrictionsBundle.getString(SHARED_MANAGED_CONFIG_APP_NAME)
                .toString() else getString(R.string.app_name)

        val showScreenshotsFolder =
            if (restrictionsBundle.containsKey(SHARED_MANAGED_CONFIG_SHOW_SCREENSHOTS))
                restrictionsBundle.getBoolean(SHARED_MANAGED_CONFIG_SHOW_SCREENSHOTS) else false

        if (toolbar != null)
            toolbar!!.title = newAppName

        sharedPrefManaged!!.edit().putString(SHARED_MANAGED_CONFIG_APP_NAME, newAppName).apply()
        if (showScreenshotsFolder != (sharedPrefManaged!!.getBoolean(
                SHARED_MANAGED_CONFIG_SHOW_SCREENSHOTS,
                false
            ))
        ) {
            sharedPrefManaged!!.edit()
                .putBoolean(SHARED_MANAGED_CONFIG_SHOW_SCREENSHOTS, showScreenshotsFolder).apply()
            refreshItems()
        }

        startManagedConfigValuesReciever()
    }

    /**
     * Async Task to download file from URL
     */
    @Suppress(
        "NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS",
        "RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS"
    )
    private class DownloadFile : AsyncTask<String?, String?, String>() {
        private var progressDialog: ProgressDialog? = null
        private var fileName: String? = null
        private var folder: String? = null
        override fun onPreExecute() {
            super.onPreExecute()
            mActivity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val dir = File(
                Environment.getExternalStorageDirectory()
                    .path + File.separator + "esperfiles" + File.separator + "Synced Files"
            )
            if (dir.isDirectory) {
                val children = dir.list()
                for (i in children.indices) {
                    File(dir, children[i]).delete()
                }
            }
            progressDialog = ProgressDialog(mContext)
            progressDialog!!.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            progressDialog!!.setCancelable(false)
            progressDialog!!.show()
        }

        override fun doInBackground(vararg f_url: String?): String {
            var count: Int
            try {
                val url = URL(f_url[0])
                val connection = url.openConnection()
                connection.connect()
                val lengthOfFile = connection.contentLength
                val input: InputStream = BufferedInputStream(url.openStream(), 8192)
                fileName = f_url[0]!!.substring(f_url[0]!!.lastIndexOf('/') + 1, f_url[0]!!.length)
                val directory = File(
                    Environment.getExternalStorageDirectory()
                        .path + File.separator + "esperfiles" + File.separator + "Synced Files" + File.separator
                )
                if (!directory.exists()) {
                    directory.mkdirs()
                }
                val output: OutputStream = FileOutputStream(
                    Environment.getExternalStorageDirectory()
                        .path + File.separator + "esperfiles" + File.separator + "Synced Files" + File.separator + "temp.zip"
                )
                val data = ByteArray(1024)
                var total: Long = 0
                while (input.read(data).also { count = it } != -1) {
                    total += count.toLong()
                    publishProgress("" + (total * 100 / lengthOfFile).toInt())
                    Log.d("DEBUG", "Progress: " + (total * 100 / lengthOfFile).toInt())
                    output.write(data, 0, count)
                }
                output.flush()
                output.close()
                input.close()
                return "Downloaded at: $folder$fileName"
            } catch (e: Exception) {
                Log.e("Error: ", e.message)
            }
            return "Something went wrong"
        }

        override fun onProgressUpdate(vararg values: String?) {
            progressDialog!!.progress = values[0]!!.toInt()
        }

        override fun onPostExecute(message: String) {
            FileUtils.unzipFromSync(
                mContext,
                Environment.getExternalStorageDirectory()
                    .path + File.separator + "esperfiles" + File.separator + "Synced Files" + File.separator + "temp.zip",
                Environment.getExternalStorageDirectory()
                    .path + File.separator + "esperfiles" + File.separator + "Synced Files" + File.separator
            )
            val file = File(
                Environment.getExternalStorageDirectory()
                    .path + File.separator + "esperfiles" + File.separator + "Synced Files" + File.separator + "temp.zip"
            )
            file.delete()
            progressDialog!!.dismiss()
            mActivity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}