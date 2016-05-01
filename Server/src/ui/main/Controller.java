package ui.main;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class Controller {
    private MainServer single;

    public Controller() {
        single = MainServer.getInstance();
    }

    @FXML
    public void handleStartServerEvent(ActionEvent event) {
        if (!single.isServerOnline())
            single.startServer();
        else
            single.stopServer();
    }

    @FXML
    public void handleRefreshSettingsEvent(ActionEvent event) {
        single.checkSettings();
    }
}
