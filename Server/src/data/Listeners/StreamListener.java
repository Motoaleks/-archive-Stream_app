package data.Listeners;

import io.UserStream;

/**
 * Created by Aleksand Smilyanskiy on 06.04.2016.
 * "The more we do, the more we can do." ©
 */

/**
 * Слушатель собатий стримов
 */
public interface StreamListener {
    void onStreamDisconnected(UserStream userStream);

    void onGeopositionChanged(int latitude, int longitude);

    void onStreamResized(int width, int height);

    void onTranslationStatusChanged(boolean status);
}
