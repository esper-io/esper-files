@file:Suppress("DEPRECATION")

package io.esper.files.activity

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.ConnectivityManager
import android.os.*
import android.os.Build.VERSION.SDK_INT
import android.os.StrictMode.VmPolicy
import android.text.InputType
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.alespero.expandablecardview.ExpandableCardView
import com.downloader.OnDownloadListener
import com.downloader.PRDownloader
import com.downloader.PRDownloaderConfig
import com.example.flatdialoglibrary.dialog.FlatDialog
import com.ferfalk.simplesearchview.SimpleSearchView
import com.ferfalk.simplesearchview.utils.DimensUtils.convertDpToPx
import com.shashank.sony.fancytoastlib.FancyToast
import com.tonyodev.storagegrapher.Storage
import com.tonyodev.storagegrapher.StorageGraphBar
import com.tonyodev.storagegrapher.StorageVolume
import com.tonyodev.storagegrapher.widget.StorageGraphView
import hendrawd.storageutil.library.StorageUtil
import io.esper.files.R
import io.esper.files.constants.Constants.BfilSyncFolder
import io.esper.files.constants.Constants.ExternalRootFolder
import io.esper.files.constants.Constants.InternalCheckerString
import io.esper.files.constants.Constants.InternalRootFolder
import io.esper.files.constants.Constants.MainActivityTag
import io.esper.files.constants.Constants.SHARED_EXTERNAL_STORAGE_VALUE
import io.esper.files.constants.Constants.SHARED_LAST_PREFERRED_STORAGE
import io.esper.files.constants.Constants.SHARED_MANAGED_CONFIG_APP_NAME
import io.esper.files.constants.Constants.SHARED_MANAGED_CONFIG_DELETION_ALLOWED
import io.esper.files.constants.Constants.SHARED_MANAGED_CONFIG_FILE_FORMATS_AUDIO_VIDEO
import io.esper.files.constants.Constants.SHARED_MANAGED_CONFIG_FILE_FORMATS_IMAGE
import io.esper.files.constants.Constants.SHARED_MANAGED_CONFIG_FILE_FORMATS_OTHER
import io.esper.files.constants.Constants.SHARED_MANAGED_CONFIG_KIOSK_SLIDESHOW
import io.esper.files.constants.Constants.SHARED_MANAGED_CONFIG_KIOSK_SLIDESHOW_DELAY
import io.esper.files.constants.Constants.SHARED_MANAGED_CONFIG_KIOSK_SLIDESHOW_IMAGE_STRATEGY
import io.esper.files.constants.Constants.SHARED_MANAGED_CONFIG_KIOSK_SLIDESHOW_PATH
import io.esper.files.constants.Constants.SHARED_MANAGED_CONFIG_SHOW_SCREENSHOTS
import io.esper.files.constants.Constants.SHARED_MANAGED_CONFIG_USE_INBUILT_AUDIO_VIDEO
import io.esper.files.constants.Constants.SHARED_MANAGED_CONFIG_USE_INBUILT_IMAGE
import io.esper.files.constants.Constants.SHARED_MANAGED_CONFIG_USE_INBUILT_PDF
import io.esper.files.constants.Constants.SHARED_MANAGED_CONFIG_VALUES
import io.esper.files.constants.Constants.imageFileFormats
import io.esper.files.constants.Constants.otherFileFormats
import io.esper.files.constants.Constants.SHARED_MANAGED_SYNC_SERVER_IP
import io.esper.files.constants.Constants.storagePermission
import io.esper.files.constants.Constants.videoAudioFileFormats
import io.esper.files.fragment.ListItemsFragment
import io.esper.files.service.AutoInstallService
import io.esper.files.util.FileUtils
import io.esper.files.util.InstallUtil
import org.greenrobot.eventbus.EventBus
import java.io.*
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.util.regex.Pattern
import kotlin.math.abs


class MainActivity : AppCompatActivity(), ListItemsFragment.UpdateViewOnScroll {

