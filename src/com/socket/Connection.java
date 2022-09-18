package com.socket;

/**
 * @author Samuele Facenda
 * tcp socket server class, the socket is passed in the constructor
 */

import java.io.*;
import java.net.*;

public class Connection extends Thread {
    private final Socket socket;
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
            System.err.println(e.getMessage());
            System.out.println("Error in Connection constructor");
        }
    }

    private boolean isLocal(){
        return !System.getProperty("os.name").startsWith("Windows");
    }

    @Override
    public void run(){
        cu.writeLine("Welcome to Samu's server");
        cu.writeLine("Please login or register(1/2)");
        String username = null;
        String password = null;
        String in = cu.readLine();
        switch(in){
            case "1" -> {
                cu.writeLine("Please insert your username");
                username = cu.readLine();
                cu.writeLine("Please insert your password");
                password = cu.readLine();
                s.P();
                if(pc.checkUser(username, password)){
                    cu.writeLine("Login successful");
                } else {
                    cu.writeLine("Login failed");
                }
                s.V();
            }
            case "2" -> {
                cu.writeLine("Please insert your username");
                username = cu.readLine();
                cu.writeLine("Please insert your password");
                password = cu.readLine();
                cu.writeLine("Please insert your password again");
                String password2 = cu.readLine();
                s.P();
                if(!password.equals(password2)){
                    cu.writeLine("Passwords don't match");
                }else if(!pc.addUser(username, password)){
                    cu.writeLine("Username already exists");
                }
                s.V();
            }
            default -> {
                cu.writeLine("Invalid input");
            }
        }
        try {
            socket.close();
        } catch (IOException e) {
            System.out.println("Error in Connection run method, closing socket");
        }
    }
}
