module com.mycompany.passwordmanager {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.mycompany.passwordmanager to javafx.fxml;
    exports com.mycompany.passwordmanager;
}
