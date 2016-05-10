package ui.main;

import io.Server;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class Controller {
    private Main single;

    public Controller() {
        single = Main.getInstance();
    }

    @FXML
    public void handleStartServerEvent(ActionEvent event) {
        switch (single.getServerStatus()){
            case -1:
                single.startServer();
                break;
            case 1:
                single.stopServer();
                break;
        }
    }

    @FXML
    public void handleRefreshSettingsEvent(ActionEvent event) {
        single.checkSettings();
    }
}
