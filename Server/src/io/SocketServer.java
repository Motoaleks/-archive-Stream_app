package io;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import data.BufferManager;
import data.Listeners.DataListener;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * Сервер для работы с подключениями клиентов
 *
 * @author Aleksandr Smilyanskiy
 * @version 1.0
 */
public class SocketServer extends Thread {
    // создание переменных для входных и выходных обменов информацией
    BufferedInputStream inputStream = null;
    BufferedOutputStream outputStream = null;
    // создание переменной текущего соединения
    Socket socket = null;
    //ошибки
    ByteArrayOutputStream errorArray;
    PrintWriter errors;
    /**
     * Сокет-сервер входящих соединений для обработки подключений
     */
    private ServerSocket mServer;
    /**
     * Интерфейс для ожидания процесса перекодировки потока байтов в картинки
     */
    private DataListener mDataListener;
    /**
     * Менеджер буферов по работе с потоками данных
     */
    private BufferManager mBufferManager;
    /**
     * Ширина и высота изображения с Andtoid
     */
    private int mWidth, mHeight;
    /**
     * Порт для сервера
     */
    private int port = 8585;

    public SocketServer() {

    }

    public SocketServer(int port) {
        this.port = port;
    }

    /**
     * Обработка входящих запросов
     */
    @Override
    public void run() {
        super.run();

        System.out.println("server's waiting");

        try {
            // включение сервера
            mServer = new ServerSocket(port);
            while (!Thread.currentThread().isInterrupted()) {
                // если было соединение - подчищение информации от предыдущего контакта
                if (errorArray != null)
                    errorArray.reset();
                else {
                    errorArray = new ByteArrayOutputStream();
                    errors = new PrintWriter(errorArray);
                }

                // принятие нового клиента
                socket = mServer.accept();
                System.out.println("new socket");

                // инициализация потоков ввода-вывода
                inputStream = new BufferedInputStream(socket.getInputStream());
                outputStream = new BufferedOutputStream(socket.getOutputStream());

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
                            mWidth = element.getAsInt();
                            element = obj.get("height");
                            mHeight = element.getAsInt();

                            // зная длину одного фрейма создаём буфер
                            imageBuff = new byte[length];
                            // создаём менеджер буферов
                            mBufferManager = new BufferManager(length, mWidth, mHeight);
                            mBufferManager.setOnDataListener(mDataListener);
                            break;
                        }
                    }
                    // если Json не получен запишем в буфер ошибок
                    else {
                        // TODO: 01.04.2016 Переделать механизм обработки ошибки Json приёма
                        errorArray.write(buff, 0, len);
                        break;
                    }
                }

                // если буфер создался нормально
                if (imageBuff != null) {
                    // создаём Json объект для отправки обратно информации о нормальном завершении
                    JsonObject jsonObj = new JsonObject();
                    jsonObj.addProperty("state", "ok");
                    outputStream.write(jsonObj.toString().getBytes());
                    outputStream.flush();

                    // цикл по чтению приходящих фреймов
                    while ((len = inputStream.read(imageBuff)) != -1) {
                        mBufferManager.fillBuffer(imageBuff, len);
                    }
                }

