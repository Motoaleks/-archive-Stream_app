package data.Listeners;

import io.UserStream;

/**
 * Created by Aleksand Smilyanskiy on 06.04.2016.
 * "The more we do, the more we can do." ©
 */

/**
 * Слушатель событий пула стримов
 */
public interface PoolListener {
    void onStreamAdded(UserStream userStream);
    void onStreamDisconnect(UserStream userStream);
}
