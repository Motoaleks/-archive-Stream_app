package io;

import data.Abstractions.StreamData;
import data.Listeners.PoolListener;

import java.net.Socket;
import java.util.*;

/**
 * Created by Aleksand Smilyanskiy on 05.04.2016.
 * "The more we do, the more we can do." ©
 */
public class StreamPool {
    private final int MAX_USERS = 10;
    private HashMap<String, UserStream> pool;
    private PoolListener poolListener;

    // Constructors
    public StreamPool() {
    }
    public StreamPool(PoolListener poolListener) {
        this();
        setPoolListener(poolListener);
    }


    // Pool options
    public boolean heartbeatStream(String id){
        // TODO: 01.05.2016 this method
        return true;
    }
    public void heartbeatStreams(){

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
            stream.disconnect();
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
}
