//package com.example.aleksandr.socketstreamer;
//
//import android.location.LocationListener;
//import android.util.Log;
//
//import com.example.aleksandr.socketstreamer.supporting.CameraPreview;
//import com.google.gson.JsonElement;
//import com.google.gson.JsonObject;
//import com.google.gson.JsonParseException;
//import com.google.gson.JsonParser;
//
//import java.io.BufferedInputStream;
//import java.io.BufferedOutputStream;
//import java.io.IOException;
//import java.net.InetSocketAddress;
//import java.net.Socket;
//import java.net.SocketTimeoutException;
//
///**
// * Сокет-клиент для отправки данных на сервер
// */
//public class SocketClient extends Thread {
//    /**
//     * Тэг для логера
//     */
//    private static final String TAG = "socket";
//    /**
//     * Слушатель изменения геолокации
//     */
//    LocationListener locationListener;
//    /**
//     * Сам сокет
//     */
//    private Socket mSocket;
//    /**
//     * Превью для отображения
//     */
//    private CameraPreview mCameraPreview;
//    /**
//     * Дефолтный ip
//     */
//    private String mIP = "77.94.175.81";
//    /**
//     * Дефолтный порт
//     */
//    private int mPort = 8585;
//
//    /**
//     * Создание сокет клиента для отправки данных на сервер и запуск
//     *
//     * @param preview Превью для отображения
//     * @param ip      Ip сервера
//     * @param port    Порт сервера
//     */
//    public SocketClient(CameraPreview preview, String ip, int port) {
//        mCameraPreview = preview;
//        mIP = ip;
//        mPort = port;
//        start();
//    }
//
//    /**
//     * Создание сокет клиента и запуск с дефолтным значение порта и сервер ip
//     *
//     * @param preview Превью для отображения
//     */
//    public SocketClient(CameraPreview preview) {
//        mCameraPreview = preview;
//        start();
//    }
//
//    /**
//     * Запуск подсоединения к серверу и обмена информацией
//     */
//    @Override
//    public void run() {
//        super.run();
//
//        try {
//            // подключение к серверу
//            mSocket = new Socket();
//            mSocket.connect(new InetSocketAddress(mIP, mPort), 10000);
//
//            // потоки ввода-вывода
//            BufferedOutputStream outputStream = new BufferedOutputStream(mSocket.getOutputStream());
//            BufferedInputStream inputStream = new BufferedInputStream(mSocket.getInputStream());
//
//            // создаём Json объект для отправки информации о картинке
//            JsonObject jsonObj = new JsonObject();
//            jsonObj.addProperty("type", "data");
//            jsonObj.addProperty("length", mCameraPreview.getPreviewLength());
//            jsonObj.addProperty("width", mCameraPreview.getPreviewWidth());
//            jsonObj.addProperty("height", mCameraPreview.getPreviewHeight());
//
//            // буфер для получения сообщений
//            byte[] buff = new byte[256];
//            // длина сообщения на вход
//            int len = 0;
//            // сообщение в String виде
//            String msg = null;
//
//            // отправка данных о картинке с телефона
//            outputStream.write(jsonObj.toString().getBytes());
//            outputStream.flush();
//
//            // пока не достигнут конец стрима
//            while ((len = inputStream.read(buff)) != -1) {
//                // получение String из буфера байтов
//                msg = new String(buff, 0, len);
//
//                // Json распарсирование
//                JsonParser parser = new JsonParser();
//                boolean isJSON = true;
//                JsonElement element = null;
//                try {
//                    // парсим сообщение
//                    element = parser.parse(msg);
//                } catch (JsonParseException e) {
//                    Log.e(TAG, "exception: " + e);
//                    isJSON = false;
//                }
//
//                // Если удалось распарсить и сообщение есть
//                if (isJSON && element != null) {
//                    // получение состояния сервера
//                    JsonObject obj = element.getAsJsonObject();
//                    element = obj.get("state");
//                    // если всё хорошо и сервер сказал да
//                    if (element != null && element.getAsString().equals("ok")) {
//                        // отправляем инфу до последнего
//                        while (true) {
//
//                            if (mSocket.isOutputShutdown()) {
//                                break;
//                            }
//
//                            // отправляем последний благоприятный фрейм
//                            outputStream.write(mCameraPreview.getImageBuffer());
//                            outputStream.flush();
//
//                            // если пора прекратить отправку
//                            if (Thread.currentThread().isInterrupted())
//                                break;
//                        }
//
//                        // выход по окончанию отправки
//                        break;
//                    }
//                } else {
//                    // выход по недоступному серверу
//                    break;
//                }
//            }
//
//            // закрытие каналов
//            outputStream.close();
//            inputStream.close();
//        }
//        // сервер не доступен для подключения
//        catch (SocketTimeoutException e) {
//            Log.e(TAG, "Socket-server is not ready. \n" + e.toString());
//        } catch (Exception e) {
//            Log.e(TAG, e.toString());
//        } finally {
//            try {
//                // закрытие сокетов
//                mSocket.close();
//                mSocket = null;
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    /**
//     * Закрытие потока
//     */
//    public void close() {
//        if (mSocket != null) {
//            try {
//                mSocket.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    /**
//     * Установить слушатель геолокации
//     *
//     * @param locationListener слушатель геолокации
//     */
//    public void setLocationListener(LocationListener locationListener) {
//        this.locationListener = locationListener;
//    }
//}
