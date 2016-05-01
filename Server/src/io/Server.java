package io;

import data.Abstractions.StreamData;
import data.BufferManager;
import data.Listeners.DataListener;
import data.Listeners.ErrorListener;
import data.Listeners.PoolListener;
import data.Listeners.ServerListener;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

/**
 * Created by Aleksand Smilyanskiy on 06.04.2016.
 * "The more we do, the more we can do." ©
 */
public class Server extends Thread{
    // Компоненты сервера
    private static Server server;
    private ServerSocket serverSocket;
    private StreamPool streamPool;
    private BufferManager bufferManager;
    // Listeners
    private DataListener dataListener; // converter
    private PoolListener poolListener; // pool
    private ErrorListener errorListener; // errors
    private ServerListener serverListener; // current server
    // Параметрыы сервера
    private int port = 8585;
    private static final int MAX_USERS = 10;
    private int bufferActive = 0;

    public Server(){
        synchronized (server){
            server = this;
        }
        streamPool = new StreamPool(this.poolListener);
        bufferManager = new BufferManager();
    }

    public Server(PoolListener poolListener, DataListener dataListener) {
        this();
        this.poolListener = poolListener;
        this.dataListener = dataListener;
    }

    public Server(PoolListener poolListener, DataListener dataListener, int port) {
        this(poolListener, dataListener);
        this.port = port;
    }


    @Override
    public void run() {
        super.run();
        try {
            serverSocket = new ServerSocket(port);
            while (!isInterrupted()) {
                Socket inputConnection = serverSocket.accept();

                // принимаем и обрабатываем входящее соединение по одному из шаблонов:
                // 1 - регистрируем стрим
                // 2 - отсылаем список стримов, закрываем соединение
                // 3 - ошибочный запрос, закрываем соединение
                HandleIncomingConnection handleIncomingConnection = new HandleIncomingConnection(this, streamPool, inputConnection);
                handleIncomingConnection.start();
            }

            // Здесь операции по завершению работы сервера
            serverSocket.close();
            streamPool.closeAllConnections();
        } catch (IOException e) {
            errorListener.onError("Error on running server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public ErrorListener getErrorListener() {
        return errorListener;
    }

    public static Server getCurrentServer() {
        synchronized (server){
            return server;
        }
    }

    // Server options
    public List<StreamData> getStreams(){
        return streamPool.getAllStreams();
    }
    public void deleteStream(String id) {
        streamPool.removeUserStream(id);
    }
    public void openStream(String id){
        if (!streamPool.heartbeatStream(id)){
            errorListener.onError("Warning in opening stream: user is not active anymore.");
            return;
        }
        UserStream user = streamPool.getUserStream(id);
        user.setBufferManager(bufferManager);
        try{
            user.sendStart();
        } catch (IOException e) {
            e.printStackTrace();
            errorListener.onError("Error in opening stream: user is not active anymore.");
        }

    }
    public void closeStream(String id){
        streamPool.removeUserStream(id);
    }
    public void heartbeatStreams(){
        streamPool.heartbeatStreams();
    }


    // Listener setters
    public void setDataListener(DataListener dataListener) {
        this.dataListener = dataListener;
    }
    public void setPoolListener(PoolListener poolListener) {
        this.poolListener = poolListener;
    }
    public void setErrorListener(ErrorListener errorListener) {
        this.errorListener = errorListener;
    }
    public void setServerListener(ServerListener serverListener) {
        this.serverListener = serverListener;
    }
}
