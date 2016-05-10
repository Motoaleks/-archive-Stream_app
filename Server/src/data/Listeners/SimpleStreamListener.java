package data.Listeners;

import data.Abstractions.StreamData;

/**
 * Created by Aleksand Smilyanskiy on 07.05.2016.
 * "The more we do, the more we can do." ©
 */
public interface SimpleStreamListener {
    void onStreamShutdown(StreamData streamData);
}
