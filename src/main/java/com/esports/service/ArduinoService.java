package com.esports.service;

import com.fazecast.jSerialComm.SerialPort;
import java.nio.charset.StandardCharsets;

public class ArduinoService {

    private static ArduinoService instance;
    private SerialPort serialPort;

    private ArduinoService() {
        SerialPort[] ports = SerialPort.getCommPorts();
        if (ports.length > 0) {
            serialPort = ports[0];
            serialPort.setBaudRate(9600);
            serialPort.openPort();
            System.out.println("[ArduinoService] Port ouvert : " + serialPort.getSystemPortName());
        } else {
            System.err.println("[ArduinoService] Aucun port série trouvé.");
        }
    }

    public static synchronized ArduinoService getInstance() {
        if (instance == null) {
            instance = new ArduinoService();
        }
        return instance;
    }

    public void sendData(String data) {
        if (serialPort != null && serialPort.isOpen()) {
            String message = data + "\n";
            byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
            serialPort.writeBytes(bytes, bytes.length);
            System.out.println("[ArduinoService] Envoi : " + data);
        } else {
            System.err.println("[ArduinoService] Port non ouvert, impossible d'envoyer.");
        }
    }

    public void closeConnection() {
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
        }
    }
}
