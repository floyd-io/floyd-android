package com.scvsoft.floyd;

import android.os.Handler;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by diego on 6/12/14.
 */
public class MessagePostWorkerThread extends Thread {

    HttpURLConnection connection;
    InputStream inputStream;
    Runnable runOnMessagePosted;
    String host;
    String message;
    Handler uiHandler;

    public MessagePostWorkerThread(String host, String message, Handler uiHandler, Runnable runOnMessagePosted) {
        this.runOnMessagePosted = runOnMessagePosted;
        this.host = host;
        this.message = message;
        this.uiHandler = uiHandler;
    }

    @Override
    public void run(){
        try
        {
            Log.d("TEST", "Connecting using DefaultHttpClient (" + host + ")");
            String sURL = String.format("http://%s:1337/update", this.host);

            HttpClient client = new DefaultHttpClient();
            HttpPost request = new HttpPost(sURL);

            StringEntity se = new StringEntity(this.message);
            request.setEntity(se);

            // Execute HTTP Post Request
            HttpResponse response = client.execute(request);

            uiHandler.post(runOnMessagePosted);
        }
        catch (Exception E)
        {
            Log.d("TEST", E.getMessage());
        }
    }
}
