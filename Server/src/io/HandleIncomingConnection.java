package io;

import com.google.gson.*;
import data.Abstractions.PictureData;
import data.Abstractions.StreamData;
import data.BufferManager;
import data.Listeners.ErrorListener;

import java.io.*;
import java.net.Socket;
import java.util.List;

/**
 * Created by Aleksand Smilyanskiy on 06.04.2016.
 * "The more we do, the more we can do." ©
 */
public class HandleIncomingConnection extends Thread {
    private Socket socket;
    private Server server;
    private JsonObject object;
    private StreamPool pool;
    private UserStream user;
    private BufferedReader inputStream;
    private BufferedWriter outputStream;
    private ErrorListener errorListener;

    public HandleIncomingConnection(Server server, StreamPool pool, Socket incoming) {
        socket = incoming;
        errorListener = server.getErrorListener();
        this.server = server;
        this.pool = pool;
    }

    @Override
    public void run() {
        super.run();
        try {
            // не должен меняться до конца этой операции
            synchronized (socket) {
                outputStream = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));


                // проверка на json запрос
                boolean isJson = isIncomingJson();


                // анализируем тип запроса если json
                if (isJson) {
                    if (analyzeIncomingJson()) {
                        // запрос на регистрацию стрима
                        synchronized (pool) {
                            pool.addUserStream(user);
                            return;
                        }
                    } else {
                        // запрос на получение списка стримов
                        sendStreams(pool.getAllStreams());
                    }
                }

                // закрываем соединения если запрос был краткосрочный или неправильный
                try {
                    inputStream.close();
                    outputStream.close();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    errorListener.onError("Error in disconnecting wrong connection.");
                }
            }
        } catch (IOException e) {
            errorListener.onError("Incoming connection error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean isIncomingJson() {
        try {
            // чтение Json
            String message = inputStream.readLine();
            JsonParser parser = new JsonParser();
            JsonElement element = null;
            try {
                if (message == null){
                    throw  new JsonParseException("Message is null.");
                }
                element = parser.parse(message);
            } catch (JsonParseException e) {
                errorListener.onError("Incoming connection refused: not a json.");
                return false;
            }
            // нет входного сообщения -> это или неправильное соединение
            if (element == null) {
                errorListener.onError("Incoming connection refused: no message.");
                return false;
            }

            object = element.getAsJsonObject();
        } catch (IOException e) {
            errorListener.onError("Incoming connection error: " + e.getMessage());
            return false;
        }
        return true;
    }

    private boolean analyzeIncomingJson() {
        // класс с информацией о изображении
        PictureData pictureData = new PictureData();

        // Если чего-то нет это неправильный запрос или запрос на получение списка стримов
        try {
            pictureData.setFrameLength(object.get("length").getAsInt());
            pictureData.setWidth(object.get("width").getAsInt());
            pictureData.setHeight(object.get("height").getAsInt());
        } catch (NullPointerException e) {
            return false;
        }
        if (!pictureData.checkCorrect()) {
            return false;
        }

        // Значит это не запрос на регистрацию стрима
        user = new UserStream(server, socket);
        user.setPictureData(pictureData);
        user.setBufferManager(new BufferManager(pictureData));
        return true;
    }

    private void sendStreams(List<StreamData> streams) {
        // создание сообщения
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("info", "streamData");
        JsonArray streamList = new JsonArray();
        for (StreamData userData : streams) {
            JsonObject currentUser = new JsonObject();
            currentUser.addProperty("name", userData.getName());
            currentUser.addProperty("id", userData.getId());
            streamList.add(currentUser);
        }
        jsonObject.add("streams", streamList);
        // попытка отправки
        try {
            outputStream.write(jsonObject.toString() + "\n");
            outputStream.flush();
        } catch (IOException e) {
            errorListener.onError("Warning on incoming connection: Can not send stream list, probably user disconnected.");
        }
    }
}
