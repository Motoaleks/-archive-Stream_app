package data;

import java.io.ByteArrayOutputStream;
import java.util.LinkedList;

/**
 * Буффер для одного фрейма. Ждёт пока не придёт достаточно информации и начинает запись в "очередь" на
 * перекодирование в RGB.
 *
 * @author Aleksandr Smilyanskiy
 * @version 1.0
 */
public class ImageFrameBuffer {

    /**
     * Длина одного фрейма
     */
    private final int mFrameLength;
    /**
     * Количество текущей заполненной информации
     */
    private int mTotalLength = 0;
    /**
     * Поток для записи-вывода последовательно идущих фреймов
     */
    private ByteArrayOutputStream mByteArrayOutputStream;

    /***
     * Создание буфера для фрейма изображения
     *
     * @param frameLength длина одного фрейма
     * @param width       ширина картинки
     * @param height      высота картинки
     */
    public ImageFrameBuffer(int frameLength, int width, int height) {
        // Создаём поток для вывода байтов фрейма
        mByteArrayOutputStream = new ByteArrayOutputStream();
        // запоминание длины одного фрейма
        mFrameLength = frameLength;
    }

    /***
     * Запись из потока полследовательно идущих фреймов в "очередь" фреймов-кадров (по сути некоторого рода кадрирование)
     *
     * @param data     буфер
     * @param off      смещение в буфере
     * @param len      длина новой информации
     * @param YUVQueue "очередь" кадров
     * @return код выподнения (стандартный)
     */
    public int fillBuffer(byte[] data, int off, int len, LinkedList<byte[]> YUVQueue) {
        // добавление длины нового массива информации
        mTotalLength += len;
        // запись информации в буффер ожидания записи
        mByteArrayOutputStream.write(data, off, len);

        // если фрейм записан в буфер полностью
        if (mTotalLength == mFrameLength) {

            // запись в "очередь" фреймов
            synchronized (YUVQueue) {
                // запись в "очередь" фреймов
                YUVQueue.add(mByteArrayOutputStream.toByteArray());
                // обнуление буффера
                mByteArrayOutputStream.reset();
            }

            // обнуление длины буфера
            mTotalLength = 0;
            // выводим информацию о получении и записи в очередь файла
            System.out.println("received file");
        }

        return 0;
    }
}
