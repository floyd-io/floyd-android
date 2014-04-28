package com.scvsoft.floyd;

import android.Manifest;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import ch.boye.httpclientandroidlib.HttpEntity;
import ch.boye.httpclientandroidlib.HttpRequest;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.HttpClient;
import ch.boye.httpclientandroidlib.client.ResponseHandler;
import ch.boye.httpclientandroidlib.client.methods.HttpGet;
import ch.boye.httpclientandroidlib.impl.client.BasicResponseHandler;
import ch.boye.httpclientandroidlib.impl.client.CloseableHttpClient;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import ch.boye.httpclientandroidlib.impl.client.HttpClientBuilder;

/**
 * Created by Six66 on 24/04/14.
 */
public class PimpedHttpClientWorkerThread extends Thread {
    Runnable onConnect;
    Runnable onDisconnect;
    String host;
    Handler observerHandler;
    CloseableHttpClient httpClient;
    HttpGet httpget;
    boolean stop = false;
    boolean serverDisconnected = false;

    public PimpedHttpClientWorkerThread(String host, Handler uiHandler, Runnable onConnect, Runnable onDisconnect){
        this.onConnect = onConnect;
        this.onDisconnect = onDisconnect;
        this.host = host;
        this.observerHandler = uiHandler;
    }


    Runnable disconnect = new Runnable() {
        @Override
        public void run() {
            try {
                Log.d("TEST", "Attempting to disconnect...");
                stop = true;
                //httpClient.close();
                //httpget.abort();
                httpClient.close();
            }
            catch (Exception E)
            {
                Log.d("TEST", "Attempt to disconnect failed with message: " + E.getMessage());
            }
        }
    };

    private void notifyUI(String msg) {
        Message message = observerHandler.obtainMessage();
        message.obj = msg;
        observerHandler.sendMessage(message);
    }

    public Runnable getDisconnect()
    {
        return disconnect;
    }

    @Override
    public void run(){
        Log.d("TEST", "Connecting using Pimped HTTP Client (" + host + ")");
        String sURL = String.format("http://%s:1337/part2.html", this.host);

        httpClient = HttpClientBuilder.create().build();

        try {
            httpget = new HttpGet(sURL);

            Log.d("TEST", "executing request " + httpget.getURI());

            HttpResponse httpResponse = httpClient.execute(httpget);
            HttpEntity entity = httpResponse.getEntity();

            if (entity != null) {
                Log.d("TEST", "Connected!");
                observerHandler.post(onConnect);

                InputStream instream = entity.getContent();
                String content;
                BufferedReader br = new BufferedReader(new InputStreamReader(
                        instream));

                // we read as long as we do not ask to stop or if the content == null (server disconnection)
                Log.d("TEST", "Listening...");
                while (!stop && !serverDisconnected) {

                    content = br.readLine();
                    if (content != null) {
                        Log.d("TEST", "Line: " + content);
                        notifyUI(content);

                    }
                    else {
                        serverDisconnected = true;
                    }
                }

                observerHandler.post(onDisconnect);

                if (serverDisconnected && !stop) {
                    Log.d("TEST", "Disconnected (Server closed the connection gracefully)");
                }
                else if (serverDisconnected && stop) {
                    Log.d("TEST", "Disconnected!");
                }
            }
        }
        catch (Exception E)
        {
            Log.d("TEST", E.getMessage());
        }
        finally {
            // When HttpClient instance is no longer needed,
            // shut down the connection manager to ensure
            // immediate deallocation of all system resources
            //httpClient.getConnectionManager().shutdown();
            try{
                httpget.abort();
                httpClient.close();
            }
            catch (Exception Ex)
            {
                Log.d("TEST", "Erorr cleaning up resources. Message: " + Ex.getMessage());
            }
        }
    }
}
