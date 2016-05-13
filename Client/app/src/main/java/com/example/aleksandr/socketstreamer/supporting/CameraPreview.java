package com.example.aleksandr.socketstreamer.supporting;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Представление для камеры. Показывает текущую картинку с камеры.
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, PreviewCallback {
    // Objects
    private SurfaceHolder surfaceHolder;
    private Camera camera; // инстанс камеры
    private Size previewSize; // размер изображения с камеры
    private int frameLength; // длина одного фрейма информации
    private LinkedList<byte[]> framesQueue = new LinkedList<>(); // очередь фреймов
    private byte[] lastFrame = null; // последний фрейм в очереди
    private FrameQueue frameQueue = new FrameQueue();
    private ColorMatrixColorFilter filter = null;
    private Paint paint = new Paint();
    private Filters currentFilter;
    private Context context;

    // Properties
    private static final String TAG = "CAMERA_PREVIEW";
    private static final int MAX_BUFFER = 15; // макс. кол-во фреймов поддерживаемое в очереди
    private static final int JPEG_QUALITY = 70; //experimental
    private static final int BLUR_RADIUS = 9; // радиус эффекта размытия

    // все возможные фильтры
    public enum Filters {
        DEFAULT, GREYSCALE, SEPIA, BINARY, BLUR
    }

    // Blur objects - RenderScript
    private RenderScript rs;
    private ScriptIntrinsicBlur script;


    public CameraPreview(Context context, Camera camera) {
        super(context);
        // установка инстанса камеры
        this.camera = camera;

        // установка отображения и колбэка
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);

        // deprecated setting, but required on Android versions prior to 3.0
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        // получение параметров камеры
        Parameters params = this.camera.getParameters();
        // получение поддерживаемых форматов отображения
        List<Size> sizes = params.getSupportedPreviewSizes();
        for (Size s : sizes) {
            Log.i(TAG, "preview size = " + s.width + ", " + s.height);
        }

        // установка маленького формата превью, т.к. меньше задержек будет
        params.setPreviewSize(320, 240);
        this.camera.setParameters(params);

        // получение параметров превью
        previewSize = this.camera.getParameters().getPreviewSize();
        Log.i(TAG, "preview size = " + previewSize.width + ", " + previewSize.height);

        // получение текущего формата отображения
        int format = this.camera.getParameters().getPreviewFormat();
        // по формату устанавливается размер фрейма
        frameLength = previewSize.width * previewSize.height * ImageFormat.getBitsPerPixel(format) / 8;


        // TODO: 12.05.2016 FOR Filter!, delete if not working
        setWillNotDraw(false);
    }


    // Options
    private void resetBuff() {
        // очищение "очереди" фреймов и последнего фрейма
        synchronized (framesQueue) {
            framesQueue.clear();
            lastFrame = null;
        }
    }


    // Surface methods
    public void onPause() {
        // при паузе - освободить камеру
        if (camera != null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
        }
        // и очистить буфер
        resetBuff();
    }

    public void surfaceCreated(SurfaceHolder holder) {
        try {
            // установка дисплея дли отображения
            camera.setPreviewDisplay(holder);
            // начало превью
            camera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // при если текущего отображения нет - менять нечего
        if (surfaceHolder.getSurface() == null) {
            return;
        }

        try {
            // остановка отображения
            camera.stopPreview();
            // очищение буферов
            resetBuff();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            // TODO: 01.04.2016 Проверить необходиомсть перезаписывать surfaceHolder
            // установка прежнего Callback-a
            camera.setPreviewCallback(this);
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();

        } catch (Exception e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    // кастомная очередь фреймов
    private class FrameQueue {
        // Queue
        private final LinkedList<byte[]> buffer = new LinkedList<>();
        // Instances of variables
        private ByteArrayOutputStream convertingStream = new ByteArrayOutputStream();
        private ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        private YuvImage yuvImage;
        private byte[] lastFrame;
        private Bitmap lastConvertedFrame;
        private Bitmap lastScaledFrame;
        private boolean lastFrameConverted = false;
        private Canvas canvas;

        public void storeData(byte[] data) {
            synchronized (buffer) {
                // если не усевает работать конвертация
                if (buffer.size() == MAX_BUFFER) {
                    buffer.poll();
                }
                buffer.add(data);
                lastFrameConverted = false;
            }
        }

        public synchronized byte[] getBytedLastFrame() {
            if (!lastFrameConverted) {
                convertLastFrame();
            }
            Bitmap bmOverlay = Bitmap.createBitmap(lastConvertedFrame.getWidth(), lastConvertedFrame.getHeight(), lastConvertedFrame.getConfig());
            canvas = new Canvas(bmOverlay);
            canvas.drawBitmap(lastConvertedFrame, new Matrix(), paint);
            outputStream = new ByteArrayOutputStream();
            bmOverlay.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream);
            return outputStream.toByteArray();
        }

        public synchronized Bitmap getBitmapLastFrame() {
            if (!lastFrameConverted) {
                convertLastFrame();
            }
            return lastScaledFrame;
        }

        private byte[] getLastUnconvertedFrame() {
            synchronized (buffer) {
                if (buffer.size() > 0) {
                    lastFrame = buffer.poll();
                }
            }
            return lastFrame;
        }

        private void convertLastFrame() {
            if (buffer.size() == 0) {
                return;
            }
            yuvImage = new YuvImage(getLastUnconvertedFrame(), ImageFormat.NV21, previewSize.width, previewSize.height, null);
            convertingStream.reset();
            yuvImage.compressToJpeg(new Rect(0, 0, previewSize.width, previewSize.height), JPEG_QUALITY, convertingStream);
            lastConvertedFrame = BitmapFactory.decodeByteArray(convertingStream.toByteArray(), 0, convertingStream.size());
            try {
                lastScaledFrame = Bitmap.createScaledBitmap(lastConvertedFrame, getWidth(), getHeight(), true);
                lastFrameConverted = true;
            } catch (NullPointerException ignored) {
                lastFrameConverted = false;
            }

        }
    }


    // Filers
    public void setFilter(Filters filter) {
        switch (filter) {
            case GREYSCALE: {
                paint.setColorFilter(new ColorMatrixColorFilter(FilterMatrix.getGraysacle()));
                break;
            }
            case SEPIA: {
                paint.setColorFilter(new ColorMatrixColorFilter(FilterMatrix.getSepia()));
                break;
            }
            case BINARY: {
                paint.setColorFilter(new ColorMatrixColorFilter(FilterMatrix.getBinary()));
                break;
            }
            case BLUR: {
                rs = RenderScript.create(context);
                script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
                script.setRadius(9f);
                break;
            }
            default: {
                paint = new Paint();
                break;
            }
        }
        currentFilter = filter;
    }

    private Bitmap blur(Bitmap original) {
        Bitmap bitmap = Bitmap.createBitmap(
                original.getWidth(), original.getHeight(),
                Bitmap.Config.ARGB_8888);

        rs = RenderScript.create(context);
        script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        Allocation allocIn = Allocation.createFromBitmap(rs, original);
        Allocation allocOut = Allocation.createFromBitmap(rs, bitmap);

        ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        blur.setInput(allocIn);
        blur.forEach(allocOut);

        allocOut.copyTo(bitmap);
        rs.destroy();
        return bitmap;
    }

    // Setters
    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    // Getters
    public int getPreviewLength() {
        return frameLength;
    }

    public int getPreviewWidth() {
        return previewSize.width;
    }

    public int getPreviewHeight() {
        return previewSize.height;
    }

    public int getActualWidth() {
        return getMeasuredWidth();
    }

    public int getActualHeight() {
        return getMeasuredHeight();
    }

    public byte[] getImageBuffer() {
        // синхронизированное по Queue получение фрейма из "очереди"

        return frameQueue.getBytedLastFrame();
//        synchronized (framesQueue) {
//            if (framesQueue.size() > 0) {
//                lastFrame = framesQueue.poll();
//            }
//        }
//
//        return lastFrame;
    }


    // PreviewCallback
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
//        // запись изображений с камеры в очередь
//        synchronized (framesQueue) {
//            // Если информация не успевает уходить - пропустим последние фреймы
//            if (framesQueue.size() == MAX_BUFFER) {
//                framesQueue.poll();
//            }
//            // запишем новую информацию
//            framesQueue.add(data);
//
//            // experimental
//            createBitmap();
//            invalidate();
//        }
        frameQueue.storeData(data);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {

        Bitmap bitmap = frameQueue.getBitmapLastFrame();
        if (bitmap == null)
            return;
        synchronized (paint) {
            switch (currentFilter) {
                case BLUR: {
                    canvas.drawBitmap(blur(bitmap), 0, 0, paint);
                    break;
                }
                default: {
                    canvas.drawBitmap(bitmap, 0, 0, paint);
                    break;
                }
            }
        }
    }
}

class FilterMatrix {
    static ColorMatrix getGraysacle() {
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);
        return colorMatrix;
    }

    static ColorMatrix getSepia() {
        ColorMatrix colorMatrix = getGraysacle();

        ColorMatrix colorScale = new ColorMatrix();
        colorScale.setScale(1, 1, 0.8f, 1);

        // Convert to grayscale, then apply brown color
        colorMatrix.postConcat(colorScale);
        return colorMatrix;
    }

    static ColorMatrix getBinary() {
        ColorMatrix colorMatrix = getGraysacle();

        float m = 255f;
        float t = -255 * 128f;
        ColorMatrix threshold = new ColorMatrix(new float[]{
                m, 0, 0, 1, t,
                0, m, 0, 1, t,
                0, 0, m, 1, t,
                0, 0, 0, 1, 0
        });

        // Convert to grayscale, then scale and clamp
        colorMatrix.postConcat(threshold);

        return colorMatrix;
    }
}