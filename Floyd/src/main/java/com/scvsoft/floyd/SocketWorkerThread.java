package com.scvsoft.floyd;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Created by Six66 on 20/04/14.
 */
public class SocketWorkerThread extends Thread {

    Runnable onConnect;
    Runnable onDisconnect;
    String host;
    Handler observerHandler;
    Socket socket;
    InputStream inputStream;
    OutputStream outputStream;

    Runnable killMePlease = new Runnable() {
        @Override
        public void run() {
            try
            {
                Log.d("TEST", "Attempting to Disconnect");

                //trying to tell the server to close the connection raises a socket
                //exception on the blocking read operation.
                //outputStream.close();

                //closing the input to unblock the read operation, raises a socket
                //exception on the blocking read operation.
                //inputStream.close();

                //closing the socket to free the blocking read operation, raises
                //a socket exception on the blocking read operation.
                //socket.close();


                //WTF? This closes the connection gracefully. #misterio
                socket.shutdownInput();
            }
            catch (Exception E)
            {
                Log.d("TEST", "Disconnect attempt failed with Message: " + E.getMessage());
            }
        }
    };

    public Runnable getKillMePlease()
    {
        return killMePlease;
    }

    public SocketWorkerThread(String host, Handler uiHandler, Runnable onConnect, Runnable onDisconnect){
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

    @Override
    public void run(){

        String resourceName = "/part2.html";

        try
        {
            Log.d("TEST", "Connecting trough socket to host (" + host + ")...");
            InetAddress serverAddress = InetAddress.getByName(host);
            //socket = new Socket(serverAddress, 1337);

            SocketAddress socketAddress = new InetSocketAddress(serverAddress, 1337);

            socket = new Socket();
            socket.connect(socketAddress, 5000);

            Log.d("TEST", "Connected!");
            observerHandler.post(onConnect);

            String request = "GET " + resourceName + " HTTP/1.1\nHost: " + host + "\n\n";
            byte[] outputData = request.getBytes(StandardCharsets.UTF_8);

            outputStream = socket.getOutputStream();
            Log.d("TEST", "Sending Request");
            outputStream.write(outputData, 0, outputData.length);
            Log.d("TEST", "Request Sent");

            byte[] inputData = new byte[1024];

            inputStream  = socket.getInputStream();


            //socket.setSoTimeout(0); ???

            int readCount;
            String msg;

            Log.d("TEST", "Listening...");

            //this read will BLOCK!!!! (and will return -1 (EOF) when the other side of the connection
            //is closed).
            while ((readCount = inputStream.read(inputData)) != -1) {
                Log.d("TEST", Integer.toString(readCount) + " chars received");

                //parse this json into business objs.
                //Chunks CAN be determined since each one starts with an hex indicating it size, a carriage return
                //and then the message. When the message ends there is another carriage return.
                //a size of 0 indicates that the response is over and no more chunks will come.
                //Since the server closes the connection after suck a chunk, we will read a -1 (EOF) on readCount
                //and will exit this loop
                msg = new String(inputData, 0, readCount);

                //post data to UI
                notifyUI(msg);

            }

            Log.d("TEST", "Housekeeping");
            outputStream.close();
            inputStream.close();
            socket.close();

            observerHandler.post(onDisconnect);
            Log.d("TEST", "Disconnected (like a Sir)");
        }
        catch (Exception E){
            Log.d("TEST", "SocketWorker blown! Exc Class: " + E.getClass().getName() + " Message:" +  E.getMessage());
            observerHandler.post(onDisconnect);
        }
    }
}