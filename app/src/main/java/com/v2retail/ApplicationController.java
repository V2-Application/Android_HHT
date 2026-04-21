package com.v2retail;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.LruCache;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

import java.util.Observable;

import io.sentry.Sentry;

// Custom Application class.
public class ApplicationController extends Application  {

    public static final String TAG = ApplicationController.class.getSimpleName();

    private static ApplicationController mApplicationController;

    private RequestQueue mRequestQueue;
    private ImageLoader mImageLoader;

    private Observable mRefreshObservable = new RefreshObservable();

    @Override
    public void onCreate() {
        super.onCreate();
        mApplicationController = this;



        // CookieHandler.setDefault(new CookieManager());

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
                // Sentry.captureMessage("Something went wrong");
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
        // TODO Auto-generated method stub
        super.attachBaseContext(base);
        // MultiDex.install(this);
    }


    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(getContext());
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
        // set the default tag if tag is empty
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


    static class RefreshObservable extends  Observable {

        @Override
        public synchronized boolean hasChanged() {
            return true;
        }
    }

}
