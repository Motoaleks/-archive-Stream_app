package com.example.aleksandr.socketstreamer.supporting;

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
    private Camera camera;
    /**
     * Контекс в котором используется камера
     */
    private Context context;

    /**
     * Создание менеджера камеры
     *
     * @param context использованный контекст программы
     */
    public CameraManager(Context context) {
        this.context = context;
        camera = getCameraInstance();
    }

    /**
     * Getter для инстанса камеры
     *
     * @return текущий инстанс камеры
     */
    public Camera getCamera() {
        return camera;
    }

    /**
     * Освобождение камеры
     */
    public void releaseCamera() {
        if (camera != null) {
            camera.release();
            camera = null;
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
        if (camera == null) {
            camera = getCameraInstance();
        }

        Toast.makeText(context, "preview size = " + camera.getParameters().getPreviewSize().width +
                ", " + camera.getParameters().getPreviewSize().height, Toast.LENGTH_LONG).show();
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
