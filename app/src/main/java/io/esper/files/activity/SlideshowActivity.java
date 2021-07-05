package io.esper.files.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.esper.files.R;
import io.esper.files.listener.OnSwipeTouchListener;
import io.esper.files.model.FileItem;
import io.esper.files.strategy.image.CustomImageStrategy;
import io.esper.files.strategy.image.GlideImageStrategy;
import io.esper.files.strategy.image.ImageStrategy;

import static io.esper.files.constants.Constants.SHARED_MANAGED_CONFIG_KIOSK_SLIDESHOW_DELAY;
import static io.esper.files.constants.Constants.SHARED_MANAGED_CONFIG_KIOSK_SLIDESHOW_PATH;
import static io.esper.files.constants.Constants.SHARED_MANAGED_CONFIG_VALUES;
import static io.esper.files.constants.Constants.SlideShowActivityTag;

public class SlideshowActivity extends AppCompatActivity implements ImageStrategy.ImageStrategyCallback {

    private static final boolean REVERSE_ORDER = false;
    private static final boolean RANDOM_ORDER = false;
    private static final boolean REFRESH_FOLDER = true;
    private static final boolean PRELOAD_IMAGES = true;
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mSlideshowHandler = new Handler();
    private final Handler mHideHandler = new Handler();
    List<FileItem> fileList = new ArrayList<>();
    Handler handler = new Handler();
    Runnable runnable;
    int delay = 20000;
    private int SLIDESHOW_DELAY;
    private boolean blockPreferenceReload = false;
    private ImageStrategy imageStrategy;
    private int imagePosition;
    private boolean isRunning = false;
    private ImageView mContentView;
    private boolean mVisible;
    private final Runnable mHideRunnable = this::hide;
    private boolean userInputAllowed = true;
    private String currentPath;
    private final Runnable mSlideshowRunnable = () -> {
        int nextPos = followingImagePosition();
        nextImage(nextPos, false);
    };
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

