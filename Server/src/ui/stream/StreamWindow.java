package ui.stream;

import data.Abstractions.Coordinate;
import data.Abstractions.StreamData;
import data.Listeners.DataListener;
import data.Listeners.GeoListener;
import io.UserStream;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import ui.main.Main;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.LinkedList;

/**
 * Created by Aleksand Smilyanskiy on 05.04.2016.
 * "The more we do, the more we can do." ©
 */
public class StreamWindow extends Application implements DataListener, GeoListener {
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
    private Stage primaryStage;
    private int mWidth, mHeight;
    /**
     * "Очередь" картинок для отображения
     */
    private LinkedList<BufferedImage> mQueue = new LinkedList<>();

    // objects
    private Main parent;
    private StreamData streamData;
    private UserStream userStream;

    // UI
    private Text geopositionStatus;
    private Text convTime;
    private Text latitude;
    private Text longitude;
    private VBox geoposition;
    private ImageView stream;
    private Coordinate lastCoordinate;


    public StreamWindow(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    public static void main(String[] args) {
        launch(args);
    }

    // start-stop
    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("stream.fxml"));
//        streamScene = new Scene(root, mWidth - 10, mHeight - 10);
        streamScene = new Scene(root, 520, 390);
        this.primaryStage = primaryStage;
        initforms();

        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                try {
                    stop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        geoposition.setOpacity(0);

        primaryStage.setScene(streamScene);
        primaryStage.setTitle("Video translation from android client");
        primaryStage.setResizable(false);
        primaryStage.show();
    }


    @Override
    public void stop() throws Exception {
        if (userStream == null){
            return;
        }

        try {
            userStream.requestWait();
        } catch (IOException e) {
            e.printStackTrace();
        }

        userStream.setGeoListener(null);
        userStream.setDataListener(null);

        super.stop();
    }


    // func
    private void initforms() {
        if (streamScene == null)
            return;
        geopositionStatus = (Text) streamScene.lookup("#txt_geoStatus");
        convTime = (Text) streamScene.lookup("#txt_convTime");
        latitude = (Text) streamScene.lookup("#txt_latitude");
        longitude = (Text) streamScene.lookup("#txt_longitude");
        geoposition = (VBox) streamScene.lookup("#vbox_geo");
        stream = (ImageView) streamScene.lookup("#view_stream");
    }

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


    // Setters
    private void setNewGeo(Coordinate coordinate) {
        this.lastCoordinate = coordinate;
        if (latitude == null){
            return;
        }
        latitude.setText(String.valueOf(coordinate.getLatitude()));
        longitude.setText(String.valueOf(coordinate.getLongitude()));
        if (geoposition.getOpacity() != 1) {
            geoposition.setOpacity(1);
        }
    }

    public void setParent(Main parent) {
        this.parent = parent;
    }

    public void setUserStream(UserStream userStream) {
        userStream.setDataListener(this);
        userStream.setGeoListener(this);
        this.userStream = userStream;
    }

    // Listeners
    @Override
    public void onDirty(BufferedImage bufferedImage) {
        // по готовности - обновляем изображение
        updateUI(bufferedImage);
    }

    @Override
    public void onGeoChange(Coordinate coordinate) {
        setNewGeo(coordinate);
    }
}