                // если был менеджер буферов - закрытие его для данного клиента
                if (mBufferManager != null) {
                    mBufferManager.close();
                }
            }

        } catch (SocketException ignored) {
        } catch (IOException e) {
            // TODO: 01.04.2016 Переделать механизм информировании об ошибках
            e.printStackTrace();
        } finally {
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
                if (socket != null) {
                    socket.close();
                    socket = null;
                }

                // закрытие канала ошибок канала
                if (errorArray != null) {
                    errorArray.close();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

    /**
     * Закрывает соединения если необходимо
     *
     * @throws IOException
     */
    private void closeConnections() throws IOException {
        if (outputStream != null)
            outputStream.close();
        if (inputStream != null)
            inputStream.close();
    }

    /**
     * Установка слушателя процесса перекодирования в Rgb картинку
     *
     * @param listener слушатель проуесса перекодирования
     */
    public void setOnDataListener(DataListener listener) {
        mDataListener = listener;
    }

    public void writeError(String userId, String message) {
        errors.write("Error on user <" + userId + ">: " + message);
    }

    // Закрыть стрим
    public void closeStream(String id) {
        // TODO: 03.04.2016 Close stream
        System.out.println("Closing stream id <" + id + ">.");
    }

    /**
     * Закртыие сервера
     */
    public void closeServer() {
        try {
            mServer.close();
        } catch (IOException e) {
            errors.write("Error closing server: " + e.getMessage());
        }
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

//    class UserStream extends Thread {
//        /**
//         * Команда для стрима проверить переменные
//         */
//        private final Object callUpdate = new Object();
//        /**
//         * Сокет текущего клиента
//         */
//        Socket client = null;
//        // создание переменных для входных и выходных обменов информацией
//        BufferedInputStream inputStream = null;
//        BufferedOutputStream outputStream = null;
//        private int id = -1;
//        private boolean isOn = false;
//        /**
//         * Интерфейс для ожидания процесса перекодировки потока байтов в картинки
//         */
//        private DataListener mDataListener;
//        /**
//         * Слушатель изменения геопозиции
//         */
//        private GeoListener mGeoListener;
//        /**
//         * Менеджер буферов по работе с потоками данных
//         */
//        private BufferManager mBufferManager;
//        /**
//         * Устанавливается в true если необходимо закрыть поток вещания
//         */
//        private boolean isClosed = false;
//
//        UserStream(Socket client, int id) {
//            this.id = id;
//            this.client = client;
//        }
//
//
//        @Override
//        public void run() {
//            super.run();
//
//            try {
//                while (!Thread.currentThread().isInterrupted()) {
//                    System.out.println("Client initiated, id " + id);
//
//
//                    // инициализация потоков ввода-вывода
//                    inputStream = new BufferedInputStream(client.getInputStream());
//                    outputStream = new BufferedOutputStream(client.getOutputStream());
//
//                    // буфер для сообщений
//                    byte[] buff = new byte[256];
//                    byte[] imageBuff = null;
//                    // длина нового сообщения
//                    int len = 0;
//                    String msg = null;
//                    // пока можно прочитать сообщение (то есть пока оно есть)
//                    while ((len = inputStream.read(buff)) != -1) {
//                        // читаем сообщение
//                        msg = new String(buff, 0, len);
//                        // Анализируем JSON
//                        JsonParser parser = new JsonParser();
//                        boolean isJSON = true;
//                        JsonElement element = null; //текущее json распарсенное сообщение
//                        try {
//                            element = parser.parse(msg);
//                        } catch (JsonParseException e) {
//                            System.out.println("exception: " + e);
//                            isJSON = false;
//                        }
//                        // если получилось распарсить и есть json элемент
//                        if (isJSON && element != null) {
//                            // получение параметров из объекта
//                            JsonObject obj = element.getAsJsonObject();
//                            element = obj.get("type");
//                            if (element != null && element.getAsString().equals("data")) {
//                                element = obj.get("length");
//                                int length = element.getAsInt();
//                                element = obj.get("width");
//                                int width = element.getAsInt();
//                                element = obj.get("height");
//                                int height = element.getAsInt();
//
//                                // зная длину одного фрейма создаём буфер
//                                imageBuff = new byte[length];
//                                // создаём менеджер буферов
//                                mBufferManager = new BufferManager(length, width, height);
//                                break;
//                            }
//                        }
//                        // если Json не получен запишем в буфер ошибок
//                        else {
//                            writeError(id, "Initial json could not be parsed correctly.");
//                            break;
//                        }
//                    }
//
//                    // если буфер создался нормально
//                    if (imageBuff != null) {
//                        // Просим клиента подождать
//                        sendWait();
//
//                        // Установим заглушку
//                        setDefaultDataListener();
//                        while (!isClosed) {
//
//                            // ждём изменения DataListener-a
//                            callUpdate.wait();
//
//                            // Если всё равно не включён - возвращаемся
//                            if (!isOn)
//                                continue;
//
//                            // Сообщаем клиенту о старте
//                            sendStart();
//                            while ((len = inputStream.read(imageBuff)) != -1 && isOn) {
//                                mBufferManager.fillBuffer(imageBuff, len);
//                            }
//                            // пользователь закрыл трансляцию
//                            if (len == -1)
//                                break;
//
//                            // Возвращаем клиента в ожидание
//                            sendWait();
//                        }
//
//                        isOn = false;
//                        closeStream(id);
//                    }
//
//                    // если был менеджер буферов - закрытие его для данного клиента
//                    if (mBufferManager != null) {
//                        mBufferManager.close();
//                    }
//                }
//
//            } catch (InterruptedException e) {
//                writeError(id, "Error waiting for DataListener.");
//            } catch (IOException e) {
//                writeError(id, "Error in taking information from user.");
//            } finally
//
//            {
//                try {
//                    // закрытие выходящего канала
//                    if (outputStream != null) {
//                        outputStream.close();
//                        outputStream = null;
//                    }
//
//                    // закрытие входящего канала
//                    if (inputStream != null) {
//                        inputStream.close();
//                        inputStream = null;
//                    }
//
//                    // закрытие сокета клиента
//                    if (client != null) {
//                        client.close();
//                        client = null;
//                    }
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    writeError(id, "Error closing streams.");
//                }
//
//            }
//        }
//
//        /**
//         * Установка слушателя процесса перекодирования в Rgb картинку
//         *
//         * @param listener слушатель проуесса перекодирования
//         */
//        public void setOnDataListener(DataListener listener) {
//            if (listener == null) {
//                return;
//            }
//            this.mDataListener = listener;
//            this.isOn = true;
//            makeCall();
//        }
//
//        /**
//         * Установка дефолтного слушателя и переведения стрима в режим ожидания
//         */
//        public void setDefaultDataListener() {
//            this.mDataListener = new DataListener() {
//                @Override
//                public void onDirty(BufferedImage bufferedImage) {
//
//                }
//            };
//            this.isOn = false;
//            makeCall();
//        }
//
//        /**
//         * Зовёт стрим проснуться
//         */
//        public void makeCall() {
//            this.callUpdate.notify();
//        }
//
//        /**
//         * Отсылает команду к старту трансляции
//         *
//         * @throws IOException
//         */
//        public void sendWait() throws IOException {
//            if (outputStream == null) {
//                return;
//            }
//            // создаём Json объект для отправки обратно информации о необходимости подождать
//            JsonObject jsonObj = new JsonObject();
//            jsonObj.addProperty("state", "wait");
//            outputStream.write(jsonObj.toString().getBytes());
//            outputStream.flush();
//        }
//
//        /**
//         * Отсылает команду к завершению трансляции
//         *
//         * @throws IOException
//         */
//        public void sendStart() throws IOException {
//            if (outputStream == null) {
//                return;
//            }
//            // создаём Json объект для отправки обратно информации о необходимости подождать
//            JsonObject jsonObj = new JsonObject();
//            jsonObj.addProperty("state", "start");
//            outputStream.write(jsonObj.toString().getBytes());
//            outputStream.flush();
//        }
//    }
}
