package com.example.aleksandr.socketstreamer.io;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.JsonReader;

import com.example.aleksandr.socketstreamer.supporting.CameraPreview;
import com.example.aleksandr.socketstreamer.UI.StreamingActivity;
import com.example.aleksandr.socketstreamer.data.Listeners.ErrorListener;

import com.google.gson.JsonObject;


import org.json.JSONException;
import org.json.JSONObject;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;

/**
 * Created by Aleksandr on 03.05.2016.
 */
public class Client extends Thread implements LocationListener {
    // Main parts
    private Socket socket;
    private StreamingActivity uiThread;

    // streams
    private BufferedWriter outputStream;
    private BufferedReader bufferedReader;
    private JsonReader jsonReader;

    // Objects
    private Location lastLocation;
    private Thread input;
    private Thread output;
    private final Object statusBell = new Object(); // слушатель изменения translationStatus
    private final Object ipAndHostBell = new Object(); // слушатель изменения host&ip

    // Paramerers
    private String hostIp;
    private int hostPort = -1;
    private CameraPreview cameraPreview;
    private int translationStatus; // -1 = закончено соединение, 0 = ожидание/не подсоединялось, 1 = активно

    // listeners
    private ErrorListener errorListener;

    // параметры
    private static final int SERVER_TIMEOUT = 10000;


    // Constructors
    private Client(String hostIp, int hostPort) {
        this.translationStatus = 0;
        this.hostIp = hostIp;
        this.hostPort = hostPort;
    }

    public Client(StreamingActivity uiThread, String hostIp, int hostPort, CameraPreview cameraPreview) {
        this(hostIp, hostPort);
        this.cameraPreview = cameraPreview;
        this.uiThread = uiThread;
        this.errorListener = (ErrorListener) uiThread;
    }


    // Options
    public void close() {
        setTranslationStatus(-1);
        if (output != null && output.isAlive()) {
            output.interrupt();
        }
        if (input != null && input.isAlive()) {
            input.interrupt();
        }
        try {
            socket.close();
        } catch (IOException e) {
            if (errorListener != null) {
                errorListener.onError("Error closing connection.");
            }
            e.printStackTrace();
        }
        synchronized (statusBell) {
            statusBell.notifyAll();
        }
        synchronized (ipAndHostBell) {
            ipAndHostBell.notifyAll();
        }
    }


    // helpers
    private boolean openStream() {
        if (hostPort <= 0 || hostIp == null || hostIp.equals("")) {
            return false;
        }
        setTranslationStatus(0);
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(hostIp, hostPort), SERVER_TIMEOUT);

