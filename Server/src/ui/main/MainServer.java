package ui.main;

import io.SocketServer;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import ui.stream.StreamWindow;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;

public class MainServer extends Application {
    /**
     * Максимальное кол-во картинок в очереди для отображения.
     */
    private static final int MAX_BUFFER = 15;
    /**
     * Текущий объект, т.к. сервер может быть лишь один
     */
    private static MainServer single;
    /**
     * Дефолтный порт
     */
    private final int DEFAULT_PORT = 8585;
    /**
     * Включён ли сервер
     */
    private boolean isServerOnline = false;
    /**
     * "Очередь" картинок для отображения
     */
    private LinkedList<BufferedImage> mQueue = new LinkedList<>();
    /**
     * Дефолтная картинка и последний фрейм-картинка.
     */
    private BufferedImage mImage, mLastFrame;
    /**
     * Сокет-клиент
     */
    private SocketServer server;
    /**
     * Включена ли трансляция
     */
    private boolean isWatchingOn = false;
    /**
     * Окно проигрывания стрима
     */
    private StreamWindow streamWindowWindow;
    /**
     * Стейдж окна прогирывания стрима
     */
    private Stage streamStage;
    /**
     * Главная сцена
     */
    private Scene mainWindow;
    /**
     * Круг - индикатор доступности сервера
     */
    private Circle status;
    /**
     * Поле порта сервера
     */
    private Text port;
    /**
     * Поле адреса сервера
     */
    private Text ipAdress;
    /**
     * Логер
     */
    private TextArea log;
    /**
     * Перезапускает проверку настроек
     */
    private Button rerunSettingsCheck;
    /**
     * Запуск сервера
     */
    private Button launchServer;


    public MainServer() {

    }

    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Возвращает инстанс текущего сервера
     *
     * @return инстанс окна сервера
     */
    public static MainServer getInstance() {
        return single;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        single = this;

        Parent root = FXMLLoader.load(getClass().getResource("main.fxml"));

        mainWindow = new Scene(root, 490, 390);

        primaryStage.setTitle("StreamEra server");
        primaryStage.setScene(mainWindow);
        primaryStage.setResizable(false);


        initForms();
        checkSettings();

        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                System.exit(0);
            }
        });
        primaryStage.show();
    }

    /**
     * Инициация полей - видов
     */
    public void initForms() {
        if (mainWindow == null) {
            return;
        }

        // Статус сервера
        status = (Circle) mainWindow.lookup("#crc_serverStatus");
        status.setFill(Color.RED);// Окно показа

        // Окно адреса
        ipAdress = (Text) mainWindow.lookup("#lbl_ipAdress");
        ipAdress.setText("Waiting for ip...");

        // Log
        log = (TextArea) mainWindow.lookup("#txt_log");
        log.setEditable(false);
        log.setFocusTraversable(false);

        // Кнопка запуска перепроверки настроек
        rerunSettingsCheck = (Button) mainWindow.lookup("#btn_rerun");

        //поле порта сервера
        port = (Text) mainWindow.lookup("#lbl_port");

        //кнопка запуска сервера
        launchServer = (Button) mainWindow.lookup("#btn_launch");
    }

    /**
     * Запуск сокет-сервера, установка слушателя
     */
    public void startServer() {
        // создание сокет-сервера
        server = new SocketServer();
        // запуск сервера
        server.start();
        launchServer.setText("STOP SERVER");
        setOnline(true);

        // TODO: 03.04.2016 delete it
        startStreamTranslation();
    }

    /**
     * Остановка работы сервера
     */
    public void stopServer() {
        server.closeServer();
        stopStreamTranslation();
        launchServer.setText("START SERVER");
        setOnline(false);
    }

    /**
     * Проверяет ip адрес и порт
     */
    public void checkSettings() {
        // Получение и установка public ip
        Thread getPublicIp = new Thread() {
            @Override
            public void run() {
                super.run();
                URL whatismyip = null;
                try {
                    port.setText(String.valueOf(DEFAULT_PORT));

                    whatismyip = new URL("http://checkip.amazonaws.com");
                    BufferedReader in = new BufferedReader(new InputStreamReader(whatismyip.openStream()));
                    String ip = in.readLine(); //you get the IP as a String
                    ipAdress.setText(ip);
                    writeLog("Current ip received: " + ip);
                } catch (Exception e) {
                    writeLog("Problems with getting ip adress from <http://checkip.amazonaws.com>, check internet connection.");
                    ipAdress.setText("Could not get server ip.");
                }
            }
        };
        getPublicIp.start();
    }

    /**
     * Начинает просмотр трансляции
     */
    public void startStreamTranslation() {
        if (isWatchingOn) {
            return;
        }
        streamWindowWindow = new StreamWindow(server.getWidth(), server.getHeight());
        streamStage = new Stage();
        try {
            streamWindowWindow.start(streamStage);
        } catch (Exception e) {
            e.printStackTrace();
        }
        server.setOnDataListener(streamWindowWindow);
        writeLog("Translation opened.");
        setIsOn(true);
    }

    /**
     * Останавливает просмотр трансляции
     */
    public void stopStreamTranslation() {
        if (!isWatchingOn) {
            return;
        }
        if (streamStage == null)
            return;

        streamStage.close();
        writeLog("Translation closed.");
        setIsOn(false);
    }

    /**
     * Сеттер для трансляции
     *
     * @param isOn состояние трансляции
     */
    public void setIsOn(boolean isOn) {
        this.isWatchingOn = isOn;
    }

    /**
     * Установка состояния сервера
     *
     * @param online состояние
     */
    public void setOnline(boolean online) {
        this.isServerOnline = online;
        if (isServerOnline) {
            status.setFill(Color.GREEN);
        } else {
            status.setFill(Color.RED);
        }
    }

    /**
     * Узнаёт включён ли сервер
     *
     * @return состояние сервера
     */
    public boolean isServerOnline() {
        return isServerOnline;
    }

    /**
     * Запись в лог информации
     *
     * @param message сообщение для записи
     */
    public void writeLog(String message) {
        // Запишь timestamp и сообщения в логер
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss: ");
        log.setText(log.getText() + sdf.format(calendar.getTime()) + message + "\n");
    }
}
