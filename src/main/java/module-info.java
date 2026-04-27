module com.esports {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.base;
    requires javafx.web;
    requires javafx.media;
    requires javafx.swing;
    requires java.sql;
    requires java.base;
    requires java.desktop;
    requires mysql.connector.j;
    requires java.net.http;
    requires org.json;
    requires jdk.jsobject;

    // iText7 PDF (automatic modules)
    requires kernel;
    requires layout;
    requires io;

    // ZXing (QR Codes)
    requires com.google.zxing;
    requires com.google.zxing.javase;
    requires com.sothawo.mapjfx;
    requires org.slf4j;

    opens com.esports to javafx.fxml;
    opens com.esports.controller to javafx.fxml, javafx.web;
    opens com.esports.model to javafx.fxml;
    opens com.esports.service to javafx.fxml;

    exports com.esports;
    exports com.esports.controller;
    exports com.esports.model;
    exports com.esports.service;
    exports com.esports.interfaces;
    exports com.esports.utils;
}
