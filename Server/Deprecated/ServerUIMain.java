package Deprecated;

import data.DataListener;
import io.SocketServer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.LinkedList;


/**
 * Главный интерфейс и поток программы.
 *
 * @author Aleksandr Smilyanskiy
 * @version 1.0
 */
public class ServerUIMain extends JPanel implements DataListener {
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
     * Сокет-клиент
     */
    SocketServer server;

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
     * Запуск сокет-сервера, установка слушателя
     */
    public ServerUIMain() {
        // создание сокет-сервера
        server = new SocketServer();
        // установка слушателя процесса конвертирования
        server.setOnDataListener(this);
        // запуск сервера
        server.start();
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

    public static void main(String[] args) {
        // создание окна
        JFrame f = new JFrame("Monitor");

        // установка операции закрытия
        f.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // TODO: 02.04.2016 Починить экстренное закрытие
                System.exit(0);
            }
        });

        // добавление панели
        f.add(new ServerUIMain());
        // компресс
        f.pack();
        // делаем видной
        f.setVisible(true);
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
}