            outputStream = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            jsonReader = new JsonReader(bufferedReader);
            jsonReader.setLenient(true);
        } catch (IOException e) {
            if (errorListener != null) {
                errorListener.onError("Error on opening stream: server not responding.");
            }
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean checkConnection() {
        return translationStatus != -1 && translationStatus != 0 && socket.isConnected();
    }

    private void pauseStream() {
        setTranslationStatus(0);
    }

    private void startStream() {
        setTranslationStatus(1);
    }


    @Override
    public void run() {
        super.run();

        if (!openStream()) {
            uiThread.offButton();
            setTranslationStatus(-1);
            return;
        }

        input = new inputAnalyze();
        output = new outputData();
        input.start();
        output.start();
    }

    // thread for input and output
    class inputAnalyze extends Thread {
        @Override
        public void run() {
            super.run();
            while (!this.isInterrupted()) {
                if (!readMessage()) {
                    break;
                }
            }
        }

        private boolean readMessage() {
            try {
                jsonReader.beginObject();
                switch (jsonReader.nextName()) {
                    case "state": {
                        String condition = jsonReader.nextString();
                        if ("start".equals(condition)) {
                            startStream();
                            break;
                        }
                        if ("wait".equals(condition)) {
                            pauseStream();
                            break;
                        }
                        // SMTH new here
                        break;
                    }
                    case "heartbeat": {
                        String condition = jsonReader.nextString();
                        if ("request".equals(condition)) {
                            sendHeartbeatAnswer();
                            break;
                        }
                        // SMTH new
                        break;
                    }
                }
                jsonReader.endObject();

            } catch (IOException | IllegalStateException e) {
                if (!socket.isClosed()) {
                    uiThread.offButton();
                    setTranslationStatus(-1);
                }
                return false;
            }
            return true;
        }

        private void sendHeartbeatAnswer() {
            if (checkConnection()) {
                return;
            }

            JsonObject heartbeat = new JsonObject();
            heartbeat.addProperty("heartbeat", "answer");
            try {
                outputStream.write(heartbeat.toString() + "\n");
                outputStream.flush();
            } catch (IOException e) {
                if (errorListener != null)
                    errorListener.onError("Error in sending heartbeat answer: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    class outputData extends Thread {
        int previousStatus = 0;
        private static final String STOP_COMMAND = "STOP";

        @Override
        public void run() {
            super.run();

            try {
                sendInitialMessage();
            } catch (IOException e) {
                if (errorListener != null) {
                    errorListener.onError("Error sending initial message, closing.");
                }
                e.printStackTrace();
                close();
                return;
            }

            while (!this.isInterrupted()) {
                try {
                    if (translationStatus == -1){
                        return;
                    }
                    while (translationStatus == 1) {
                        sendData();
                    }
                } catch (JSONException e) {
                    if (errorListener != null) {
                        errorListener.onError("Json parsing error.");
                    }
                    e.printStackTrace();
                } catch (IOException e) {
                    if (translationStatus != -1){
                        if (errorListener != null) {
                            errorListener.onError("Sending data error.");
                        }
                        e.printStackTrace();
                    }
                }
                synchronized (statusBell) {
                    try {
                        statusBell.wait(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
        }

        private void sendData() throws JSONException, IOException {
            if (translationStatus != 1) {
                return;
            }
            byte[] data = cameraPreview.getImageBuffer();
            String dataS = Arrays.toString(data);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("data", dataS);
            double[] geo = new double[2];
            if (lastLocation != null) {
                geo[0] = lastLocation.getAltitude();
                geo[1] = lastLocation.getLatitude();
            } else {
                geo[0] = geo[1] = -1;
            }
            jsonObject.put("geo", "[" + geo[0] + ", " + geo[1] + "]");
            outputStream.write(jsonObject.toString() + "\n");
            outputStream.flush();
        }

        private void sendInitialMessage() throws IOException {
            if (translationStatus == -1) {
                return;
            }
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("length", cameraPreview.getPreviewLength());
                jsonObject.put("width", cameraPreview.getPreviewWidth());
                jsonObject.put("height", cameraPreview.getPreviewHeight());
//                jsonObject.put("length", cameraPreview.getAc)
            } catch (JSONException e) {
                if (errorListener != null) {
                    errorListener.onError(">>> Error making json in initial message.");
                }
                e.printStackTrace();
                return;
            }

            outputStream.write(jsonObject.toString() + "\n");
            outputStream.flush();
        }

        private void sendStop() throws IOException {
            if (translationStatus == -1){
                return;
            }
            outputStream.write(Arrays.toString(STOP_COMMAND.getBytes()));
            outputStream.flush();
        }
    }

    // Setters-getters
    private void setTranslationStatus(int translationStatus) {
        this.translationStatus = translationStatus;
        synchronized (statusBell) {
            statusBell.notify();
        }
        synchronized (ipAndHostBell) {
            ipAndHostBell.notify();
        }
    }

    public int getTranslationStatus() {
        return translationStatus;
    }

    public void setHostIp(String hostIp) {
        this.hostIp = hostIp;
        synchronized (ipAndHostBell) {
            ipAndHostBell.notify();
        }
    }

    public void setHostPort(int hostPort) {
        this.hostPort = hostPort;
        synchronized (ipAndHostBell) {
            ipAndHostBell.notify();
        }
    }

    // Location Listener
    @Override
    public void onLocationChanged(Location location) {
        this.lastLocation = location;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

}
