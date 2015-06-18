package com.iktwo.spotifystreamer;

import android.os.AsyncTask;
import android.util.Log;

import com.squareup.okhttp.CacheControl;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.ArrayList;

public class HttpAsyncRequest extends AsyncTask<String, String, ArrayList<String>> {
    private static final String TAG = "HttpAsyncRequest";
    AsyncResponse requester = null;

    public HttpAsyncRequest(AsyncResponse requester) {
        this.requester = requester;
    }

    @Override
    protected ArrayList<String> doInBackground(String... params) {
        ArrayList<String> reply = new ArrayList();

        OkHttpClient httpclient = new OkHttpClient();
        Response response;

        try {
            /// Create request with cache disabled
            Request request = new Request.Builder()
                    .cacheControl(new CacheControl.Builder().noCache().build())
                    .url(params[0])
                    .build();

            response = httpclient.newCall(request).execute();

            if (response.isSuccessful()) {
                /// Add Url to the reply
                reply.add(params[0]);

                /// Add response to the reply
                reply.add(response.body().string());

                response.body().close();
            }
        } catch (IOException e) {
            reply.add("error");
            reply.add(e.getMessage());
            Log.e(TAG, "ERROR:" + e.toString());
        }

        return reply;
    }

    @Override
    protected void onPostExecute(ArrayList<String> reply) {
        requester.processFinish(reply);
    }

    public interface AsyncResponse {
        void processFinish(ArrayList<String> reply);
    }
}
