package com.socat;

public class MainSocat {
    public static void main(String[] args) {
        System.out.println("Hello World!");
        Communicator c = new Communicator();
        c.talk();
        System.out.println("bye");
    }
}
