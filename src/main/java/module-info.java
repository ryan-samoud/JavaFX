module com.esports {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.swing;
    requires java.sql;
    requires mysql.connector.j;
    requires javafx.web;
    requires jbcrypt;
    requires java.mail;
    requires java.desktop;
    requires webcam.capture;
    opens com.esports to javafx.fxml;
    opens com.esports.controller to javafx.fxml;
    opens com.esports.model to javafx.fxml;

    exports com.esports;
    exports com.esports.controller;
    exports com.esports.model;
    exports com.esports.service;
    exports com.esports.utils;
}
