package io.esper.files.strategy.image;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;

import com.bumptech.glide.BitmapRequestBuilder;
import com.bumptech.glide.DrawableRequestBuilder;
import com.bumptech.glide.DrawableTypeRequest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import io.esper.files.R;
import io.esper.files.model.FileItem;
import io.esper.files.strategy.image.glide.GlideRotateDimenTransformation;

public class GlideImageStrategy implements ImageStrategy {

    private static final String TAG = GlideImageStrategy.class.getName();

    private Context context;
    private ImageStrategyCallback callback;

    private final boolean PLAY_GIF = true;
    private final boolean AUTO_ROTATE_DIMEN = false;

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
        final DrawableTypeRequest<String> glideLoad = Glide
                .with(context)
                .load(item.getPath());
        if (PLAY_GIF) {
            // Play GIFs
            glideLoad.preload();
        } else {
            // Force bitmap so GIFs don't play
            glideLoad.asBitmap().preload();
        }
    }

    @Override
    public void load(final FileItem item, final ImageView view) {
        try {
            DrawableTypeRequest<String> glideLoad = Glide
                    .with(context)
                    .load(item.getPath());
            if (PLAY_GIF) {
                DrawableRequestBuilder<String> builder = glideLoad.diskCacheStrategy(DiskCacheStrategy.SOURCE)
                        .animate(R.anim.slide_up);
//                    .centerCrop()
//                    .crossFade();
//                    .dontAnimate();

                if (AUTO_ROTATE_DIMEN) {
                    builder = builder.transform(new GlideRotateDimenTransformation(context));
                } else {
                    builder = builder.fitCenter();
                }


                builder.placeholder(view.getDrawable())
                        .listener(new RequestListener<String, GlideDrawable>() {
                            @Override
                            public boolean onException(Exception e, String s, Target<GlideDrawable> target, boolean b) {
                                Log.e(TAG, "Error loading image", e);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(GlideDrawable glideDrawable, String s, Target<GlideDrawable> target, boolean b, boolean b1) {
                                if (glideDrawable instanceof GifDrawable) {
                                    // Queue the next slide after the animation completes
                                    GifDrawable gifDrawable = (GifDrawable) glideDrawable;

                                    int duration = 250; // Start with a little extra time
                                    GifDecoder decoder = gifDrawable.getDecoder();
                                    for (int i = 0; i < gifDrawable.getFrameCount(); i++) {
                                        duration += decoder.getDelay(i);
                                    }

                                    callback.queueSlide(duration);
                                } else {
                                    callback.queueSlide();
                                }

                                return false;
                            }
                        })
                        .into(view);
            } else {
                // Force bitmap so GIFs don't play
                BitmapRequestBuilder<String, Bitmap> builder = glideLoad.asBitmap()
                        .dontAnimate();

                if (AUTO_ROTATE_DIMEN) {
                    builder = builder.transform(new GlideRotateDimenTransformation(context));
                } else {
                    builder = builder.fitCenter();
                }

                builder.placeholder(view.getDrawable())
                        .error(R.color.image_background)
                        .listener(new RequestListener<String, Bitmap>() {
                            @Override
                            public boolean onException(Exception e, String s, Target<Bitmap> target, boolean b) {
                                Log.e(TAG, "Error loading image", e);
                                callback.queueSlide();
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Bitmap bitmap, String s, Target<Bitmap> target, boolean b, boolean b1) {
                                callback.queueSlide();

                                return false;
                            }
                        })
                        .into(view);
            }
        }
        catch (Exception e)
        {
            Log.e("TAG", e.toString());
        }
        finally {

        }
    }

}
