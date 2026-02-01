module com.mnemos {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.xerial.sqlitejdbc;
    requires org.slf4j;
    requires java.sql;
    requires java.desktop;
    requires java.prefs;

    opens com.mnemos to javafx.fxml;
    opens com.mnemos.ui to javafx.fxml;

    exports com.mnemos;
}
