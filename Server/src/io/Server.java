package io;

import data.Abstractions.StreamData;
import data.BufferManager;
import data.Listeners.*;
import javafx.application.Platform;
import ui.main.Main;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

/**
 * Created by Aleksand Smilyanskiy on 06.04.2016.
 * "The more we do, the more we can do." ©
 */
public class Server extends Thread{
    // parent
    private Main main;

    // Компоненты сервера
    private ServerSocket serverSocket;
    private StreamPool streamPool;
    // Listeners
//    private DataListener dataListener; // converter
    private PoolListener poolListener; // pool
    private ErrorListener errorListener; // errors
    private ServerListener serverListener; // current server
    // Параметрыы сервера
    private int port = 8585;
    private static final int MAX_USERS = 10;

    public Server(Main main){
        super();
        this.main = main;
        streamPool = new StreamPool(this.poolListener);
        streamPool.setErrorListener(errorListener);
    }
//
//    public Server(PoolListener poolListener) {
//        this();
//        this.poolListener = poolListener;
//    }
//
//    public Server(PoolListener poolListener, int port) {
//        this(poolListener);
//        this.port = port;
//    }


    @Override
    public void run() {
        super.run();
        Platform.runLater(() -> main.onServerThinking());
        try {
//            serverSocket = new ServerSocket(port);
            serverSocket = new ServerSocket(port);
            Platform.runLater(() -> main.onServerOpen());
            while (!isInterrupted()) {
                Socket inputConnection = serverSocket.accept();

                // принимаем и обрабатываем входящее соединение по одному из шаблонов:
                // 1 - регистрируем стрим
                // 2 - отсылаем список стримов, закрываем соединение
                // 3 - ошибочный запрос, закрываем соединение
                HandleIncomingConnection handleIncomingConnection = new HandleIncomingConnection(this, streamPool, inputConnection);
                handleIncomingConnection.start();
            }
        } catch (IOException e) {
            errorListener.onError("Server going offline.");
        }
    }

    public ErrorListener getErrorListener() {
        return errorListener;
    }

    // Server options
    public List<StreamData> getStreams(){
        return streamPool.getAllStreams();
    }
    public void deleteStream(String id) {
        streamPool.removeUserStream(id);
    }
    public UserStream openStream(String id){
        if (!streamPool.heartbeatStream(id)){
            errorListener.onError("Warning in opening stream: user is not active anymore.");
            return null;
        }
        UserStream user = streamPool.getUserStream(id);
        try{
            user.requestStart();
        } catch (IOException e) {
            e.printStackTrace();
            errorListener.onError("Error in opening stream: user is not active anymore.");
        }
        return user;
    }
    public void closeStream(String id){
        streamPool.removeUserStream(id);
    }
    public void heartbeatStreams(){
        streamPool.heartbeatStreams();
    }
    public void closeServer(){
        // Здесь операции по завершению работы сервера
        try {
            serverSocket.close();
            streamPool.closeAllConnections();
        } catch (IOException e) {
            errorListener.onError("Error on running server: " + e.getMessage());
            e.printStackTrace();
        }
        Platform.runLater(() -> main.onServerClosed());
        interrupt();
    }


    // Listener setters
    public void setPoolListener(PoolListener poolListener) {
        this.poolListener = poolListener;
        if (streamPool != null){
            streamPool.setPoolListener(poolListener);
        }
    }
    public void setErrorListener(ErrorListener errorListener) {
        this.errorListener = errorListener;
        if (streamPool != null){
            streamPool.setErrorListener(errorListener);
        }
    }
    public void setServerListener(ServerListener serverListener) {
        this.serverListener = serverListener;
    }
}
