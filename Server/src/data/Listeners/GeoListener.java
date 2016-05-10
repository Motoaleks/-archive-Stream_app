package data.Listeners;

import data.Abstractions.Coordinate;

import java.awt.image.BufferedImage;

/**
 * Created by Aleksand Smilyanskiy on 05.04.2016.
 * "The more we do, the more we can do." ©
 */

/**
 * Слушатель изменения геопозиции
 */
public interface GeoListener {
    /**
     * Информирует о изменении геопозиции стрима
     *
     * @param coordinate новая координата
     */
    void onGeoChange(Coordinate coordinate);
}
