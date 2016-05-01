package io;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import data.Abstractions.PictureData;
import data.Abstractions.StreamData;
import data.BufferManager;
import data.Listeners.DataListener;
import data.Listeners.ErrorListener;
import data.Listeners.GeoListener;
import data.Listeners.StreamListener;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Created by Aleksand Smilyanskiy on 05.04.2016.
 * "The more we do, the more we can do." ©
 */
public class UserStream extends Thread {
    /**
     * Команда для стрима проверить переменные
     */
    private final Object callUpdate = new Object();
    /**
     * Сокет текущего клиента
     */
    Socket client = null;
    /**
     * Сервер, на который заходит юзер
     */
    private SocketServer mServer;
    /**
     * Интерфейс для ожидания процесса перекодировки потока байтов в картинки
     */
    private DataListener mDataListener;
    /**
     * Слушатель изменения геопозиции
     */
    private GeoListener mGeoListener;
    /**
     * Устанавливается в true если необходимо закрыть поток вещания
     */
    private boolean isClosed = false;


    /**
     * Установка дефолтного слушателя и переведения стрима в режим ожидания
     */
    public void setDefaultDataListener() {
        this.mDataListener = new DataListener() {
            @Override
            public void onDirty(BufferedImage bufferedImage) {

            }
        };
        this.isOn = false;
        makeCall();
    }


    public void makeCall() {
        this.callUpdate.notify();
    }


    //----- To stay
    private StreamData streamData;
    private StreamListener streamListener;
    private final Server server;
    private BufferedInputStream inputStream;
    private BufferedOutputStream outputStream;
    private ErrorListener errorListener;
    private boolean isOn = false;
    private BufferManager bufferManager;


    public UserStream(Server server, BufferedInputStream inputStream, BufferedOutputStream outputStream) {
        this.server = server;
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        streamData = new StreamData();
    }

    @Override
    public void run() {
        super.run();

        try {
            while (!Thread.currentThread().isInterrupted()) {
                System.out.println("Client initiated, id " + streamData.getId());


                // инициализация потоков ввода-вывода
                inputStream = new BufferedInputStream(client.getInputStream());
                outputStream = new BufferedOutputStream(client.getOutputStream());

                // буфер для сообщений
                byte[] buff = new byte[256];
                byte[] imageBuff = null;
                // длина нового сообщения
                int len = 0;
                String msg = null;
                // пока можно прочитать сообщение (то есть пока оно есть)
                while ((len = inputStream.read(buff)) != -1) {
                    // читаем сообщение
                    msg = new String(buff, 0, len);
                    // Анализируем JSON
                    JsonParser parser = new JsonParser();
                    boolean isJSON = true;
                    JsonElement element = null; //текущее json распарсенное сообщение
                    try {
                        element = parser.parse(msg);
                    } catch (JsonParseException e) {
                        System.out.println("exception: " + e);
                        isJSON = false;
                    }
                    // если получилось распарсить и есть json элемент
                    if (isJSON && element != null) {
                        // получение параметров из объекта
                        JsonObject obj = element.getAsJsonObject();
                        element = obj.get("type");
                        if (element != null && element.getAsString().equals("data")) {
                            element = obj.get("length");
                            int length = element.getAsInt();
                            element = obj.get("width");
                            int width = element.getAsInt();
                            element = obj.get("height");
                            int height = element.getAsInt();

                            // зная длину одного фрейма создаём буфер
                            imageBuff = new byte[length];
                            // создаём менеджер буферов
                            bufferManager = new BufferManager(streamData.getPictureData());
                            break;
                        }
                    }
                    // если Json не получен запишем в буфер ошибок
                    else {
                        mServer.writeError(streamData.getId(), "Initial json could not be parsed correctly.");
                        break;
                    }
                }

                // если буфер создался нормально
                if (imageBuff != null) {
                    // Просим клиента подождать
                    sendWait();

                    // Установим заглушку
                    setDefaultDataListener();
                    while (!isClosed) {

                        // ждём изменения DataListener-a
                        callUpdate.wait();

                        // Если всё равно не включён - возвращаемся
                        if (!isOn)
                            continue;

                        // Сообщаем клиенту о старте
                        sendStart();
                        while ((len = inputStream.read(imageBuff)) != -1 && isOn) {
                            bufferManager.fillBuffer(imageBuff, len);
                        }
                        // пользователь закрыл трансляцию
                        if (len == -1)
                            break;

                        // Возвращаем клиента в ожидание
                        sendWait();
                    }

                    isOn = false;
                    mServer.closeStream(streamData.getId());
                }

                // если был менеджер буферов - закрытие его для данного клиента
                if (bufferManager != null) {
                    bufferManager.close();
                }
            }

        } catch (InterruptedException e) {
            mServer.writeError(streamData.getId(), "Error waiting for DataListener.");
        } catch (IOException e) {
            mServer.writeError(streamData.getId(), "Error in taking information from user.");
        } finally

        {
            try {
                // закрытие выходящего канала
                if (outputStream != null) {
                    outputStream.close();
                    outputStream = null;
                }

                // закрытие входящего канала
                if (inputStream != null) {
                    inputStream.close();
                    inputStream = null;
                }

                // закрытие сокета клиента
                if (client != null) {
                    client.close();
                    client = null;
                }
            } catch (IOException e) {
                e.printStackTrace();
                mServer.writeError(streamData.getId(), "Error closing streams.");
            }

        }
    }

