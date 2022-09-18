package com.socket;

import java.io.IOException;
import java.net.ServerSocket;

public class MainSocket {
    public static void main(String[] args) {
        // listen on port 9999 and create a new thread for each connection
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(9999);
            while (true) {
                new Connection(serverSocket.accept()).start();
                System.out.println("Connection accepted");
            }
        } catch (IOException e) {
            System.err.println(e);
            System.out.println("Error in MainSocket main method");
        }
    }
}
