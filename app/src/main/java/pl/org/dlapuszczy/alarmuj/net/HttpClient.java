package pl.org.dlapuszczy.alarmuj.net;

import android.content.Context;
import android.support.annotation.NonNull;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;

import java.io.File;

/**
 * Created by ohaleck on 27/07/2017.
 */

public class HttpClient {
    private static HttpClient instance;
    private final File cacheDir;
    private RequestQueue requestQueue;

    private HttpClient(Context context) {
        cacheDir = context.getCacheDir();
    }

    public RequestQueue getVolleyRequestQueue() {
        if (requestQueue == null) {
            Cache cache = new DiskBasedCache(cacheDir, 1024 * 1024); // 1MB cap
            Network network = new BasicNetwork(new HurlStack());
            requestQueue = new RequestQueue(cache, network);
        }

        return requestQueue;
    }

    public void addRequest(@NonNull final Request<?> request, @NonNull final String tag) {
        request.setTag(tag);
        addRequest(request);
    }

    private void addRequest(@NonNull final Request<?> request) {
        getVolleyRequestQueue().add(request);
    }

    public static HttpClient getInstance(Context context) {
        if (instance == null) {
            synchronized (HttpClient.class) {
                instance = new HttpClient(context);
            }
        }
        return instance;
    }


}
