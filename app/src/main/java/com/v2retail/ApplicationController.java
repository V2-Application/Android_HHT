package com.v2retail;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.LruCache;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Observable;

import io.sentry.Sentry;

/**
 * Custom Application class.
 *
 * Adds X-HHT-Serial header to every Volley request so the Azure middleware
 * can count individual HHT devices (not just store sessions).
 *
 * Header value: Android ANDROID_ID (unique per device, stable, no permission needed).
 */
public class ApplicationController extends Application  {

    public static final String TAG = ApplicationController.class.getSimpleName();

    private static ApplicationController mApplicationController;

    private RequestQueue mRequestQueue;
    private ImageLoader mImageLoader;

    // Android ID — unique per device, stable across reboots, no permission needed
    private String mDeviceId = "";

    private Observable mRefreshObservable = new RefreshObservable();

    @Override
    public void onCreate() {
        super.onCreate();
        mApplicationController = this;
        // Cache device ID once at startup
        mDeviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        if (mDeviceId == null) mDeviceId = "";

        mImageLoader = new ImageLoader(getRequestQueue(),
                new ImageLoader.ImageCache() {
                    private final LruCache<String, Bitmap>
                            cache = new LruCache<String, Bitmap>(20);

                    public Bitmap getBitmap(String url) {
                        return cache.get(url);
                    }

                    public void putBitmap(String url, Bitmap bitmap) {
                        cache.put(url, bitmap);
                    }
                });

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t,  Throwable e) {
                Sentry.captureException(e);
            }
        });
    }

    public static synchronized ApplicationController getInstance() {
        return mApplicationController;
    }

    public static synchronized Context getContext() {
        return mApplicationController.getApplicationContext();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            final String deviceId = mDeviceId;
            // Custom HurlStack: injects X-HHT-Serial into every request
            // Middleware reads this to count individual devices (not just stores)
            HurlStack stack = new HurlStack() {
                @Override
                protected HttpURLConnection createConnection(URL url) throws IOException {
                    HttpURLConnection conn = super.createConnection(url);
                    if (deviceId != null && !deviceId.isEmpty()) {
                        conn.setRequestProperty("X-HHT-Serial", deviceId);
                    }
                    return conn;
                }
            };
            mRequestQueue = Volley.newRequestQueue(getContext(), stack);
        }
        return mRequestQueue;
    }

    public Observable refreshObservable() {
        if(mRefreshObservable==null) {
            mRefreshObservable = new RefreshObservable();
        }
        return mRefreshObservable;
    }

    public <T> void addToRequestQueue(Request<T> req, String tag) {
        req.setTag(TextUtils.isEmpty(tag) ? TAG : tag);
        getRequestQueue().add(req);
    }

    public <T> void addToRequestQueue(Request<T> req) {
        req.setTag(TAG);
        getRequestQueue().add(req);
    }

    public void cancelPendingRequests(Object tag) {
        if (mRequestQueue != null) {
            mRequestQueue.cancelAll(tag);
        }
    }

    public ImageLoader getImageLoader() {
        return mImageLoader;
    }

    static class RefreshObservable extends Observable {
        @Override
        public synchronized boolean hasChanged() {
            return true;
        }
    }
}
