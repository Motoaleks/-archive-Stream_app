package Deprecated;

import data.DataListener;
import io.SocketServer;
import javafx.stage.Stage;
import ui.main.Main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.LinkedList;

/**
 * Created by Aleksand Smilyanskiy on 03.04.2016.
 * "The more we do, the more we can do." ©
 */
public class StreamWindow extends JPanel implements DataListener {
    /**
     * "Очередь" картинок для отображения
     */
    private LinkedList<BufferedImage> mQueue = new LinkedList<>();
    /**
     * Максимальное кол-во картинок в очереди для отображения.
     */
    private static final int MAX_BUFFER = 15;
    /**
     * Дефолтная картинка и последний фрейм-картинка.
     */
    BufferedImage mImage, mLastFrame;
    /**
     * Окно показа
     */
    JFrame stream;

    public StreamWindow(Main parent, SocketServer socketServer) {
        socketServer.setOnDataListener(this);
        // создание окна
        stream = new JFrame("Monitor");
        stream.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                parent.stopStreamTranslation();
//                System.exit(0);
            }
        });
        stream.add(this);
        // компресс
        stream.pack();
        // делаем видной
        stream.setVisible(true);
        parent.setIsOn(true);
    }

    @Override
    public void paint(Graphics g) {
        // отображаем картинки по очереди из "очереди"
        synchronized (mQueue) {
            if (mQueue.size() > 0) {
                mLastFrame = mQueue.poll();
            }
        }
        // прорисовываем картинку
        if (mLastFrame != null) {
            g.drawImage(mLastFrame, 0, 0, null);
        } else if (mImage != null) {
            // или заглушку
            g.drawImage(mImage, 0, 0, null);
        }
    }

    /**
     * Обновление картинки
     *
     * @param bufferedImage новое изображение
     */
    private void updateUI(BufferedImage bufferedImage) {
        // когда картинки не успевают отображаться - некоторые пропускаем
        synchronized (mQueue) {
            if (mQueue.size() == MAX_BUFFER) {
                // убираем некоторые не успевающие прорисоваться картинки
                mLastFrame = mQueue.poll();
            }
            // добавляем в очередь картинку
            mQueue.add(bufferedImage);
        }

        repaint();
    }

    /**
     * Возвращает рекомендуемый для окна размер.
     *
     * @return наилучшее разрешение окна
     */
    @Override
    public Dimension getPreferredSize() {
        // установка дефолтного размера
        if (mImage == null) {
            return new Dimension(960, 720); // init window size
        } else {
            return new Dimension(mImage.getWidth(null), mImage.getHeight(null));
        }
    }

    /**
     * Метод интерфейса, помогающий получить картинку из "недр" процесса конвертирования.
     *
     * @param bufferedImage Получившаяся картинка
     */
    @Override
    public void onDirty(BufferedImage bufferedImage) {
        // по готовности - обновляем изображение
        updateUI(bufferedImage);
    }

    public void closeWindow() {
        // TODO: 05.04.2016 close window JAVAFX
    }
}
