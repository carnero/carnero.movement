package carnero.movement.common;

import android.content.Context;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

public class ImageLoaderSingleton {

    protected static ImageLoader sImageLoader;

    public static ImageLoader getInstance() {
        if (sImageLoader == null) {
            DisplayImageOptions defaultOptions = new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisc(true)
                .resetViewBeforeLoading(true)
                .imageScaleType(ImageScaleType.EXACTLY)
                .build();

            ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(Application.get())
                .defaultDisplayImageOptions(defaultOptions)
                .discCacheSize(10 * 1024 * 1024)
                .threadPriority(Thread.MIN_PRIORITY)
                .build();

            ImageLoader.getInstance().init(config);

            sImageLoader = ImageLoader.getInstance();
        }

        return sImageLoader;
    }
}