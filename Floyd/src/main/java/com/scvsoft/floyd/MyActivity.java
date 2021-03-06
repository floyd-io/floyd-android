package com.scvsoft.floyd;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MyActivity extends Activity {
    private TextView txtIpAddress;
    private TextView txtResponseView;
    private Spinner spConnectionType;
    private Button btnConnect;
    private EditText editTextToPost;
    private Button btnPost;

    private boolean connected = false;

    private Runnable stopListening;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        txtIpAddress = (TextView)this.findViewById(R.id.ipAddress);
        txtResponseView = (TextView)this.findViewById(R.id.txtResponse);
        spConnectionType = (Spinner)this.findViewById(R.id.connectionType);
        btnConnect = (Button)this.findViewById(R.id.btnConnect);
        btnPost = (Button)this.findViewById(R.id.btnPost);
        editTextToPost = (EditText)this.findViewById(R.id.txtPostMessage);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.connection_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spConnectionType.setAdapter(adapter);

        txtIpAddress.setText("192.168.1.105");

        final Runnable runOnConnect = new Runnable() {
            @Override
            public void run() {
                connected = true;
                txtResponseView.setText("");
                btnConnect.setText("Disconnect");
            }
        };

        final Runnable runOnDisconnect = new Runnable() {
            @Override
            public void run() {
                connected = false;
                txtResponseView.setText("");
                btnConnect.setText("Connect");
            }
        };

        final Handler onNewDataHandler = new Handler() {
            public void handleMessage(Message msg) {
                txtResponseView.setText((String)msg.obj);
            }
        };


        final Runnable runOnMessagePosted = new Runnable() {
            @Override
            public void run() {
                editTextToPost.setText("");
            }
        };

        final Handler onMessagePostedHandler = new Handler();

        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!connected) {

                    //PORT AND RESOURCE ARE FIXED (1337 // /part2.html)

                    switch (spConnectionType.getSelectedItemPosition())
                    {
                        case 0:{
                            //Socket

                            SocketWorkerThread socketWorkerThread = new SocketWorkerThread(txtIpAddress.getText().toString(),
                                    onNewDataHandler, runOnConnect, runOnDisconnect);

                            stopListening = socketWorkerThread.getKillMePlease();
                            socketWorkerThread.start();

                            break;
                        }
                        case 1: {
                            //HttpURLConnection

                            //System.setProperty("http.keepAlive", "false");
                            HTTPUrlConnectionWorkerThread httpUrlConnectionWorkerThread = new HTTPUrlConnectionWorkerThread(txtIpAddress.getText().toString(),
                                    onNewDataHandler, runOnConnect, runOnDisconnect);

                            stopListening = httpUrlConnectionWorkerThread.getDisconnect();
                            httpUrlConnectionWorkerThread.start();

                            //new BackGroundTask().execute("http://192.168.1.105:1337/part2.html");

                            break;
                        }
                        case 2: {
                            //DefaultHttpClient

                            DefaultHttpClientWorkerThread defaultHttpClientWorkerThread = new DefaultHttpClientWorkerThread(txtIpAddress.getText().toString(),
                                    onNewDataHandler, runOnConnect, runOnDisconnect);

                            stopListening = defaultHttpClientWorkerThread.getDisconnect();
                            defaultHttpClientWorkerThread.start();

                            break;
                        }

                        case 3: {
                            //PimpedHttpClientWorkerThread
                            PimpedHttpClientWorkerThread pimpedHttpClientWorkerThread = new PimpedHttpClientWorkerThread(txtIpAddress.getText().toString(),
                                    onNewDataHandler, runOnConnect, runOnDisconnect);

                            stopListening = pimpedHttpClientWorkerThread.getDisconnect();
                            pimpedHttpClientWorkerThread.start();

                            break;
                        }
                    }


                }
                else {
                    if (stopListening != null){

                        //do not poison the UI Thread.
                        //call this to stop the listening blocking thread...
                        Thread t = new Thread(stopListening);
                        t.start();
                    }
                }

                //ip de mi host en vbox (genymotion)
                //new BackGroundTask().execute("http://192.168.56.1:1337/part2.html");
            }
        });

        btnPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                editTextToPost.getText();

                MessagePostWorkerThread messagePostWorkerThread = new MessagePostWorkerThread(txtIpAddress.getText().toString(), editTextToPost.getText().toString(),
                        onMessagePostedHandler, runOnMessagePosted);
                messagePostWorkerThread.start();
            }
        });
    }


    /*
    public class BackGroundSocketTask extends AsyncTask<String, String, Integer> {

        @Override
        protected void onProgressUpdate(String... values) {
            txtResponseView.setText(values[0]);
        }

        protected Integer doInBackground(String... servers) {
            try {
                String resourceName = "/part2.html";

                InetAddress serverAddress = InetAddress.getByName(servers[0]);
                Socket socket = new Socket(serverAddress, 1337);
                Log.d("TEST", "connected");

                String request = "GET " + resourceName + " HTTP/1.1\nHost: " + servers[0] + "\n\n";
                byte[] outputData = request.getBytes(StandardCharsets.UTF_8);

                OutputStream outputStream = socket.getOutputStream();
                Log.d("TEST", "sending");
                outputStream.write(outputData, 0, outputData.length);
                Log.d("TEST", "sent");

                byte[] inputData = new byte[10024];

                InputStream inputStream  = socket.getInputStream();


                int readCount;
                String msg;

                Log.d("TEST", "listening");
                while ((readCount = inputStream.read(inputData)) != -1) {
                    Log.d("TEST", Integer.toString(readCount));
                    msg = new String(inputData, 0, readCount);
                    //Log.d("TEST", msg);
                    publishProgress(msg);
                    //publishProgress(Integer.toString(readCount));
                }

                outputStream.close();
                inputStream.close();
                socket.close();

                Log.d("TEST", "disconnected");
                return 0;

            } catch (Exception e) {
                //this.exception = e;
                Log.d("TEST",  e.getMessage());
                return null;
            }
        }
    }
    */

    public class BackGroundTask extends AsyncTask<String, String, Integer> {

        private Exception exception;

        private byte[] readStream(InputStream in) throws IOException {
            byte[] buf = new byte[1024];
            int count = 0;
            ByteArrayOutputStream out = new ByteArrayOutputStream(1024);

            String msg;

            while ((count = in.read(buf)) != -1) {
                //mando a la UI mi respuesta parcial
                //publishProgress(new String(Charset.defaultCharset().decode(ByteBuffer.wrap(buf)).toString()));
                msg = new String(buf, 0, count);
                publishProgress(msg);

                Log.d("TEST", "AsyncTask Received Data: " + msg);
            }

            return out.toByteArray();
        }

        @Override
        protected void onProgressUpdate(String... values) {
            txtResponseView.setText(txtResponseView.getText() + values[0]);
        }

        protected Integer doInBackground(String... urls) {
            try {
                Log.d("TEST", "AsyncTask Connecting...");
                HttpURLConnection connection = null;
                connection = (HttpURLConnection) new URL(urls[0]).openConnection();

                Log.d("TEST", "AsyncTask Connected...");
                //para servers que soporten RANGE (testear si lo soportan con un HEAD call y mirar los headers)
                //connection.addRequestProperty("RANGE", "bytes=0-1024");

                //connection.setConnectTimeout(50000);
                //connection.setReadTimeout(50000);
                connection.connect();

                int retValue = connection.getResponseCode();

                BufferedInputStream in = new BufferedInputStream(connection.getInputStream());

                Log.d("TEST", "AsyncTask Listening...");

                String resp = new String(readStream(in), "UTF-8");

                //Log.d("TEST", Integer.toString(resp.length()));

                return retValue;

            } catch (Exception e) {
                this.exception = e;
                Log.d("TEST", e.getMessage());
                return null;
            }
        }
    }
}
