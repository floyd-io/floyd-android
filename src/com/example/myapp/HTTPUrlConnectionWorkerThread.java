package com.example.myapp;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Created by Six66 on 20/04/14.
 */
public class HTTPUrlConnectionWorkerThread extends Thread {

    Runnable onConnect;
    Runnable onDisconnect;
    String host;
    Handler observerHandler;
    HttpURLConnection connection;
    InputStream inputStream;

    public HTTPUrlConnectionWorkerThread(String host, Handler uiHandler, Runnable onConnect, Runnable onDisconnect){
        this.onConnect = onConnect;
        this.onDisconnect = onDisconnect;
        this.host = host;
        this.observerHandler = uiHandler;
    }

    private void notifyUI(String msg) {
        Message message = observerHandler.obtainMessage();
        message.obj = msg;
        observerHandler.sendMessage(message);
    }

    Runnable disconnect = new Runnable() {
        @Override
        public void run() {
            try
            {
                //connection.disconnect();
                inputStream.close();
            }
            catch (Exception E)
            {
                Log.d("TEST", "Disconnect attempt failed with Message: " + E.getMessage());
            }
        }
    };

    public Runnable getDisconnect()
    {
        return disconnect;
    }

    @Override
    public void run(){

        Log.d("TEST", "Connecting...");
        connection = null;

        try
        {
            URL url = new URL("http", host, 1337, "part2.html");
            connection = (HttpURLConnection) url.openConnection();

            //connection.setConnectTimeout(50000);
            //connection.setReadTimeout(50000);
            //connection.connect();

            Log.d("TEST", "Connected!");
            observerHandler.post(onConnect);

            //should be 200
            int retValue = connection.getResponseCode();

            //inputStream = connection.getInputStream();
            BufferedInputStream inputStream = new BufferedInputStream(connection.getInputStream());

            byte[] buffer = new byte[1024];
            int readCount = 0;
            String msg;

            Log.d("TEST", "Listening...");

            while ((readCount = inputStream.read(buffer)) != -1) {
                Log.d("TEST", Integer.toString(readCount) + " chars received");

                msg = new String(buffer, 0, readCount);

                //post data to UI
                notifyUI(msg);
            }

            inputStream.close();
        }
        catch (Exception E){
            Log.d("TEST", "SocketWorker blown! Exc Class: " + E.getClass().getName() + " Message:" +  E.getMessage());
            observerHandler.post(onDisconnect);
        }
        finally{
            if (connection != null) {
                observerHandler.post(onDisconnect);

                //frees the instance in the pool to reuse
                connection.disconnect();

                Log.d("TEST", "Disconnected (like a Sir)");
            }
        }
    }
}
