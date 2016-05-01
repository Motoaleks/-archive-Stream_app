package io;

import com.google.gson.*;
import data.Abstractions.PictureData;
import data.Abstractions.StreamData;
import data.Listeners.ErrorListener;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Collection;
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
    private BufferedInputStream inputStream;
    private BufferedOutputStream outputStream;
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
                outputStream = new BufferedOutputStream(socket.getOutputStream());
                inputStream = new BufferedInputStream(socket.getInputStream());

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
                inputStream.close();
                outputStream.close();
                socket.close();
            }
        } catch (IOException e) {
            errorListener.onError("Incoming connection error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean isIncomingJson() {
        try {
            int len = 0;
            byte[] buff = new byte[256];

            // чтение Json
            len = inputStream.read(buff);
            String message = new String(buff, 0, len);
            JsonParser parser = new JsonParser();
            JsonElement element = null;
            try {
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
        user = new UserStream(server, inputStream, outputStream);
        user.initialize(pictureData);
        return true;
    }

    private void sendStreams(List<StreamData> streams) {
        // создание сообщения
        JsonArray streamList = new JsonArray();
        for (StreamData userData : streams) {
            JsonObject currentUser = new JsonObject();
            currentUser.addProperty("name", userData.getName());
            currentUser.addProperty("id", userData.getId());
        }
        // попытка отправки
        try {
            outputStream.write(streamList.toString().getBytes());
        } catch (IOException e) {
            errorListener.onError("Warning on incoming connection: Can not send stream list, probably user disconnected.");
        }
    }
}
