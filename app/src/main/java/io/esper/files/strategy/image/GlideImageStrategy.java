package io.esper.files.strategy.image;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import io.esper.files.R;
import io.esper.files.model.FileItem;
import io.esper.files.strategy.image.glide.GlideRotateDimenTransformation;

@SuppressWarnings("ALL")
public class GlideImageStrategy implements ImageStrategy {

    private static final String TAG = GlideImageStrategy.class.getName();
    private final boolean PLAY_GIF = true;
    @SuppressWarnings("FieldCanBeLocal")
    private final boolean AUTO_ROTATE_DIMEN = false;
    private Context context;
    private ImageStrategyCallback callback;

    @Override
    public void setContext(Context context) {
        this.context = context;
    }

    @Override
    public void setCallback(ImageStrategyCallback callback) {
        this.callback = callback;
    }

    @Override
    public void preload(final FileItem item) {
//        if (PLAY_GIF) {
        final RequestBuilder<Drawable> glideLoad = Glide
                .with(context)
                .asDrawable()
                .load(item.getPath());
        glideLoad.preload();
//        } else {
//            // Force bitmap so GIFs don't play
//            final RequestBuilder<Bitmap> glideLoad = Glide
//                    .with(context)
//                    .asBitmap()
//                    .load(item.getPath());
//            glideLoad.preload();
//        }
    }

    @Override
    public void load(final FileItem item, final ImageView view) {
        //noinspection EmptyFinallyBlock
        try {
            RequestBuilder<Drawable> glideLoad = Glide
                    .with(context)
                    .asDrawable()
                    .load(item.getPath());
//            if (PLAY_GIF) {
//                DrawableRequestBuilder<String> builder = glideLoad.diskCacheStrategy(DiskCacheStrategy.SOURCE)
//                        .animate(R.anim.slide_up);
////                    .centerCrop()
////                    .crossFade();
////                    .dontAnimate();
//
//                if (AUTO_ROTATE_DIMEN) {
//                    builder = builder.transform(new GlideRotateDimenTransformation(context));
//                } else {
//                    builder = builder.fitCenter();
//                }
//
//
//                builder.placeholder(view.getDrawable())
//                        .listener(new RequestListener<String, GlideDrawable>() {
//                            @Override
//                            public boolean onException(Exception e, String s, Target<GlideDrawable> target, boolean b) {
//                                Log.e(TAG, "Error loading image", e);
//                                return false;
//                            }
//
//                            @Override
//                            public boolean onResourceReady(GlideDrawable glideDrawable, String s, Target<GlideDrawable> target, boolean b, boolean b1) {
//                                if (drawable instanceof Animatable) {
//                                    // Queue the next slide after the animation completes
//                                    GifDrawable gifDrawable = (GifDrawable) glideDrawable;
//
//                                    int duration = 250; // Start with a little extra time
//                                    GifDecoder decoder = gifDrawable.getDecoder();
//                                    for (int i = 0; i < gifDrawable.getFrameCount(); i++) {
//                                        duration += decoder.getDelay(i);
//                                    }
//
//                                    callback.queueSlide(duration);
//                                } else {
//                                    callback.queueSlide();
//                                }
//
//                                return false;
//                            }
//                        })
//                        .into(view);
//            } else {
            // Force bitmap so GIFs don't play
            RequestBuilder<Drawable> builder = glideLoad.diskCacheStrategy(DiskCacheStrategy.ALL)
                    .dontAnimate();

            if (AUTO_ROTATE_DIMEN) {
                builder = builder.transform(new GlideRotateDimenTransformation());
            } else {
                builder = builder.fitCenter();
            }

            builder.placeholder(view.getDrawable())
                    .error(R.color.image_background)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            Log.e(TAG, "Error loading image", e);
                            callback.queueSlide();
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            callback.queueSlide();
                            return false;
                        }
                    })
                    .into(view);
//            }
        } catch (Exception e) {
            Log.e("TAG", e.toString());
        } finally {

        }
    }

}
