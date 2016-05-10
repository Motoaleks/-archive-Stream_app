package data.Listeners;

/**
 * Created by Aleksand Smilyanskiy on 06.04.2016.
 * "The more we do, the more we can do." ©
 */

/**
 * Слушатель собатий стримов
 */
public interface StreamListener {
    void onGeopositionChanged(int latitude, int longitude);

    void onStreamResized(int width, int height);

    void onTranslationStatusChanged(int status); // -1 = offline, 0 = paused, 1 = online

    void onHeartbeatReceived();
}
