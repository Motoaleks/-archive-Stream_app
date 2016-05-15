package io;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import data.Abstractions.Coordinate;
import data.Abstractions.PictureData;
import data.Abstractions.StreamData;
import data.BufferManager;
import data.Listeners.*;

import java.io.*;
import java.net.Socket;
import java.util.Map;

/**
 * Created by Aleksand Smilyanskiy on 05.04.2016.
 * "The more we do, the more we can do." ©
 */
public class UserStream extends Thread {
    // Main objects
    private Socket socket;
    private StreamData streamData;
    private final Server server;
    private BufferManager bufferManager;

    // Streams
    private BufferedReader inputStream;
    private BufferedInputStream bufferedInputStream;
    private JsonReader jsonReader;
    private BufferedWriter outputStream;

    // Listeners
    private ErrorListener errorListener;
    private StreamListener streamListener;
    //    private DataListener dataListener;
    private GeoListener geoListener;
    private SimpleStreamListener simpleStreamListener;

    // Properties
    private int streamStatus = -1; // -1 = offline, 0 = pending or waiting, 1 = online
    private int previousStatus = -1;
    private final Object statusWaiter = new Object();
    private final Object heartbeatWaiter = new Object();
    private static final String STOP_COMMAND = "STOP";
    private static final int AWAITING_TIME = 10000;

    // For GSON Parsing
    private class DataResponse {
        public Map<String, Data> descriptor;
    }

    private class Data {
        private byte[] data;
        private double[] geo;
    }


