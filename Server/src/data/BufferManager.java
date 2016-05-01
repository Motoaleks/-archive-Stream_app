package data;

import data.Abstractions.PictureData;
import data.Listeners.DataListener;
import ui.Utils;

import java.awt.image.BufferedImage;
import java.util.LinkedList;

//!!! Для данного случая, используется слово фрейм - ещё не обработанный массив байтов или его часть,
//!!! равная размеру в байтах картинки полученной с камеры на телефоне


/**
 * Осуществляет работу с буферами фреймов, кадрирование, перевод из YUV в RGB. Общение с View посредством
 * <strong>DataListener</strong>. Используется поочерёдная смена буфферов.
 *
 * @author Aleksandr Smilyanskiy
 * @version 1.0
 */
public class BufferManager extends Thread {

    /**
     * Количество буферов для содержания и динамической работы с поступающими фреймами
     */
    private static final int MAX_BUFFER_COUNT = 2;
    /**
     * Буферы записи для записи фреймов
     */
    private ImageFrameBuffer[] mBufferQueue;
    /**
     * Номер текущего заполняемого буфера
     */
    private int mFillCount = 0;
    /**
     * Количество оставшейся до записи в текущий буфер информации
     */
    private int mRemained = 0;

    private PictureData pictureData;
    /**
     * "Очередь" фреймов для преобразования в картинки
     */
    private LinkedList<byte[]> mYUVQueue = new LinkedList<>();
    /**
     * Слушатель о завершении преобразования одного фрейма
     */
    private DataListener mListener;

    public BufferManager(){

    }

    /***
     * Создаёт "преобразователь" фреймов в картинки
     *
     * @param pictureData информация о картинке
     */
    public BufferManager(PictureData pictureData) {
        this();
        setPictureData(pictureData);

        mBufferQueue = new ImageFrameBuffer[MAX_BUFFER_COUNT];
        for (int i = 0; i < MAX_BUFFER_COUNT; ++i) {
            mBufferQueue[i] = new ImageFrameBuffer(pictureData.getFrameLength(), pictureData.getWidth(), pictureData.getHeight());
        }
    }

    // TODO: 30.04.2016 DELETE THIS CONSTRUCTOR
    public BufferManager(int length, int width, int height){
        this();
        pictureData = new PictureData(length,width,height);

        mBufferQueue = new ImageFrameBuffer[MAX_BUFFER_COUNT];
        for (int i = 0; i < MAX_BUFFER_COUNT; ++i) {
            mBufferQueue[i] = new ImageFrameBuffer(pictureData.getFrameLength(), pictureData.getWidth(), pictureData.getHeight());
        }
    }

    /**
     * Общий метод направляющий на раскадрирование по фреймам поток последовательно идущих фреймов
     *
     * @param data байт массив - "поток" фреймов
     * @param len  длина новой информации
     */
    public void fillBuffer(byte[] data, int len) {
        // не проведена инициализация
        if (pictureData == null || !pictureData.checkCorrect()){
            return;
        }

        // исправим инкремент
        mFillCount = mFillCount % MAX_BUFFER_COUNT;

        // Если осталось дозаписать в текущий буффер
        if (mRemained != 0) {
            // Если длина поступвшей информации больше оставшейся до записи
            if (mRemained < len) {
                // пишем сколько осталось в текущий буффер
                mBufferQueue[mFillCount].fillBuffer(data, 0, mRemained, mYUVQueue);
                // меняем буффер
                ++mFillCount;
                if (mFillCount == MAX_BUFFER_COUNT)
                    mFillCount = 0;
                // пишем оставшееся, но с оффсетом для data=mRemained и в другой буффер
                mBufferQueue[mFillCount].fillBuffer(data, mRemained, len - mRemained, mYUVQueue);
                // запишем сколько осталось дописать в буффер
                mRemained = pictureData.getFrameLength() - len + mRemained;
            } else if (mRemained == len) {
                // если длина информации для записи равна длине оставшейся информации для записи
                // заполняем оставшийся буффер
                mBufferQueue[mFillCount].fillBuffer(data, 0, mRemained, mYUVQueue);
                // обнуляем кол-во необходимой оставшейся информации
                mRemained = 0;
                // меняем буффер
                ++mFillCount;
                if (mFillCount == MAX_BUFFER_COUNT)
                    mFillCount = 0;
            } else {
                // если len<mRemained, то записываем инфо в буффер и уменьшаем кол-во оставшейся до записи информации
                mBufferQueue[mFillCount].fillBuffer(data, 0, len, mYUVQueue);
                mRemained = mRemained - len;
            }
        } else {
            // заполнения буфера НА кадрирование
            mBufferQueue[mFillCount].fillBuffer(data, 0, len, mYUVQueue);

            // если длина меньше длины одного фрейма - нехватка информации
            if (len < pictureData.getFrameLength()) {
                // тогда осталось до полного фрейма - длина фрейма - текущая длина
                mRemained = pictureData.getFrameLength() - len;
            } else {
                // иначе - текущий буффер заполнен, идём в следующий
                ++mFillCount;
                // идём на первый буфер если дошли до последнего
                if (mFillCount == MAX_BUFFER_COUNT)
                    mFillCount = 0;
            }
        }
    }

    /***
     * Установка слушателя процесса кадрирования и запуск процесса переведения из кадров в картинки
     *
     * @param listener слушатель процесса кадрирования
     */
    public void setOnDataListener(DataListener listener) {
        // Слушатель изменения информации
        mListener = listener;
        // при установке слушателя тут же начинается превращение из кадров в картинки
        start();
    }

    public void setPictureData(PictureData pictureData) {
        this.pictureData = pictureData;
    }

    /***
     * Закрытие процесса превращения кадров в картинки
     */
    public void close() {
        // сообщить о желании закрыть поток
        interrupt();
        try {
            // дождаться прогрузки кадра
            join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /***
     * Процесс переведния кадров в картинки BufferedImage(RGB)
     */
    @Override
    public void run() {
        super.run();
        // Работает пока не будет прерван специально
        while (!Thread.currentThread().isInterrupted()) {
            // буффер
            byte[] data = null;

            // синхронизируем по потоку кадров
            synchronized (mYUVQueue) {
                // Получение последней картинки из "потока" с телефона
                data = mYUVQueue.poll();

                // Если последний кадр существует
                if (data != null) {
                    // получение дампа времени
                    long t = System.currentTimeMillis();

                    BufferedImage bufferedImage = null;
                    // конвертирование потока(массива) в RGB из YUV
                    int[] rgbArray = Utils.convertYUVtoRGB(data, pictureData.getWidth(), pictureData.getHeight());
                    // создание картинки
                    bufferedImage = new BufferedImage(pictureData.getWidth(), pictureData.getHeight(), BufferedImage.TYPE_USHORT_565_RGB);
                    // портирование массива в картинку
                    bufferedImage.setRGB(0, 0, pictureData.getWidth(), pictureData.getHeight(), rgbArray, 0, pictureData.getWidth());

                    // сообщим о создании очередной картинки
                    mListener.onDirty(bufferedImage);
                    // время занятое на трансформацию очередного кадра
                    System.out.println("time cost = " + (System.currentTimeMillis() - t));
                }

            }
        }
    }
}
