package ui.main;

import data.Listeners.ErrorListener;
import data.Listeners.PoolListener;
import data.Listeners.ServerListener;
import io.Server;
import io.UserStream;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import ui.stream.StreamWindow;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Main extends Application implements PoolListener, ServerListener, ErrorListener {
    /**
     * Текущий объект, т.к. сервер может быть лишь один
     */
    private static Main single;

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
    private Circle statusCircle;
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

    private ListView<data.Abstractions.StreamData> list;

    // Do not delete this
    private Server server;
    private int serverStatus = -1;
    private ObservableList<data.Abstractions.StreamData> data = FXCollections.observableArrayList();


    public Main() {

    }

    public static void main(String[] args) {
        launch(args);
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

        while (!primaryStage.isShowing()) {
            try {
                primaryStage.show();
            } catch (Exception e) {
                Thread.sleep(1000);
            }
        }

    }

    /**
     * Инициация полей - видов
     */
    public void initForms() {
        if (mainWindow == null) {
            return;
        }

        // Статус сервера
        statusCircle = (Circle) mainWindow.lookup("#crc_serverStatus");

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
        port.setText("Waiting for port...");

        //кнопка запуска сервера
        launchServer = (Button) mainWindow.lookup("#btn_launch");

        list = (ListView<data.Abstractions.StreamData>) mainWindow.lookup("#lv_liveNow");

        list.setItems(data);
        list.setCellFactory(param -> new ListCellX());


//        list.setItems(data);
//        list.setCellFactory(list1 -> new StreamData());


        setStatusCircle(-1);
    }


    private class ListCellX extends ListCell<data.Abstractions.StreamData> {
        private Button deleteButton = new Button("Delete");
        private Button startButton = new Button("Start");
        private Label name = new Label();
        private HBox hBox = new HBox();

        public ListCellX() {
            super();
            hBox.getChildren().addAll(startButton, deleteButton, name);
        }

        @Override
        protected void updateItem(data.Abstractions.StreamData item, boolean empty) {
            super.updateItem(item, empty);
            setText(null);
            if (item == null || empty) {
                setGraphic(null);
                return;
            }
            deleteButton.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    server.closeStream(item.getId());
                    data.remove(item);
                    list.getItems().remove(item);
                }
            });
            deleteButton.setStyle("-fx-font: 12 arial; -fx-base: #ff4040;");
            startButton.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    writeLog("Stream " + item.getId() + " starting...");

                    new Thread() {
                        @Override
                        public void run() {
                            UserStream userStream = server.openStream(item.getId());
                            if (userStream == null) {
                                onError("Can't find correct stream.");
                            }

                            // здесь код не предназначенный для изменения экрана
                            StreamWindow streamWindow = new StreamWindow(item.getPictureData().getWidth(), item.getPictureData().getHeight());
                            Platform.runLater(new Runnable() {
                                public void run() {
                                    try {
                                        streamWindow.start(new Stage());
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                            streamWindow.setUserStream(userStream);
                        }
                    }.start();
                }
            });
            startButton.setStyle("-fx-font: 12 arial; -fx-base: #67c4a7;");
            if (item.getName() != null && !item.getName().equals("null"))
                name.setText(item.getName() + ":\t" + item.getId());
            else
                name.setText(item.getId());
            setGraphic(hBox);
        }
    }


    // Server options
    public void startServer() {
        server = new Server(this);
        server.setPoolListener(this); // изменение пула
        server.setErrorListener(this); // ошибки
        server.setServerListener(this); // состояние сервера
        server.start();
    }

    public void stopServer() {
        if (getServerStatus() == 0 || getServerStatus() == -1) {
            return;
        }
        server.closeServer();
        data.removeAll();
        list.refresh();
    }


    // UI options
    public void checkSettings() {
        // Получение и установка public ip
        Thread getPublicIp = new Thread() {
            @Override
            public void run() {
                super.run();
                URL whatismyip = null;
                try {
                    port.setText(String.valueOf(8585));

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

    public void writeLog(String message) {
        // Запишь timestamp и сообщения в логер
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss: ");
        log.setText(log.getText() + sdf.format(calendar.getTime()) + message + "\n");
    }


    // Getters - setters
    public static Main getInstance() {
        return single;
    }


    // Changing ui
    private void setStatusCircle(int code) {
        // -1 = offline
        // 0 = disconecting or connecting
        // 1 = online
        serverStatus = code;
        switch (code) {
            case -1:
                statusCircle.setFill(Color.RED);
                launchServer.setText("START SERVER");
                break;
            case 0:
                statusCircle.setFill(Color.WHITE);
                break;
            case 1:
                statusCircle.setFill(Color.GREEN);
                launchServer.setText("STOP SERVER");
                break;
            default:
                statusCircle.setFill(Color.DARKGREY);
        }
    }

    public int getServerStatus() {
        return serverStatus;
    }

    // Listening for events in pool or server or events for errors
    @Override
    public void onStreamAdded(UserStream userStream) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                data.add(userStream.getStreamData());
                list.refresh();
            }
        });
    }

    @Override
    public void onStreamDisconnect(UserStream userStream) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                data.remove(userStream.getStreamData());
                list.refresh();
            }
        });

    }

    @Override
    public void onServerThinking() {
        setStatusCircle(0);
    }

    @Override
    public void onServerClosed() {
        setStatusCircle(-1);
    }

    @Override
    public void onServerOpen() {
        setStatusCircle(1);
    }

    @Override
    public void onError(String message) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                writeLog(message);
            }
        });
    }
}