            startSlideshow();
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_slideshow);
        mVisible = true;
        mContentView = findViewById(R.id.fullscreen_content);

        loadPreferences();
        // Stop resume from reloading the same settings
        blockPreferenceReload = true;

        // Gesture / click detection
        mContentView.setOnTouchListener(new OnSwipeTouchListener(this) {

            @Override
            public void onClick() {
//                if (checkUserInputAllowed()) {
//                    toggle();
//                }
            }

            @Override
            public void onDoubleClick() {
                if (!mVisible && checkUserInputAllowed()) {
                    toggleSlideshow();
                    if (isRunning) {
                        Toast.makeText(SlideshowActivity.this, R.string.toast_resumed, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(SlideshowActivity.this, R.string.toast_paused, Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onLongClick() {
                userInputAllowed = !userInputAllowed;
                if (checkUserInputAllowed()) {
                    Toast.makeText(SlideshowActivity.this, R.string.toast_input_allowed, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SlideshowActivity.this, R.string.toast_input_blocked, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onSwipeLeft() {
                if (checkUserInputAllowed()) {
                    nextImage(true, false);
                    startSlideshowIfFullscreen();
                }
            }

            @Override
            public void onSwipeRight() {
                if (checkUserInputAllowed()) {
                    nextImage(false, false);
                    startSlideshowIfFullscreen();
                }
            }

            @Override
            protected void onSwipeUp() {
//                if (checkUserInputAllowed()) {
//                    // Swipe up starts and stops the slideshow
//                    toggle();
//                }
            }

            @Override
            protected void onSwipeDown() {
//                if (checkUserInputAllowed()) {
//                    // Swipe down starts and stops the slideshow
//                    toggle();
//                }
            }
        });

        SharedPreferences sharedPrefManaged = getSharedPreferences(SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE);
        currentPath = sharedPrefManaged.getString(SHARED_MANAGED_CONFIG_KIOSK_SLIDESHOW_PATH, null);
        SLIDESHOW_DELAY = (int) (Float.parseFloat(String.valueOf(sharedPrefManaged.getInt(SHARED_MANAGED_CONFIG_KIOSK_SLIDESHOW_DELAY, 3))) * 1000);

        // Set up image list
        assert currentPath != null;
        fileList = getFileList(currentPath, false, true);
        if (fileList.size() == 0) {
            // No files to view. Exit
            Log.i(SlideShowActivityTag, "No files in list.");
            Toast.makeText(this, R.string.toast_no_files, Toast.LENGTH_SHORT).show();
            onBackPressed();
            return;
        }

        if (RANDOM_ORDER) {
            Collections.shuffle(fileList);
        }

        if (currentPath == null) {
            imagePosition = 0;
            nextImage(true, true);
        } else {
            for (int i = 0; i < fileList.size(); i++) {
                if (currentPath.equals(fileList.get(i).getPath())) {
                    imagePosition = i;
                    break;
                }
            }
        }

        Log.v(SlideShowActivityTag, String.format("First item is at index: %s", imagePosition));
        Log.v(SlideShowActivityTag, String.format("File list has size of: %s", fileList.size()));

        // Show the first image
        loadImage(imagePosition, false);
    }

    /**
     * Checks if user input is allowed and toasts if not.
     *
     * @return True if allowed, false otherwise
     */
    private boolean checkUserInputAllowed() {
//        if (!userInputAllowed) {
//            Toast.makeText(SlideshowActivity.this, R.string.toast_input_blocked, Toast.LENGTH_SHORT).show();
//        }
        return userInputAllowed;
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Only reload the settings if not blocked by onCreate
        if (blockPreferenceReload) {
            blockPreferenceReload = false;
        } else {
            loadPreferences();
        }
        // Start slideshow if no UI
        startSlideshowIfFullscreen();
    }

    /**
     * Load the relevant preferences.
     */
    private void loadPreferences() {
        try {
            imageStrategy = new GlideImageStrategy();
        } catch (Exception e) {
            imageStrategy = new CustomImageStrategy();
        }
        imageStrategy.setContext(this);
        imageStrategy.setCallback(this);
    }

    /**
     * Show the next image.
     */
    private void nextImage(boolean forwards, boolean preload) {
        nextImage(nextImagePosition(forwards), preload);
    }

    /**
     * Show the next image.
     */
    private void nextImage(int newPosition, boolean preload) {
        if (preload && !PRELOAD_IMAGES) {
            // Stop
            return;
        }

        int current = imagePosition;
        if (REFRESH_FOLDER && newPosition == 0) { // Time to reload, easy base case
            fileList = getFileList(currentPath, false, true);
            if (RANDOM_ORDER) {
                Collections.shuffle(fileList);
            }
        }

        if (newPosition == current) {
            // Looped. Exit
//            onBackPressed();
            return;
        }
        //noinspection EmptyFinallyBlock
        try {
            if (!preload) {
                imagePosition = newPosition;
            }

            loadImage(newPosition, preload);
        } catch (Exception e) {
            Log.e("TAG", e.toString());
        } finally {

        }
    }

    /**
     * Creates a list of fileitem for the given path.
     *
     * @param currentPath           The directory path.
     * @param includeDirectories    Whether or not to include directories.
     * @param includeSubDirectories Whether or not to include sub directories.
     */
    public List<FileItem> getFileList(@NonNull String currentPath, boolean includeDirectories,
                                      boolean includeSubDirectories) {
        Log.d(SlideShowActivityTag, "updateFileList currentPath: " + currentPath);

        // Create file list
        List<FileItem> fileList = new ArrayList<>();
        File dir = new File(currentPath);

        File[] files = dir.listFiles();
        if (files != null) {
            // Check hidden file preference
            for (File file : files) {
                if (!file.getName().startsWith(".")) {
                    // Test directories
                    if (includeDirectories || !file.isDirectory()) {
                        fileList.add(createFileItem(file));
                    } else if (includeSubDirectories) {
                        fileList.addAll(getFileList(file.getAbsolutePath(), false, true));
                    }
                }
            }
        }
        Collections.sort(fileList);
        return fileList;
    }

    /**
     * Create a fileitem from the given file.
     */
    public FileItem createFileItem(File file) {
        FileItem item = new FileItem();
        item.setName(file.getName());
        item.setPath(file.getAbsolutePath());
        item.setIsDirectory(file.isDirectory());
        return item;
    }

    /**
     * Show the following image.
     * This method handles whether or not the slideshow is in reverse order.
     */
    private void followingImage() {
        nextImage(!REVERSE_ORDER, true);
    }

    /**
     * Gets the position of the next image.
     */
    private int nextImagePosition(boolean forwards) {
        int newPosition = imagePosition;

        do {
            newPosition += forwards ? 1 : -1;
            if (newPosition < 0) {
                newPosition = fileList.size() - 1;
            }
            if (newPosition >= fileList.size()) {
                newPosition = 0;
            }
        } while (!testPositionIsImage(newPosition) || !testPositionExists(newPosition));
        return newPosition;
    }

    /**
     * Gets the position of the following image.
     * This method handles whether or not the slideshow is in reverse order.
     */
    private int followingImagePosition() {
        return nextImagePosition(!REVERSE_ORDER);
    }

    /**
     * Tests if the current file item is an image.
     *
     * @return True if image, false otherwise.
     */
    private boolean testPositionIsImage(int position) {
        return isImage(fileList.get(position));
    }

    /**
     * Checks the mime-type of the file to see if it is an image.
     */
    public boolean isImage(FileItem item) {
        if (item.getIsDirectory()) {
            return false;
        }
        if (item.getIsImage() != null) {
            return item.getIsImage();
        }
        String mimeType = getImageMimeType(item);
        item.setIsImage(mimeType != null && mimeType.startsWith("image"));
        return item.getIsImage();
    }

    /**
     * Returns the mime type of the given item.
     */
    public String getImageMimeType(FileItem item) {
        String mime = "";
        try {
            mime = URLConnection.guessContentTypeFromName(item.getPath());
        } catch (StringIndexOutOfBoundsException e) {
            // Not sure the cause of this issue but it occurred on production so handling as blank mime.
        }

        if (mime == null || mime.isEmpty()) {
            // Test mime type by loading the image
            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(item.getPath(), opt);
            mime = opt.outMimeType;
        }

        return mime;
    }

    /**
     * Tests if the current file item still exists.
     *
     * @return True if it's there, false otherwise.
     */
    private boolean testPositionExists(int position) {
        return new File(fileList.get(position).getPath()).exists();
    }

    /**
     * Load the image to the screen.
     */
    private void loadImage(int position, boolean preload) {
        if (preload && !PRELOAD_IMAGES) {
            // Stop
            return;
        }

        try {
            final FileItem item = fileList.get(position);

            if (preload) {
                imageStrategy.preload(item);
            } else {
                setTitle(item.getName());
                imageStrategy.load(item, mContentView);
            }
        } catch (NullPointerException npe) {
            Toast.makeText(this, R.string.toast_error_loading_image, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been created, to briefly hint
        // to the user that UI controls are available.
        delayedHide();
    }

    /**
     * Pause or play the slideshow.
     */
    private void toggleSlideshow() {
        if (isRunning) {
            stopSlideshow();
        } else {
            startSlideshowIfFullscreen();
        }
    }

    /**
     * Stop or start the slideshow.
     */
//    private void toggle() {
//        if (mVisible) {
//            hide();
//        } else {
//            show();
//        }
//    }
    private void hide() {
        mVisible = false;
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

//    private void show() {
//        stopSlideshow();
//        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
//        mVisible = true;
//        // Schedule a runnable to display UI elements after a delay
//        mHideHandler.removeCallbacks(mHidePart2Runnable);
//    }

    /**
     * Schedules a call to hide() in 100 milliseconds, canceling any previously scheduled calls.
     */
    private void delayedHide() {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, 100);
    }

    /**
     * Starts or restarts the slideshow if the view is in fullscreen mode.
     */
    private void startSlideshowIfFullscreen() {
        if (!mVisible) {
            startSlideshow();
        }
    }

    /**
     * Starts or restarts the slideshow
     */
    private void startSlideshow() {
        isRunning = true;
        mSlideshowHandler.removeCallbacks(mSlideshowRunnable);
        queueSlide();
    }

    /**
     * Queue the next slide in the slideshow
     */
    @Override
    public void queueSlide() {
        queueSlide(SLIDESHOW_DELAY);
    }

    @Override
    public void queueSlide(int delayMillis) {
        if (delayMillis < SLIDESHOW_DELAY) {
            delayMillis = SLIDESHOW_DELAY;
        }
        if (isRunning) {
            // Ensure only one runnable is in the queue
            mSlideshowHandler.removeCallbacks(mSlideshowRunnable);
            mSlideshowHandler.postDelayed(mSlideshowRunnable, delayMillis);
            // Preload the next image
            followingImage();
        }
    }

    private void stopSlideshow() {
        isRunning = false;
        mSlideshowHandler.removeCallbacks(mSlideshowRunnable);
    }

    @Override
    protected void onResume() {
        handler.postDelayed(runnable = () -> {
            handler.postDelayed(runnable, delay);
            hide();
        }, delay);
        super.onResume();
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(runnable);
        super.onPause();
    }
}