    private var changeInValue: Boolean = false
    private var expandableCard: ExpandableCardView? = null
    private var isSdCardStorageGraphViewPopulated: Boolean = false
    private var toolbar: Toolbar? = null
    private var searched: Boolean = false
    private var sharedPref: SharedPreferences? = null
    private var sharedPrefManaged: SharedPreferences? = null
    private var sdCardAvailable: Boolean = false
    private var externalStoragePaths: Array<String>? = null
    private var mCurrentPath: String = InternalRootFolder
    private var searchView: SimpleSearchView? = null
    private var internalStorageGraphView: StorageGraphView? = null
    private var sdCardStorageGraphView: StorageGraphView? = null
    private val managedAudioVideoList = ArrayList<String>()
    private val managedImageList = ArrayList<String>()
    private val managedOtherList = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init()
    }

    private fun init() {
        //Used for Glide Image Loader
        val builder = VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

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

        expandableCard = findViewById(R.id.expandableCard)

        internalStorageGraphView = findViewById(R.id.storageView)
        sdCardStorageGraphView = findViewById(R.id.sdCardStorageView)

        setInternalStorageGraphView()

        sharedPref = getSharedPreferences(SHARED_LAST_PREFERRED_STORAGE, Context.MODE_PRIVATE)

        externalStoragePaths = StorageUtil.getStorageDirectories(this)

        if (externalStoragePaths!!.size > 1 && ContextCompat.getExternalFilesDirs(
                this,
                null
            ).size >= 2
        )
            sdCardAvailable = true

        if (sdCardAvailable)
            if (sharedPref!!.getBoolean(SHARED_EXTERNAL_STORAGE_VALUE, false)) {
                mCurrentPath = if (externalStoragePaths!![0] == InternalCheckerString)
                    externalStoragePaths!![1] + ExternalRootFolder else externalStoragePaths!![0] + ExternalRootFolder

                setSdCardStorageGraphView()
                internalStorageGraphView!!.visibility = View.GONE
                sdCardStorageGraphView!!.visibility = View.VISIBLE
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
//            refreshItems()
            swipeContainer.isRefreshing = false
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        val item = menu!!.findItem(R.id.toggle_switch)
        item.setActionView(R.layout.actionbar_service_toggle)

        val mySwitch = item.actionView.findViewById<SwitchCompat>(R.id.switchForActionBar)
        mySwitch.visibility = if (sdCardAvailable) View.VISIBLE else View.GONE

        if (sharedPref!!.getBoolean(SHARED_EXTERNAL_STORAGE_VALUE, false)) {
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
                if (!isSdCardStorageGraphViewPopulated) {
                    setSdCardStorageGraphView()
                } else {
                    internalStorageGraphView!!.visibility = View.GONE
                    sdCardStorageGraphView!!.visibility = View.VISIBLE
                }
            } else {
                mySwitch.text = getString(R.string.internal_storage)
                storageExt = false
                mCurrentPath = InternalRootFolder
                internalStorageGraphView!!.visibility = View.VISIBLE
                sdCardStorageGraphView!!.visibility = View.GONE
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
                syncFunction(
                        sharedPrefManaged!!.getString(SHARED_MANAGED_SYNC_SERVER_IP, null).toString()
                )
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun syncFunction(syncServerIP: String) {
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
        else if (isServerReachable("http://$syncServerIP:51515/send/transfer")) {
            syncFile(syncServerIP)
        } else {
            showManualEntryDialog()
        }
    }

    private fun showManualEntryDialog() {
        val flatDialog = FlatDialog(this@MainActivity)
        flatDialog.setCanceledOnTouchOutside(true)
        flatDialog.setTitle("Enter Sync Server IP")
                .setIcon(R.drawable.ip)
                .setFirstTextFieldInputType(InputType.TYPE_CLASS_PHONE)
                .setSubtitle("Ask your branch administrator for the IP.")
                .setFirstTextFieldHint("192.168.X.X")
                .setFirstButtonText("Connect")
                .withFirstButtonListner {
                    if (isValidIPAddress(flatDialog.firstTextField.toString())) {
                        syncFunction(flatDialog.firstTextField.toString())
                        flatDialog.dismiss()
                    } else {
                        flatDialog.setFirstTextFieldBorderColor(Color.RED)
                        Toast.makeText(this, "Enter Properly Formatted IP", Toast.LENGTH_SHORT)
                                .show()
                    }
                }
                .show()
    }

    private fun syncFile(syncServerIP: String) {

        mCurrentPath = InternalRootFolder
        refreshItems()

        val progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Syncing Files")
        progressDialog.setMessage("Please wait...")
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        progressDialog.setCancelable(false)
        progressDialog.show()
        PRDownloader.initialize(applicationContext, PRDownloaderConfig.newBuilder()
                .setReadTimeout(30000)
                .setConnectTimeout(30000)
                .build())
        PRDownloader.download(
                "http://$syncServerIP:51515/send/transfer",
                BfilSyncFolder,
                "bfil_temp.zip"
        )
                .build()
                .setOnProgressListener {
                    progressDialog.progress = (it.currentBytes * 100 / it.totalBytes).toInt()
                }
                .start(object : OnDownloadListener {
                    @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
                    override fun onDownloadComplete() {
                        val dir = File(BfilSyncFolder)
                        if (dir.isDirectory) {
                            val children = dir.list()
                            for (i in children.indices) {
                                if (File(dir, children[i]).name != "bfil_temp.zip")
                                    File(dir, children[i]).delete()
                            }
                        }
                        FileUtils.Decompress(
                                this@MainActivity,
                                BfilSyncFolder + "bfil_temp.zip",
                                BfilSyncFolder,
                                true,
                                this@MainActivity
                        ).execute()
//                        val file = File(BfilSyncFolder + "bfil_temp.zip")
//                        file.delete()
                        progressDialog.dismiss()
                        sharedPrefManaged!!.edit()
                                .putString(SHARED_MANAGED_SYNC_SERVER_IP, syncServerIP)
                                .apply()
                        refreshItems()
                        FancyToast.makeText(applicationContext, "Sync Completed", FancyToast.LENGTH_LONG, FancyToast.SUCCESS, false).show()
                    }

                    override fun onError(error: com.downloader.Error?) {
                        FancyToast.makeText(applicationContext, "Server Connection Lost, Contact Administrator", FancyToast.LENGTH_LONG, FancyToast.ERROR, false).show()
                        progressDialog.dismiss()
                    }
                })

        Handler().postDelayed({
            val file = File(BfilSyncFolder + "bfil_temp.zip")
            file.delete()
        }, 15000)

    }

    private fun isValidIPAddress(ip: String): Boolean {
        val reg0To255 = ("(\\d{1,2}|(0|1)\\" + "d{2}|2[0-4]\\d|25[0-5])")
        val regex = (reg0To255 + "\\."
                + reg0To255 + "\\."
                + reg0To255 + "\\."
                + reg0To255)
        val p = Pattern.compile(regex)
        val m = p.matcher(ip)
        return m.matches()
    }

    @Suppress("SameParameterValue")
    private fun isServerReachable(serverUrl: String): Boolean {
        val connMan = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo = connMan.activeNetworkInfo
        return if (netInfo != null && netInfo.isConnected) {
            try {
                val urlServer = URL(serverUrl)
                val urlConn = urlServer.openConnection() as HttpURLConnection
                urlConn.connectTimeout = 1000 //<- 1.5 Seconds Timeout
                urlConn.connect()
                urlConn.responseCode == 200
            } catch (e1: MalformedURLException) {
                false
            } catch (e: IOException) {
                false
            }
        } else false
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

    private fun refreshItems() {
        if (expandableCard!!.isExpanded)
            expandableCard!!.collapse()
        if (searchView != null)
            searchView!!.closeSearch()
        EventBus.getDefault().post(ListItemsFragment.RefreshStackEvent(true))
        initFileListFragment()
        hideKeyboard(activity = this)
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
            expandableCard!!.isExpanded ->
                expandableCard!!.collapse()
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
//        //(default: Files)
//        "app_name": "Company Name",
//        //(default: false)
//        "show_screenshots_folder": true,
//        //(default: true)
//        "deletion_allowed": true,
//        //(default: false)
//        "kiosk_slideshow": true,
//        //(default: "/storage/emulated/0/esperfiles/")
//        "kiosk_slideshow_path": "/storage/emulated/0/esperfiles/folder_name",
//        //(default: 3 sec)
//        "kiosk_slideshow_delay": 3,
//        //(default: 1 -> 1. Glide Image Strategy, 2. Custom Image Strategy)
//        "kiosk_slideshow_image_strategy": 1,
//        //(default : "*" //All Allowed)
//        "image":["jpg", "jpeg"],
//        //(default : "*" //All Allowed)
//        "audio_video":["mp4", "mp3"],
//        //(default : "*" //All Allowed)
//        "other":["pdf", "xls", "ppt"],
//        //(default: true)
//        "inbuilt_pdf": true,
//        //(default: true)
//        "inbuilt_audio_video": true,
//        //(default: true)
//        "inbuilt_image": true
//        //(default: null)
//        "sync_server_ip": "192.168.1.2"
//    }

    private fun startManagedConfigValuesReceiver() {
        val myRestrictionsMgr =
            getSystemService(Context.RESTRICTIONS_SERVICE) as RestrictionsManager
        val restrictionsFilter = IntentFilter(Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED)

        val restrictionsReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {

                managedAudioVideoList.clear()
                managedImageList.clear()
                managedOtherList.clear()

                val appRestrictions = myRestrictionsMgr.applicationRestrictions

                val newAppName = if (appRestrictions.containsKey(SHARED_MANAGED_CONFIG_APP_NAME))
                    appRestrictions.getString(SHARED_MANAGED_CONFIG_APP_NAME)
                        .toString() else getString(R.string.app_name)

                val showScreenshotsFolder =
                    if (appRestrictions.containsKey(SHARED_MANAGED_CONFIG_SHOW_SCREENSHOTS))
                        appRestrictions.getBoolean(SHARED_MANAGED_CONFIG_SHOW_SCREENSHOTS) else false

                val deletionAllowed =
                    if (appRestrictions.containsKey(SHARED_MANAGED_CONFIG_DELETION_ALLOWED))
                        appRestrictions.getBoolean(SHARED_MANAGED_CONFIG_DELETION_ALLOWED) else true

                val kioskSlideshow =
                    if (appRestrictions.containsKey(SHARED_MANAGED_CONFIG_KIOSK_SLIDESHOW))
                        appRestrictions.getBoolean(SHARED_MANAGED_CONFIG_KIOSK_SLIDESHOW) else false

                val kioskSlideshowPath =
                    if (appRestrictions.containsKey(SHARED_MANAGED_CONFIG_KIOSK_SLIDESHOW_PATH))
                        appRestrictions.getString(SHARED_MANAGED_CONFIG_KIOSK_SLIDESHOW_PATH)
                            .toString() else InternalRootFolder

                val kioskSlideshowDelay =
                    if (appRestrictions.containsKey(SHARED_MANAGED_CONFIG_KIOSK_SLIDESHOW_DELAY))
                        appRestrictions.getInt(SHARED_MANAGED_CONFIG_KIOSK_SLIDESHOW_DELAY) else 3

                val kioskSlideshowImageStrategy =
                    if (appRestrictions.containsKey(
                            SHARED_MANAGED_CONFIG_KIOSK_SLIDESHOW_IMAGE_STRATEGY
                        )
                    )
                        appRestrictions.getInt(SHARED_MANAGED_CONFIG_KIOSK_SLIDESHOW_IMAGE_STRATEGY) else 1


                val fileFormatsAudioVideo =
                    if (appRestrictions.containsKey(SHARED_MANAGED_CONFIG_FILE_FORMATS_AUDIO_VIDEO))
                        appRestrictions.getStringArray(
                            SHARED_MANAGED_CONFIG_FILE_FORMATS_AUDIO_VIDEO
                        ) else null

                val fileFormatsImage =
                    if (appRestrictions.containsKey(SHARED_MANAGED_CONFIG_FILE_FORMATS_IMAGE))
                        appRestrictions.getStringArray(SHARED_MANAGED_CONFIG_FILE_FORMATS_IMAGE) else null

                val fileFormatsOther =
                    if (appRestrictions.containsKey(SHARED_MANAGED_CONFIG_FILE_FORMATS_OTHER))
                        appRestrictions.getStringArray(SHARED_MANAGED_CONFIG_FILE_FORMATS_OTHER) else null

                val inBuiltPDF =
                    if (appRestrictions.containsKey(SHARED_MANAGED_CONFIG_USE_INBUILT_PDF))
                        appRestrictions.getBoolean(SHARED_MANAGED_CONFIG_USE_INBUILT_PDF) else true

                val inBuiltAudioVideo =
                    if (appRestrictions.containsKey(SHARED_MANAGED_CONFIG_USE_INBUILT_AUDIO_VIDEO))
                        appRestrictions.getBoolean(SHARED_MANAGED_CONFIG_USE_INBUILT_AUDIO_VIDEO) else true

                val inBuiltImage =
                    if (appRestrictions.containsKey(SHARED_MANAGED_CONFIG_USE_INBUILT_IMAGE))
                        appRestrictions.getBoolean(SHARED_MANAGED_CONFIG_USE_INBUILT_IMAGE) else true

                if (fileFormatsAudioVideo != null) {
                    for (i in fileFormatsAudioVideo.indices) {
                        if (fileFormatsAudioVideo[i] in managedAudioVideoList)
                            changeInValue = false
                        else {
                            changeInValue = true
                            managedAudioVideoList.add(fileFormatsAudioVideo[i])
                        }
                    }
                }

                if (fileFormatsImage != null) {
                    for (i in fileFormatsImage.indices) {
                        if (fileFormatsImage[i] in managedImageList)
                            changeInValue = false
                        else {
                            changeInValue = true
                            managedImageList.add(fileFormatsImage[i])
                        }
                    }
                }

                if (fileFormatsOther != null) {
                    for (i in fileFormatsOther.indices) {
                        if (fileFormatsOther[i] in managedOtherList)
                            changeInValue = false
                        else {
                            changeInValue = true
                            managedOtherList.add(fileFormatsOther[i])
                        }
                    }
                }

                sharedPrefManaged!!.edit().putBoolean(
                    SHARED_MANAGED_CONFIG_KIOSK_SLIDESHOW,
                    kioskSlideshow
                ).apply()
                sharedPrefManaged!!.edit().putString(
                    SHARED_MANAGED_CONFIG_KIOSK_SLIDESHOW_PATH,
                    kioskSlideshowPath
                ).apply()
                sharedPrefManaged!!.edit().putInt(
                    SHARED_MANAGED_CONFIG_KIOSK_SLIDESHOW_DELAY,
                    kioskSlideshowDelay
                ).apply()
                sharedPrefManaged!!.edit().putInt(
                    SHARED_MANAGED_CONFIG_KIOSK_SLIDESHOW_IMAGE_STRATEGY,
                    kioskSlideshowImageStrategy
                ).apply()

                if (sharedPrefManaged!!.getBoolean(SHARED_MANAGED_CONFIG_KIOSK_SLIDESHOW, false))
                    startActivity(Intent(this@MainActivity, SlideshowActivity::class.java))

                val syncServerIP = if (appRestrictions.containsKey(SHARED_MANAGED_SYNC_SERVER_IP))
                    appRestrictions.getString(SHARED_MANAGED_SYNC_SERVER_IP)
                            .toString() else sharedPrefManaged!!.getString(
                        SHARED_MANAGED_SYNC_SERVER_IP,
                        null
                ).toString()

                if (toolbar != null)
                    toolbar!!.title = newAppName

                sharedPrefManaged!!.edit().putString(SHARED_MANAGED_CONFIG_APP_NAME, newAppName)
                    .apply()
                sharedPrefManaged!!.edit()
                    .putBoolean(SHARED_MANAGED_CONFIG_DELETION_ALLOWED, deletionAllowed).apply()
                sharedPrefManaged!!.edit().putString(SHARED_MANAGED_SYNC_SERVER_IP, syncServerIP).apply()

                if (showScreenshotsFolder != (sharedPrefManaged!!.getBoolean(
                        SHARED_MANAGED_CONFIG_SHOW_SCREENSHOTS,
                        false
                    ))
                ) {
                    sharedPrefManaged!!.edit()
                        .putBoolean(
                            SHARED_MANAGED_CONFIG_SHOW_SCREENSHOTS,
                            showScreenshotsFolder
                        )
                        .apply()
//                    refreshItems()
                }

                if (fileFormatsAudioVideo != null) {
                    if ("*" in fileFormatsAudioVideo)
                        sharedPrefManaged!!.edit().putString(
                            SHARED_MANAGED_CONFIG_FILE_FORMATS_AUDIO_VIDEO,
                            videoAudioFileFormats.toString()
                        ).apply()
                    else
                        sharedPrefManaged!!.edit().putString(
                            SHARED_MANAGED_CONFIG_FILE_FORMATS_AUDIO_VIDEO,
                            managedAudioVideoList.toString()
                        ).apply()
                } else
                    sharedPrefManaged!!.edit().putString(
                        SHARED_MANAGED_CONFIG_FILE_FORMATS_AUDIO_VIDEO,
                        videoAudioFileFormats.toString()
                    ).apply()

                if (fileFormatsImage != null) {
                    if ("*" in fileFormatsImage)
                        sharedPrefManaged!!.edit().putString(
                            SHARED_MANAGED_CONFIG_FILE_FORMATS_IMAGE,
                            imageFileFormats.toString()
                        ).apply()
                    else
                        sharedPrefManaged!!.edit().putString(
                            SHARED_MANAGED_CONFIG_FILE_FORMATS_IMAGE,
                            managedImageList.toString()
                        ).apply()
                } else
                    sharedPrefManaged!!.edit().putString(
                        SHARED_MANAGED_CONFIG_FILE_FORMATS_IMAGE,
                        imageFileFormats.toString()
                    ).apply()

                if (fileFormatsOther != null) {
                    if ("*" in fileFormatsOther)
                        sharedPrefManaged!!.edit().putString(
                            SHARED_MANAGED_CONFIG_FILE_FORMATS_OTHER,
                            otherFileFormats.toString()
                        ).apply()
                    else
                        sharedPrefManaged!!.edit().putString(
                            SHARED_MANAGED_CONFIG_FILE_FORMATS_OTHER,
                            managedOtherList.toString()
                        ).apply()
                } else
                    sharedPrefManaged!!.edit().putString(
                        SHARED_MANAGED_CONFIG_FILE_FORMATS_OTHER,
                        otherFileFormats.toString()
                    ).apply()

                if (inBuiltPDF != (sharedPrefManaged!!.getBoolean(
                        SHARED_MANAGED_CONFIG_USE_INBUILT_PDF,
                        true
                    ))
                ) {
                    sharedPrefManaged!!.edit()
                        .putBoolean(
                            SHARED_MANAGED_CONFIG_USE_INBUILT_PDF,
                            inBuiltPDF
                        )
                        .apply()
                }

                if (inBuiltAudioVideo != (sharedPrefManaged!!.getBoolean(
                        SHARED_MANAGED_CONFIG_USE_INBUILT_AUDIO_VIDEO,
                        true
                    ))
                ) {
                    sharedPrefManaged!!.edit()
                        .putBoolean(
                            SHARED_MANAGED_CONFIG_USE_INBUILT_AUDIO_VIDEO,
                            inBuiltAudioVideo
                        )
                        .apply()
                }

                if (inBuiltImage != (sharedPrefManaged!!.getBoolean(
                        SHARED_MANAGED_CONFIG_USE_INBUILT_IMAGE,
                        true
                    ))
                ) {
                    sharedPrefManaged!!.edit()
                        .putBoolean(
                            SHARED_MANAGED_CONFIG_USE_INBUILT_IMAGE,
                            inBuiltImage
                        )
                        .apply()
                }

                if (changeInValue && expandableCard != null)
                    refreshItems()
            }
        }
        registerReceiver(restrictionsReceiver, restrictionsFilter)
    }

    private fun getManagedConfigValues() {

        managedAudioVideoList.clear()
        managedImageList.clear()
        managedOtherList.clear()

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

        val deletionAllowed =
            if (restrictionsBundle.containsKey(SHARED_MANAGED_CONFIG_DELETION_ALLOWED))
                restrictionsBundle.getBoolean(SHARED_MANAGED_CONFIG_DELETION_ALLOWED) else true

        val kioskSlideshow =
            if (restrictionsBundle.containsKey(SHARED_MANAGED_CONFIG_KIOSK_SLIDESHOW))
                restrictionsBundle.getBoolean(SHARED_MANAGED_CONFIG_KIOSK_SLIDESHOW) else false

        val kioskSlideshowPath =
            if (restrictionsBundle.containsKey(SHARED_MANAGED_CONFIG_KIOSK_SLIDESHOW_PATH))
                restrictionsBundle.getString(SHARED_MANAGED_CONFIG_KIOSK_SLIDESHOW_PATH)
                    .toString() else InternalRootFolder

        val kioskSlideshowDelay =
            if (restrictionsBundle.containsKey(SHARED_MANAGED_CONFIG_KIOSK_SLIDESHOW_DELAY))
                restrictionsBundle.getInt(SHARED_MANAGED_CONFIG_KIOSK_SLIDESHOW_DELAY) else 3

        val kioskSlideshowImageStrategy =
            if (restrictionsBundle.containsKey(SHARED_MANAGED_CONFIG_KIOSK_SLIDESHOW_IMAGE_STRATEGY))
                restrictionsBundle.getInt(SHARED_MANAGED_CONFIG_KIOSK_SLIDESHOW_IMAGE_STRATEGY) else 1

        val fileFormatsAudioVideo =
            if (restrictionsBundle.containsKey(SHARED_MANAGED_CONFIG_FILE_FORMATS_AUDIO_VIDEO))
                restrictionsBundle.getStringArray(SHARED_MANAGED_CONFIG_FILE_FORMATS_AUDIO_VIDEO) else null

        val fileFormatsImage =
            if (restrictionsBundle.containsKey(SHARED_MANAGED_CONFIG_FILE_FORMATS_IMAGE))
                restrictionsBundle.getStringArray(SHARED_MANAGED_CONFIG_FILE_FORMATS_IMAGE) else null

        val fileFormatsOther =
            if (restrictionsBundle.containsKey(SHARED_MANAGED_CONFIG_FILE_FORMATS_OTHER))
                restrictionsBundle.getStringArray(SHARED_MANAGED_CONFIG_FILE_FORMATS_OTHER) else null

        val inBuiltPDF =
            if (restrictionsBundle.containsKey(SHARED_MANAGED_CONFIG_USE_INBUILT_PDF))
                restrictionsBundle.getBoolean(SHARED_MANAGED_CONFIG_USE_INBUILT_PDF) else true

        val inBuiltAudioVideo =
            if (restrictionsBundle.containsKey(SHARED_MANAGED_CONFIG_USE_INBUILT_AUDIO_VIDEO))
                restrictionsBundle.getBoolean(SHARED_MANAGED_CONFIG_USE_INBUILT_AUDIO_VIDEO) else true

        val inBuiltImage =
            if (restrictionsBundle.containsKey(SHARED_MANAGED_CONFIG_USE_INBUILT_IMAGE))
                restrictionsBundle.getBoolean(SHARED_MANAGED_CONFIG_USE_INBUILT_IMAGE) else true

        if (fileFormatsAudioVideo != null) {
            for (i in fileFormatsAudioVideo.indices) {
                if (fileFormatsAudioVideo[i] in managedAudioVideoList)
                    changeInValue = false
                else {
                    changeInValue = true
                    managedAudioVideoList.add(fileFormatsAudioVideo[i])
                }
            }
        }

        if (fileFormatsImage != null) {
            for (i in fileFormatsImage.indices) {
                if (fileFormatsImage[i] in managedImageList)
                    changeInValue = false
                else {
                    changeInValue = true
                    managedImageList.add(fileFormatsImage[i])
                }
            }
        }

        if (fileFormatsOther != null) {
            for (i in fileFormatsOther.indices) {
                if (fileFormatsOther[i] in managedOtherList)
                    changeInValue = false
                else {
                    changeInValue = true
                    managedOtherList.add(fileFormatsOther[i])
                }
            }
        }

        sharedPrefManaged!!.edit().putBoolean(SHARED_MANAGED_CONFIG_KIOSK_SLIDESHOW, kioskSlideshow)
            .apply()
        sharedPrefManaged!!.edit().putString(
            SHARED_MANAGED_CONFIG_KIOSK_SLIDESHOW_PATH,
            kioskSlideshowPath
        ).apply()
        sharedPrefManaged!!.edit().putInt(
            SHARED_MANAGED_CONFIG_KIOSK_SLIDESHOW_DELAY,
            kioskSlideshowDelay
        ).apply()
        sharedPrefManaged!!.edit().putInt(
            SHARED_MANAGED_CONFIG_KIOSK_SLIDESHOW_IMAGE_STRATEGY,
            kioskSlideshowImageStrategy
        ).apply()
        if (sharedPrefManaged!!.getBoolean(SHARED_MANAGED_CONFIG_KIOSK_SLIDESHOW, false))
            startActivity(Intent(this@MainActivity, SlideshowActivity::class.java))

        val syncServerIP = if (restrictionsBundle.containsKey(SHARED_MANAGED_SYNC_SERVER_IP))
            restrictionsBundle.getString(SHARED_MANAGED_SYNC_SERVER_IP)
                    .toString() else sharedPrefManaged!!.getString(
                SHARED_MANAGED_SYNC_SERVER_IP,
                null
        ).toString()

        if (toolbar != null)
            toolbar!!.title = newAppName

        sharedPrefManaged!!.edit().putString(SHARED_MANAGED_CONFIG_APP_NAME, newAppName).apply()
        sharedPrefManaged!!.edit()
            .putBoolean(SHARED_MANAGED_CONFIG_DELETION_ALLOWED, deletionAllowed).apply()
        sharedPrefManaged!!.edit().putString(SHARED_MANAGED_SYNC_SERVER_IP, syncServerIP).apply()
        if (showScreenshotsFolder != (sharedPrefManaged!!.getBoolean(
                SHARED_MANAGED_CONFIG_SHOW_SCREENSHOTS,
                false
            ))
        ) {
            sharedPrefManaged!!.edit()
                .putBoolean(SHARED_MANAGED_CONFIG_SHOW_SCREENSHOTS, showScreenshotsFolder).apply()
//            refreshItems()
        }

        if (fileFormatsAudioVideo != null) {
            if ("*" in fileFormatsAudioVideo)
                sharedPrefManaged!!.edit().putString(
                    SHARED_MANAGED_CONFIG_FILE_FORMATS_AUDIO_VIDEO,
                    videoAudioFileFormats.toString()
                ).apply()
            else
                sharedPrefManaged!!.edit().putString(
                    SHARED_MANAGED_CONFIG_FILE_FORMATS_AUDIO_VIDEO,
                    managedAudioVideoList.toString()
                ).apply()
        } else
            sharedPrefManaged!!.edit().putString(
                SHARED_MANAGED_CONFIG_FILE_FORMATS_AUDIO_VIDEO,
                videoAudioFileFormats.toString()
            ).apply()

        if (fileFormatsImage != null) {
            if ("*" in fileFormatsImage)
                sharedPrefManaged!!.edit().putString(
                    SHARED_MANAGED_CONFIG_FILE_FORMATS_IMAGE,
                    imageFileFormats.toString()
                ).apply()
            else
                sharedPrefManaged!!.edit().putString(
                    SHARED_MANAGED_CONFIG_FILE_FORMATS_IMAGE,
                    managedImageList.toString()
                ).apply()
        } else
            sharedPrefManaged!!.edit()
                .putString(SHARED_MANAGED_CONFIG_FILE_FORMATS_IMAGE, imageFileFormats.toString())
                .apply()

        if (fileFormatsOther != null) {
            if ("*" in fileFormatsOther)
                sharedPrefManaged!!.edit().putString(
                    SHARED_MANAGED_CONFIG_FILE_FORMATS_OTHER,
                    otherFileFormats.toString()
                ).apply()
            else
                sharedPrefManaged!!.edit().putString(
                    SHARED_MANAGED_CONFIG_FILE_FORMATS_OTHER,
                    managedOtherList.toString()
                ).apply()
        } else
            sharedPrefManaged!!.edit()
                .putString(SHARED_MANAGED_CONFIG_FILE_FORMATS_OTHER, otherFileFormats.toString())
                .apply()

        if (inBuiltPDF != (sharedPrefManaged!!.getBoolean(
                SHARED_MANAGED_CONFIG_USE_INBUILT_PDF,
                true
            ))
        ) {
            sharedPrefManaged!!.edit()
                .putBoolean(
                    SHARED_MANAGED_CONFIG_USE_INBUILT_PDF,
                    inBuiltPDF
                )
                .apply()
        }

        if (inBuiltAudioVideo != (sharedPrefManaged!!.getBoolean(
                SHARED_MANAGED_CONFIG_USE_INBUILT_AUDIO_VIDEO,
                true
            ))
        ) {
            sharedPrefManaged!!.edit()
                .putBoolean(
                    SHARED_MANAGED_CONFIG_USE_INBUILT_AUDIO_VIDEO,
                    inBuiltAudioVideo
                )
                .apply()
        }

        if (inBuiltImage != (sharedPrefManaged!!.getBoolean(
                SHARED_MANAGED_CONFIG_USE_INBUILT_IMAGE,
                true
            ))
        ) {
            sharedPrefManaged!!.edit()
                .putBoolean(
                    SHARED_MANAGED_CONFIG_USE_INBUILT_IMAGE,
                    inBuiltImage
                )
                .apply()
        }

        if (changeInValue && expandableCard != null)
            refreshItems()

        startManagedConfigValuesReceiver()
    }

    private fun setInternalStorageGraphView() {
        val storageVolume: StorageVolume = Storage.getPrimaryStorageVolume()!!
        val totalBar = StorageGraphBar(
            storageVolume.totalSpace.toFloat(),
            Color.GRAY,
            "Total",
            Storage.getFormattedStorageAmount(this, storageVolume.totalSpace)
        )
        val usedBar: StorageGraphBar
        if (Storage.getStoragePercentage(
                abs(storageVolume.usedSpace),
                storageVolume.totalSpace
            ) < 90
        ) {
            usedBar = StorageGraphBar(
                Storage.getStoragePercentage(
                    abs(storageVolume.usedSpace),
                    storageVolume.totalSpace
                ),
                ContextCompat.getColor(this, R.color.green),
                "Used",
                Storage.getFormattedStorageAmount(
                    this,
                    abs(storageVolume.usedSpace)
                )
            )
        } else {
            usedBar = StorageGraphBar(
                Storage.getStoragePercentage(
                    abs(storageVolume.usedSpace),
                    storageVolume.totalSpace
                ),
                ContextCompat.getColor(this, R.color.red),
                "Used",
                Storage.getFormattedStorageAmount(
                    this,
                    abs(storageVolume.usedSpace)
                )
            )
        }
        val freeBar = StorageGraphBar(
            storageVolume.freeSpacePercentage,
            ContextCompat.getColor(this, R.color.yellow),
            "Free",
            Storage.getFormattedStorageAmount(this, storageVolume.freeSpace)
        )
        internalStorageGraphView!!.addBars(usedBar, freeBar, totalBar)
        internalStorageGraphView!!.visibility = View.VISIBLE
        sdCardStorageGraphView!!.visibility = View.GONE
    }

    private fun setSdCardStorageGraphView() {
        val externalStoragePaths = StorageUtil.getStorageDirectories(this)
        var storageVolumeExt: StorageVolume? = null
        if (externalStoragePaths!!.size > 1)
            storageVolumeExt = if (externalStoragePaths[0] == InternalCheckerString)
                Storage.getStorageVolume(externalStoragePaths[1])
            else
                Storage.getStorageVolume(externalStoragePaths[0])

        if (storageVolumeExt != null) {
            val totalBar = StorageGraphBar(
                storageVolumeExt.totalSpace.toFloat(),
                Color.GRAY,
                "Total",
                Storage.getFormattedStorageAmount(this, storageVolumeExt.totalSpace)
            )
            val usedBar: StorageGraphBar
            if (Storage.getStoragePercentage(
                    abs(storageVolumeExt.usedSpace),
                    storageVolumeExt.totalSpace
                ) < 90
            ) {
                usedBar = StorageGraphBar(
                    Storage.getStoragePercentage(
                        abs(storageVolumeExt.usedSpace),
                        storageVolumeExt.totalSpace
                    ),
                    ContextCompat.getColor(this, R.color.green),
                    "Used",
                    Storage.getFormattedStorageAmount(
                        this,
                        abs(storageVolumeExt.usedSpace)
                    )
                )
            } else {
                usedBar = StorageGraphBar(
                    Storage.getStoragePercentage(
                        abs(storageVolumeExt.usedSpace),
                        storageVolumeExt.totalSpace
                    ),
                    ContextCompat.getColor(this, R.color.red),
                    "Used",
                    Storage.getFormattedStorageAmount(
                        this,
                        abs(storageVolumeExt.usedSpace)
                    )
                )
            }
            val freeBar = StorageGraphBar(
                storageVolumeExt.freeSpacePercentage,
                ContextCompat.getColor(this, R.color.yellow),
                "Free",
                Storage.getFormattedStorageAmount(this, storageVolumeExt.freeSpace)
            )
            sdCardStorageGraphView!!.addBars(usedBar, freeBar, totalBar)
            sdCardStorageGraphView!!.visibility = View.VISIBLE
            internalStorageGraphView!!.visibility = View.GONE
            isSdCardStorageGraphViewPopulated = true
        } else
            sdCardStorageGraphView!!.visibility = View.GONE
    }

    override fun verticalScroll() {
        if (expandableCard!!.isExpanded)
            expandableCard!!.collapse()
    }

    private fun hideKeyboard(activity: Activity) {
        val imm = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        var view = activity.currentFocus
        if (view == null)
            view = View(activity)
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

//    private fun checkPermission(): Boolean {
//        return if (SDK_INT >= Build.VERSION_CODES.R) {
//            Environment.isExternalStorageManager()
//        } else {
//            ContextCompat.checkSelfPermission(
//                this,
//                Manifest.permission.WRITE_EXTERNAL_STORAGE
//            ) == PackageManager.PERMISSION_GRANTED &&
//                    ContextCompat.checkSelfPermission(
//                        this,
//                        Manifest.permission.READ_EXTERNAL_STORAGE
//                    ) == PackageManager.PERMISSION_GRANTED
//        }
//    }
//
//    private fun requestPermission() {
//        if (SDK_INT >= Build.VERSION_CODES.R) {
//            try {
//                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
//                intent.addCategory("android.intent.category.DEFAULT")
//                intent.data = Uri.parse(String.format("package:%s", applicationContext.packageName))
//                startActivityForResult(intent, 2296)
//            } catch (e: Exception) {
//                val intent = Intent()
//                intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
//                startActivityForResult(intent, 2296)
//            }
//        } else {
//            //below android 11
//            ActivityCompat.requestPermissions(
//                this, arrayOf(
//                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                    Manifest.permission.READ_EXTERNAL_STORAGE
//                ), storagePermission
//            )
//        }
//    }
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, @Nullable data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (requestCode == 2296) {
//            if (SDK_INT >= Build.VERSION_CODES.R) {
//                if (Environment.isExternalStorageManager()) {
//                    createDir()
//                } else {
//                    Toast.makeText(this, "Allow permission for storage access!", Toast.LENGTH_SHORT)
//                        .show()
//                }
//            }
//        }
//        else {
//            try {
//                if (searchView!!.onActivityResult(requestCode, resultCode, data!!)) {
//                    return
//                }
//            } catch (e: Exception) {
//                Log.e(MainActivityTag, e.message.toString())
//            } finally {
//
//            }
//        }
//    }
//
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == storagePermission) {
//            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                createDir()
//            }
//            else
//                Toast.makeText(this, "Allow permission for storage access!", Toast.LENGTH_SHORT)
//                    .show()
//        }
//    }
}