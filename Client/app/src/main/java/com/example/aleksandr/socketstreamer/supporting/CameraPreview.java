package com.example.aleksandr.socketstreamer.supporting;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Представление для камеры. Показывает текущую картинку с камеры.
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    /**
     * Тэг для лога
     */
    private static final String TAG = "camera";
    /**
     * Максимальное количество фреймов, поддерживаемое в "очереди"
     */
    private static final int MAX_BUFFER = 15;
    /**
     * Среда для показа изображения
     */
    private SurfaceHolder mHolder;
    /**
     * Инстанс камеры
     */
    private Camera mCamera;

//    private byte[] mImageData;
    /**
     * Текущий размер изображения с камеры
     */
    private Size mPreviewSize;
    /**
     * "Очередь" фреймов
     */
    private LinkedList<byte[]> mQueue = new LinkedList<byte[]>();
    /**
     * Последний фрейм из "очереди"
     */
    private byte[] mLastFrame = null;
    /**
     * Длина одного фрейма информации
     */
    private int mFrameLength;
    /**
     * Callback для записи изображений с камеры в "очередь" фреймов
     */
    private PreviewCallback mPreviewCallback = new PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            // синхронизированная по mQueue запись информации в буффер
            synchronized (mQueue) {
                // Если информация не успевает уходить - пропустим последние фреймы
                if (mQueue.size() == MAX_BUFFER) {
                    mQueue.poll();
                }
                // запишем новую информацию
                mQueue.add(data);
            }
        }
    };

    /**
     * Создание инстанса представления (отображения) камеры
     *
     * @param context
     * @param camera
     */
    public CameraPreview(Context context, Camera camera) {
        super(context);

        // установка инстанса камеры
        mCamera = camera;

        // установка отображения и колбэка
        mHolder = getHolder();
        mHolder.addCallback(this);

        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        // получение параметров камеры
        Parameters params = mCamera.getParameters();
        // получение поддерживаемых форматов отображения
        List<Size> sizes = params.getSupportedPreviewSizes();
        for (Size s : sizes) {
            Log.i(TAG, "preview size = " + s.width + ", " + s.height);
        }

        // установка маленького формата превью, т.к. меньше задержек будет
        params.setPreviewSize(320, 240);
        mCamera.setParameters(params);

        // получение параметров превью
        mPreviewSize = mCamera.getParameters().getPreviewSize();
        Log.i(TAG, "preview size = " + mPreviewSize.width + ", " + mPreviewSize.height);

        // получение текущего формата отображения
        int format = mCamera.getParameters().getPreviewFormat();
        // по формату устанавливается размер фрейма
        mFrameLength = mPreviewSize.width * mPreviewSize.height * ImageFormat.getBitsPerPixel(format) / 8;
    }

    /**
     * Создание отображения
     *
     * @param holder новое отображение
     */
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            // установка дисплея дли отображения
            mCamera.setPreviewDisplay(holder);
            // начало превью
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    /**
     * Закрытие отображения
     *
     * @param holder закрываемое отображение
     */
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    /**
     * Выполнение действий, необходимых по смене места отображения
     *
     * @param holder новое место отображения
     * @param format формат отображения
     * @param w      ширина отображения
     * @param h      высота отображения
     */
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // при если текущего отображения нет - менять нечего
        if (mHolder.getSurface() == null) {
            return;
        }

        try {
            // остановка отображения
            mCamera.stopPreview();
            // очищение буферов
            resetBuff();
        } catch (Exception e) {
            //// TODO: 01.04.2016 Переделать все эксепшионы
            e.printStackTrace();
        }

        try {
            // // TODO: 01.04.2016 Проверить необходиомсть перезаписывать mHolder
            // установка прежнего Callback-a
            mCamera.setPreviewCallback(mPreviewCallback);
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();

        } catch (Exception e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    /**
     * Установка камеры для отображения
     *
     * @param camera инстанс камеры
     */
    public void setCamera(Camera camera) {
        mCamera = camera;
    }

    /**
     * Получение фрейма информации
     *
     * @return фрейм, считаемый за последний удобный для передачи
     */
    public byte[] getImageBuffer() {
        // синхронизированное по Queue получение фрейма из "очереди"
        synchronized (mQueue) {
            if (mQueue.size() > 0) {
                mLastFrame = mQueue.poll();
            }
        }

        return mLastFrame;
    }

    /**
     * Очищение "очереди" фреймов и последнего фрейма
     */
    private void resetBuff() {
        // очищение "очереди" фреймов и последнего фрейма
        synchronized (mQueue) {
            mQueue.clear();
            mLastFrame = null;
        }
    }

    /**
     * Узнать длину одного фрейма
     *
     * @return длина одного фрейма изображения
     */
    public int getPreviewLength() {
        return mFrameLength;
    }

    /**
     * Узнать ширину изображения
     *
     * @return высота изображения
     */
    public int getPreviewWidth() {
        return mPreviewSize.width;
    }

    /**
     * Узнать высоту изображения
     *
     * @return высота изображения
     */
    public int getPreviewHeight() {
        return mPreviewSize.height;
    }

    /**
     * При авузе - освобождение камеры и освобождение буфера
     */
    public void onPause() {
        // при паузе - освободить камеру
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
        }
        // и очистить буфер
        resetBuff();
    }
}
