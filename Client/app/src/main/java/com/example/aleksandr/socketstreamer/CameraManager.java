package com.example.aleksandr.socketstreamer;

import android.content.Context;
import android.hardware.Camera;
import android.widget.Toast;

/**
 * Менеджер камеры для управления с hardware.Camera.
 */
public class CameraManager {
    /**
     * Инстанс камеры
     */
    private Camera mCamera;
    /**
     * Контекс в котором используется камера
     */
    private Context mContext;

    /**
     * Создание менеджера камеры
     *
     * @param context использованный контекст программы
     */
    public CameraManager(Context context) {
        mContext = context;
        mCamera = getCameraInstance();
    }

    /**
     * Getter для инстанса камеры
     *
     * @return текущий инстанс камеры
     */
    public Camera getCamera() {
        return mCamera;
    }

    /**
     * Освобождение камеры
     */
    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    /**
     * При остановке - освобождение камеры
     */
    public void onPause() {
        releaseCamera();
    }

    /**
     * При продолжении - восстановление инстанса камеры
     */
    public void onResume() {
        if (mCamera == null) {
            mCamera = getCameraInstance();
        }

        Toast.makeText(mContext, "preview size = " + mCamera.getParameters().getPreviewSize().width +
                ", " + mCamera.getParameters().getPreviewSize().height, Toast.LENGTH_LONG).show();
    }

    /**
     * Безопасное получение инстанса камеры
     *
     * @return инстанс камеры
     */
    private static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open();
        } catch (Exception e) {
            // Камера недоступна или не может быть включена сейчас
            e.printStackTrace();
        }
        return c;
    }

}
