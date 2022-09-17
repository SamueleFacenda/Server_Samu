package com.socat;

import java.util.Scanner;

public class Communicator {
    private String name;
    private Scanner in;
    public Communicator(){
        in = new Scanner(System.in);
    }
    public void talk(){
        System.out.println("nome: ");
        name = in.nextLine();
        System.out.println("Hello " + name);
    }
}
