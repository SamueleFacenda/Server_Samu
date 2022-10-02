package com.socket;

import com.crypto.KeyManager;

import java.io.IOException;
import java.net.ServerSocket;

public class MainSocket {
    private static final int PORT = 9999;
    public static void main(String[] args) {
        //main che va eseguito da remoto sul mio server
        KeyManager.initialize();//inizializza le chiavi rsa salvate nei file
        try(ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Listening on port " + PORT);
            //accetto tutte le connessioni e creo un nuovo thread connessione
            while (true) {
                new Connection(serverSocket.accept()).start();
                System.out.println("Connection accepted at time "+ new java.util.Date());
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error in MainSocket main method");
        }
    }
}
