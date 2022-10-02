package com.socket;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MainTest {
    private static String hash(String str) {
        try {
            MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] array = md.digest(str.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : array) {
                String hex = Integer.toHexString(0xFF & b);
                if (hex.length() == 1) sb.append('0');
                sb.append(hex);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    public static void main(String[] args) {
        PostgresConnector pc = new PostgresConnector(true);
        pc.addUser("test", hash("test"), false);
    }
}
