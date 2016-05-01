package data.Listeners;

import java.awt.image.BufferedImage;

/**
 * Слушатель заканчивания процесса перекодирования из YUV в RGB.
 *
 * @author Aleksandr Smilyanskiy
 * @version 1.0
 */
public interface DataListener {
    /**
     * Информирует о перекодировании очередного фрейма в картинку-RGB BufferedImage
     *
     * @param bufferedImage Получившаяся картинка
     */
    void onDirty(BufferedImage bufferedImage);
}
