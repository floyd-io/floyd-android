package com.example.myapp;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.MalformedChunkCodingException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketException;

/**
 * Created by Six66 on 21/04/14.
 */
public class DefaultHttpClientWorkerThread extends Thread {

    Runnable onConnect;
    Runnable onDisconnect;
    String host;
    Handler observerHandler;
    ChunkedResponseHandler chunkedResponseHandler;
    HttpGet request;

    public DefaultHttpClientWorkerThread(String host, Handler uiHandler, Runnable onConnect, Runnable onDisconnect){
        this.onConnect = onConnect;
        this.onDisconnect = onDisconnect;
        this.host = host;
        this.observerHandler = uiHandler;
    }

    Runnable disconnect = new Runnable() {
        @Override
        public void run() {
            //chunkedResponseHandler.stop();
            request.abort();
        }
    };

    public Runnable getDisconnect()
    {
        return disconnect;
    }

    @Override
    public void run(){
        Log.d("TEST", "Connecting using DefaultHttpClient (" + host + ")");
        String sURL = String.format("http://%s:1337/part2.html", this.host);

        HttpClient client = new DefaultHttpClient();
        request = new HttpGet(sURL);
        try
        {
            chunkedResponseHandler = new ChunkedResponseHandler(observerHandler, onConnect, onDisconnect);
            String responseString = client.execute(request, chunkedResponseHandler);
        }
        catch (MalformedChunkCodingException E) {
            //thrown when the other party closes the socket without writing the last chunk (without zero byte indicator header)
            Log.d("TEST", "Disconnected (Server closed the connection in a graceless manner)");
            observerHandler.post(onDisconnect);
        }
        catch (SocketException E)
        {
            //can be thrown by user requesting a disconnection
            //should know about this before coming here (maybe using a flag)
            //message will be 'Socket closed'

            Log.d("TEST", "Disconnected by user request...");
            observerHandler.post(onDisconnect);
        }
        catch (Exception E) {
            Log.d("TEST", E.getClass().getName());
            Log.d("TEST", E.getMessage());
        }
        finally {
            client.getConnectionManager().shutdown();
        }
    }

    public class ChunkedResponseHandler implements ResponseHandler<String> {

        Handler observerHandler;
        Runnable onConnected;
        Runnable onDisconnected;
        InputStream inputStream;

        private void notifyUI(String msg) {
            Message message = observerHandler.obtainMessage();
            message.obj = msg;
            observerHandler.sendMessage(message);
        }

        public ChunkedResponseHandler(Handler observerHandler, Runnable onConnected, Runnable onDisconnected) {
            this.observerHandler = observerHandler;
            this.onConnected = onConnected;
            this.onDisconnected = onDisconnected;
        }

        /*
        public void stop()
        {
            try
            {
                Log.d("TEST", "Attempting to Stop");
                inputStream.close();
            }
            catch (Exception E)
            {
                Log.d("TEST", "Disconnect attempt failed with Message: " + E.getMessage());
            }
        }
        */

        public String handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
            Log.d("TEST", "Connected!");
            observerHandler.post(onConnect);

            HttpEntity entity = response.getEntity();
            inputStream = entity.getContent();

            int readCount;
            byte[] inputData = new byte[1024];
            String msg;

            while ((readCount = inputStream.read(inputData)) != -1) {
                Log.d("TEST", Integer.toString(readCount) + " chars received");

                msg = new String(inputData, 0, readCount);

                //post data to UI
                notifyUI(msg);
            }

            Log.d("TEST", "Disconnected (Server closed the connection gracefully)");
            observerHandler.post(onDisconnected);

            return "";
        }
    }
}
