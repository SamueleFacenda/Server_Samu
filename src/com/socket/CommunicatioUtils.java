package com.socket;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.Arrays;

public class CommunicatioUtils {
    private BufferedReader in;
    private PrintWriter out;

    public CommunicatioUtils(BufferedReader in, PrintWriter out) {
        this.in = in;
        this.out = out;
    }

    public String readLine() {
        StringBuilder line = new StringBuilder();
        try {
            line.append(in.readLine());
        } catch (Exception e) {
            System.out.println("Error in CommunicatioUtils readLine method");
        }
        return line.toString();
    }

    private boolean checkIfEnd(StringBuilder line, String end) {
        if(line.length() < end.length()) return false;
        char[] endChars = new char[end.length()];
        line.getChars(line.length() - end.length(), line.length(), endChars, 0);
        return Arrays.equals(endChars, end.toCharArray());
    }

    public String readUntil(String end) {
        StringBuilder line = new StringBuilder();
        try {
            while (!checkIfEnd(line, end)) {
                line.append((char) in.read());
            }
        } catch (Exception e) {
            System.out.println("Error in CommunicatioUtils readUntil method");
        }
        return line.toString();
    }

    public void writeLine(String line) {
        out.println(line);
    }

    public void write(String line) {
        out.print(line);
    }

    public String writeAfter(String line, String after) {
        String out = readUntil(after);
        writeLine(line);
        return out;
    }
}
