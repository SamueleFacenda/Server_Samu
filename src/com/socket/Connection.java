package com.socket;

/**
 * @author Samuele Facenda
 * tcp socket server class, the socket is passed in the constructor
 */

import java.io.*;
import java.net.*;

public class Connection extends Thread {
    private Socket socket;
    private CommunicatioUtils cu;
    private static PostgresConnector pc;
    private static Semaforo s;
    private static int id;


    public Connection(Socket socket) {
        super("Connection" + id++ + "   port: " + socket.getPort());
        System.out.println("Connection created:  " + this.getName());
        if(pc == null) {
            s = new Semaforo(1);
            pc = new PostgresConnector(isLocal());
        }
        this.socket = socket;
        try {
            cu = new CommunicatioUtils(
                    new BufferedReader(new InputStreamReader(socket.getInputStream())),
                    new PrintWriter(socket.getOutputStream(), true)
            );

        } catch (IOException e) {
            System.out.println("Error in Connection constructor");
        }
    }

    private boolean isLocal(){
        return !System.getProperty("os.name").startsWith("Windows");
    }

    @Override
    public void run(){
        try {
            socket.close();
        } catch (IOException e) {
            System.out.println("Error in Connection run method");
        }
    }
}
