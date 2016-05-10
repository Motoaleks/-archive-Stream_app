package io;

import com.google.gson.JsonObject;
import data.Abstractions.StreamData;
import data.Listeners.ErrorListener;
import data.Listeners.PoolListener;
import data.Listeners.SimpleStreamListener;
import data.Listeners.StreamListener;

import java.io.IOException;
import java.util.*;

/**
 * Created by Aleksand Smilyanskiy on 05.04.2016.
 * "The more we do, the more we can do." ©
 */
public class StreamPool implements SimpleStreamListener{
    private final int MAX_USERS = 10;
    private HashMap<String, UserStream> pool;
    private PoolListener poolListener;
    private StreamListener streamListener;
    private ErrorListener errorListener;

    // Constructors
    public StreamPool() {
        pool = new HashMap<>();
    }

    public StreamPool(PoolListener poolListener) {
        this();
        setPoolListener(poolListener);
    }


    // Pool options
<<<<<<< HEAD
    public boolean heartbeatStream(String id) {
        UserStream userStream = pool.get(id);
        try {
            if (userStream != null) {
                if (userStream.heartbeatStream()) {
                    return true;
                }
                userStream.interrupt();
                pool.remove(userStream.getStreamData().getId());
                if (poolListener != null)
                    poolListener.onStreamDisconnect(userStream);
            }
        } catch (IOException e) {
            userStream.interrupt();
            pool.remove(userStream.getStreamData().getId());
            e.printStackTrace();
        }
        return false;
=======
    public boolean heartbeatStream(String id){
        JsonObject heartbeat = new JsonObject();
        heartbeat.addProperty("heartbeat", 200);
        return true;
>>>>>>> origin/master
    }

    public void heartbeatStreams() {
        for (UserStream userStream : pool.values()) {
            try {
                userStream.heartbeatStream();
            } catch (IOException e) {
                userStream.interrupt();
                pool.remove(userStream.getStreamData().getId());
            }
        }
    }

    public List<StreamData> getAllStreams() {
        // !!! при большом колв-е юзеров уменьшить капасити изначальную !!!
        List<StreamData> streamDatas = new ArrayList<>(MAX_USERS);
        for (UserStream userStream : pool.values()) {
            streamDatas.add(userStream.getStreamData());
        }
        return streamDatas;
    }

    public void closeAllConnections() {
        for (Map.Entry<String, UserStream> entry : pool.entrySet()) {
            UserStream stream = entry.getValue();
            // дисконектит каждого юзера
            stream.interrupt();
        }
    }

    // Pool users options
    public int addUserStream(UserStream userStream) {
        // проверка на разрешение добавления юзера
        if (pool.size() + 1 > MAX_USERS) {
            return -1;
        }
        // получение id и запись пользователя
        String uuid = UUID.randomUUID().toString();
        userStream.getStreamData().setId(uuid);
        userStream.start();
        userStream.setErrorListener(errorListener);
        userStream.setSimpleStreamListener(this);
        pool.put(uuid, userStream);
        // сообщение слушателю о действии
        if (poolListener != null)
            poolListener.onStreamAdded(userStream);
        return 0;
    }

    public int removeUserStream(String id) {
        // проверка на существование стрима
        if (pool.get(id) == null) {
            return -1;
        }
        UserStream deletedUserStream = pool.get(id);
        // удаление стрима
        pool.remove(id);
        deletedUserStream.closeStream();
//        boolean is = deletedUserStream.isInterrupted();
        // сообщение слушателю о действии
        if (poolListener != null)
            poolListener.onStreamDisconnect(deletedUserStream);
        return 0;
    }

    public UserStream getUserStream(String id) {
        return pool.get(id);
    }

    // Pool Listener
    public PoolListener getPoolListener() {
        return poolListener;
    }

    public void setPoolListener(PoolListener poolListener) {
        this.poolListener = poolListener;
    }

    public void setStreamListener(StreamListener streamListener) {
        this.streamListener = streamListener;
    }

    public void setErrorListener(ErrorListener errorListener) {
        this.errorListener = errorListener;
        for (UserStream userStream: pool.values()){
            userStream.setErrorListener(errorListener);
        }
    }

    @Override
    public void onStreamShutdown(StreamData streamData) {
        removeUserStream(streamData.getId());
    }
}
