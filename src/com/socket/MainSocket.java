package com.socket;

import java.io.IOException;
import java.net.ServerSocket;

public class MainSocket {
    public static void main(String[] args) {
        try(ServerSocket serverSocket = new ServerSocket(9999)) {
            while (true) {
                new Connection(serverSocket.accept()).start();
                System.out.println("Connection accepted");
            }
        } catch (IOException e) {
            System.err.println(e.getLocalizedMessage());
            System.out.println("Error in MainSocket main method");
        }
    }
}