    public void sendWait() throws IOException {
        if (outputStream == null) {
            return;
        }
        // создаём Json объект для отправки обратно информации о необходимости подождать
        JsonObject jsonObj = new JsonObject();
        jsonObj.addProperty("state", "wait");
        outputStream.write(jsonObj.toString().getBytes());
        outputStream.flush();
    }

    public void sendStart() throws IOException {
        if (outputStream == null) {
            return;
        }
        // создаём Json объект для отправки обратно информации о необходимости подождать
        JsonObject jsonObj = new JsonObject();
        jsonObj.addProperty("state", "start");
        outputStream.write(jsonObj.toString().getBytes());
        outputStream.flush();
    }

    public void initialize(PictureData pictureData) {
        streamData.setPictureData(pictureData);
    }

    public void disconnect() {
        // TODO: 06.04.2016 do method
        streamListener.onStreamDisconnected(this);
    }

    private void startTranslation() {
        if (bufferManager == null) {
            errorListener.onError("Can not start stream: no buffer set.");
        }

        try {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("state", "start");
            outputStream.write(jsonObject.toString().getBytes());

            setIsOn(true);

            // Thread по получению картинки от пользователя и передаче её в BufferManager
            Thread handleInfo = new Thread() {
                @Override
                public void run() {
                    super.run();
                    // начать
                    streamListener.onTranslationStatusChanged(true);

                    // зная длину одного фрейма создаём буфер
                    byte[] imageBuff = new byte[streamData.getPictureData().getFrameLength()];
                    // конфигурируем
                    bufferManager = new BufferManager(streamData.getPictureData());
                    bufferManager.setOnDataListener(mDataListener);

                    // TODO: 06.04.2016 REDO INCOMING MESSAGING
                    // процесс чтения, пока не завершили просмотр или не достигли конца трансляции
                    int len = 0;
                    try {
                        while ((len = inputStream.read(imageBuff)) != -1 && isOn)
                            bufferManager.fillBuffer(imageBuff, len);
                    } catch (IOException e) {
                        errorListener.onError("Error reading new images from user.");
                        e.printStackTrace();
                    }

                    setBufferManager(null);
                    // и кончить
                    streamListener.onTranslationStatusChanged(false);
                }
            };
            handleInfo.start();
        } catch (IOException e) {
            errorListener.onError("Error sending <start translation> message: " + e.getMessage());
        }
    }

    private void stopTranslation() {
        // TODO: 06.04.2016 do method
        setIsOn(false);
    }

    public StreamListener getStreamListener() {
        return streamListener;
    }

    public void setStreamListener(StreamListener streamListener) {
        this.streamListener = streamListener;
    }

    public void setOnDataListener(DataListener listener) {
        if (listener == null) {
            return;
        }
        this.mDataListener = listener;
        this.isOn = true;
        makeCall();
    }

    private void setIsOn(boolean isOn) {
        this.isOn = isOn;
    }

    public void setBufferManager(BufferManager bufferManager) {
        this.bufferManager = bufferManager;
        if (bufferManager != null) {
            this.bufferManager.setPictureData(streamData.getPictureData());
        }
    }

    public StreamData getStreamData() {
        return streamData;
    }
}