    public UserStream(Server server, Socket socket) {
        this.socket = socket;
        this.server = server;
        try {
            this.inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.bufferedInputStream = new BufferedInputStream(socket.getInputStream());
            this.jsonReader = new JsonReader(inputStream);
            this.jsonReader.setLenient(true);
            this.outputStream = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        streamData = new StreamData();
    }


    @Override
    public void run() {
        super.run();
        Gson gson = new Gson();
        setStreamStatus(0);
        while (!isInterrupted()) {
            try {
                switch (streamStatus) {
                    // передача картинки осуществляется
                    case 1: {
                        takeImageBuff(gson);
                        break;
                    }

                    // ожидание команд
                    case 0: {
                        // функция пинга со временем по окончании AWAITING_TIME
                        synchronized (statusWaiter) {
                            try {
                                statusWaiter.wait(AWAITING_TIME);
                            } catch (InterruptedException e) {
                                if (errorListener != null) {
                                    errorListener.onError("Waiting for status interrupted.");
                                }
                                e.printStackTrace();
                            }

                            // если изменился
                            if (streamStatus != 0) {
                                break;
                            }

                            // отсылка пинга
                            sendHeartbeat();
                            // если пинг не прошёл
                            if (!waitForHeartbeat()) {
                                setStreamStatus(-1);
                                synchronized (heartbeatWaiter) {
                                    heartbeatWaiter.notify();
                                }
                                break;
                            }
                        }
                        break;
                    }

                    // отключение
                    case -1:
                        interrupt();
                }

            } catch (IOException e) {
                // TODO: 07.05.2016 Here we need to delete the stream
                server.deleteStream(getStreamData().getId());
                if (streamStatus != -1) {
                    if (errorListener != null)
                        errorListener.onError("Error on client <" + getStreamData().getId() + ">:" + e.getMessage());
                    e.printStackTrace();
                }
                break;
            } catch (Exception ignored) {
                server.deleteStream(getStreamData().getId());
            }
        }
        closeStream();
    }

    // Запросы
    public void requestWait() throws IOException {
        if (outputStream == null || streamStatus == -1 || streamStatus == 0) {
            return;
        }
        setStreamStatus(0);
        // создаём Json объект для отправки обратно информации о необходимости подождать
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("state", "wait");
        sendJsonObject(jsonObject);
        // ждём до конца потока вещания
//        waitForEnd();
    }

    public void requestStart() throws IOException {
        if (outputStream == null || streamStatus == -1 || streamStatus == 1) {
            return;
        }
        // создаём Json объект для отправки обратно информации о необходимости подождать
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("state", "start");
        sendJsonObject(jsonObject);
        setStreamStatus(1);
    }

    public boolean heartbeatStream() throws IOException {
        // проверяет доступность клиента
        // если сейчас идёт вещание - значит всё в порядке
        if (streamStatus == 1) {
            return true;
        }
        // если вещание закончено
        if (streamStatus == -1) {
            return false;
        }

        // иначе - ждём ответа
        synchronized (heartbeatWaiter) {
            try {
                heartbeatWaiter.wait();
                if (streamStatus == -1) {
                    return false;
                } else {
                    return true;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    private void sendHeartbeat() throws IOException {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("heartbeat", "request");
        sendJsonObject(jsonObject);
    }

    private boolean waitForHeartbeat() throws IOException {
        synchronized (jsonReader) {
            // чтение строки
            try {
                jsonReader.beginObject();
                String name = jsonReader.nextName();
                // если не ответ
                if (!"heartbeat".equals(name)) {
                    jsonReader.endObject();
                    return false;
                } else {
                    // если правильный ответ
                    if ("answer".equals(jsonReader.nextString())) {
                        jsonReader.endObject();
                        synchronized (heartbeatWaiter) {
                            heartbeatWaiter.notifyAll();
                        }
                        return true;
                    }
                    // если неправильный ответ
                    else {
                        jsonReader.endObject();
                        return false;
                    }
                }
            } catch (Exception e) {
                return false;
            }
        }
    }

    public void closeStream() {
        try {
            socket.getOutputStream().close();
            try {
                socket.getInputStream().close();
            } catch (IOException ignored) {
            }
            socket.close();
        } catch (IOException ignored) {
        }

        if (streamStatus != -1) {
            setStreamStatus(-1);
        }
    }


    // Ответы
    public void sendInfo(String id) throws IOException {
        // TODO: 07.05.2016 Test this
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", id);
        sendJsonObject(jsonObject);
    }


    // Options
    private void takeImageBuff(Gson gson) throws IOException {
        if (bufferManager == null) {
            errorListener.onError("Error, client <" + getStreamData().getId() + "> buffer not set.");
            setStreamStatus(0);
        }

        jsonReader.beginObject();
        byte[] data = null;
        double[] geo = null;
        while (jsonReader.hasNext()) {
            switch (jsonReader.nextName()) {
                case "data": {
                    data = getByteArray(jsonReader.nextString());
                    break;
                }
                case "geo": {
                    geo = getDoubleArray(jsonReader.nextString());
                    break;
                }
            }
        }
        jsonReader.endObject();
        Coordinate newGeo = new Coordinate(geo);
        if (geoListener != null)
            geoListener.onGeoChange(new Coordinate(geo));
        if (bufferManager != null)
//            bufferManager.fillBuffer(data, data.length);
            bufferManager.completeImageReceived(data);


//        jsonReader.nextName();
//        DataResponse dataResponse;
//        synchronized (jsonReader) {
//            dataResponse = gson.fromJson(jsonReader, DataResponse.class);
//        }
//        byte[] data = dataResponse.descriptor.get("data").data;
//        if (dataResponse.descriptor.containsKey("geo")) {
//            geoListener.onGeoChange(new Coordinate(dataResponse.descriptor.get("geo").geo));
//        }
//        bufferManager.fillBuffer(data, data.length);
    }

    private byte[] getByteArray(String message) {
        String[] bytes = message.split(",");
        bytes[0] = bytes[0].replaceFirst("\\[", "");
        bytes[bytes.length - 1] = bytes[bytes.length - 1].replaceFirst("]", "");
        byte[] actual = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            actual[i] = Byte.parseByte(bytes[i].replaceFirst(" ", ""));
        }
        return actual;
    }

    private double[] getDoubleArray(String message) {
        String[] doubles = message.split(",");
        doubles[0] = doubles[0].replaceFirst("\\[", "");
        doubles[doubles.length - 1] = doubles[doubles.length - 1].replaceFirst("]", "");
        doubles[doubles.length - 1] = doubles[doubles.length - 1].replaceFirst(" ", "");
        double[] actual = new double[doubles.length];
        for (int i = 0; i < doubles.length; i++) {
            actual[i] = Double.parseDouble(doubles[i]);
        }
        return actual;
    }

    private boolean waitForEnd() throws IOException {
        // ждёт конца передачи изображений
        synchronized (bufferedInputStream) {
            byte[] stopCommand = STOP_COMMAND.getBytes();
            byte[] onebyteArray = new byte[1];
            int i = 0;
            while (bufferedInputStream.read(onebyteArray) != -1) {
                if (onebyteArray[0] == stopCommand[i]) {
                    i += 1;
                    if (i == STOP_COMMAND.length()) {
                        return true;
                    }
                } else {
                    i = 0;
                }
            }
            return false;
        }
    }


    // Helpers
    private void sendJsonObject(JsonObject jsonObject) throws IOException {
        synchronized (outputStream) {
            outputStream.write(jsonObject.toString());
            outputStream.flush();
        }
    }


    //Setters
    public void setPictureData(PictureData pictureData) {
        streamData.setPictureData(pictureData);
    }

    private void setStreamStatus(int streamStatus) {
        previousStatus = this.streamStatus;
        this.streamStatus = streamStatus;
        if (previousStatus == 0 && streamStatus == 1) {
            try {
                bufferedInputStream.skip(bufferedInputStream.available());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (streamListener != null) {
            streamListener.onTranslationStatusChanged(streamStatus);
        }
        if (streamStatus == -1 && simpleStreamListener != null) {
            simpleStreamListener.onStreamShutdown(getStreamData());
        }
        synchronized (statusWaiter) {
            statusWaiter.notify();
        }
    }

    public void setStreamListener(StreamListener streamListener) {
        this.streamListener = streamListener;
    }

    public void setDataListener(DataListener dataListener) {
        bufferManager.setOnDataListener(dataListener);
    }

    public void setBufferManager(BufferManager bufferManager) {
        this.bufferManager = bufferManager;
        if (bufferManager != null) {
            this.bufferManager.setPictureData(streamData.getPictureData());
        }
    }

    public void setGeoListener(GeoListener geoListener) {
        this.geoListener = geoListener;
    }

    public void setErrorListener(ErrorListener errorListener) {
        this.errorListener = errorListener;
    }

    public void setSimpleStreamListener(SimpleStreamListener simpleStreamListener) {
        this.simpleStreamListener = simpleStreamListener;
    }

    //Getters
    public StreamListener getStreamListener() {
        return streamListener;
    }

    public StreamData getStreamData() {
        return streamData;
    }

    public GeoListener getGeoListener() {
        return geoListener;
    }

    public DataListener getDataListener() {
        return bufferManager.getDataListener();
    }
}
