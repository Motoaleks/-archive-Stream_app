package ui.stream;

import data.Listeners.DataListener;
import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.awt.image.BufferedImage;
import java.util.LinkedList;

/**
 * Created by Aleksand Smilyanskiy on 05.04.2016.
 * "The more we do, the more we can do." ©
 */
public class StreamWindow extends Application implements DataListener {
    /**
     * Максимальное кол-во картинок в очереди для отображения.
     */
    private static final int MAX_BUFFER = 15;
    /**
     * Дефолтная картинка и последний фрейм-картинка.
     */
    BufferedImage mImage, mLastFrame;
    /**
     * Последний фрейм в виде fx
     */
    WritableImage currentFrame;
    private Scene streamScene;
    private int mWidth, mHeight;
    /**
     * "Очередь" картинок для отображения
     */
    private LinkedList<BufferedImage> mQueue = new LinkedList<>();
    private Text geoposition;
    private Text convTime;
    private ImageView stream;


    public StreamWindow(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("stream.fxml"));
        streamScene = new Scene(root, mWidth - 10, mHeight - 10);

        initforms();

        primaryStage.setScene(streamScene);
        primaryStage.setTitle("Video translation from android client");
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    /**
     * Инициализация форм панели
     */
    private void initforms() {
        if (streamScene == null)
            return;
        geoposition = (Text) streamScene.lookup("#txt_geoposition");
        convTime = (Text) streamScene.lookup("#txt_convTime");
        stream = (ImageView) streamScene.lookup("#view_stream");
    }

    /**
     * Перерисовка картинки
     */
    private void repaint() {
        // отображаем картинки по очереди из "очереди"
        synchronized (mQueue) {
            if (mQueue.size() > 0) {
                mLastFrame = mQueue.poll();
            }
        }


        // прорисовываем картинку
        if (mLastFrame != null) {
            currentFrame = SwingFXUtils.toFXImage(mLastFrame, null);
            stream.setImage(currentFrame);
        } else if (mImage != null) {
            // или заглушку
            currentFrame = SwingFXUtils.toFXImage(mImage, null);
            stream.setImage(currentFrame);
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

    public Scene getScene() {
        return streamScene;
    }
}
