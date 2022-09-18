package com.socket;

import java.sql.*;
import java.sql.Connection;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;

public class PostgresConnector {
    private final String url;
    private final String user = "samu";
    private  Connection conn = null;
    private final String addUser_String, getUser_String, addData_String;
    private PreparedStatement addUser, getUser, addData;


    public PostgresConnector(boolean isLocal) {

        addUser_String = "INSERT INTO users (username, password) VALUES (?, ?)";
        getUser_String = "SELECT * FROM users WHERE username = ? AND password = ?";
        addData_String = "INSERT INTO data (user_id, label, file, created_at) VALUES (?, ?, ?, ?)";

        url = "jdbc:postgresql://"+(isLocal?"localhost":"samuele.ddns.net") +":5432/samudb";
        String password = System.getenv("SAMU_PASSWORD");

        try (Connection con = DriverManager.getConnection(url, user, password);
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT VERSION()")) {

            if (rs.next()) {
                System.out.println(rs.getString(1));
            }

            addUser = con.prepareStatement(addUser_String);
            getUser = con.prepareStatement(getUser_String);
            addData = con.prepareStatement(addData_String);
            conn = con;
        } catch (SQLException ex) {
            System.err.println(ex);
        }
    }

    public static void main(String[] args) {
        new PostgresConnector(false);
    }

    public boolean connect() {
        try {
            conn = DriverManager.getConnection(url, user, System.getenv("SAMU_PASSWORD"));
            addUser = conn.prepareStatement(addUser_String);
            getUser = conn.prepareStatement(getUser_String);
            addData = conn.prepareStatement(addData_String);
            return true;
        } catch (SQLException ex) {
            Logger.getLogger(PostgresConnector.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println(ex);
            return false;
        }
    }

    /**
     * method checkUser, check if the user is in the database and the password is correct
     * @param username the username to check
     * @param password the password to check
     * @return true if the user is in the database and the password is correct, false otherwise
     */
    public boolean checkUser(String username, String password){
        return getUser(username, password) != -1;
    }

    private int getUser(String username, String password) {
        password = hash(password);
        try {
            getUser.setString(1, username);
            getUser.setString(2, password);
            ResultSet rs = getUser.executeQuery();
            return rs.next() ? rs.getInt("id") : -1;
        } catch (SQLException ex) {
            System.err.println(ex);
        }
        return -1;
    }


    /**
     * private method hash sha256, return the hex string of the hash
     * @param str the string to hash
     * @return the hex string of the hash
     */
    private String hash(String str) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] array = md.digest(str.getBytes());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < array.length; ++i) {
                String hex = Integer.toHexString((array[i] & 0xFF) | 0x100);
                if(hex.length() == 1) sb.append('0');
                sb.append(hex);
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            System.err.println(e);
        }
        return null;
    }

    /**
     * method addUser, add a new user to the database
     * @param username the username to add
     * @param password the password to add
     * the password is hashed with the SHA-256 algorithm
     */
    public boolean addUser(String username, String password) {
        //hash the password
        password = hash(password);
        try {
            addUser.setString(1, username);
            addUser.setString(2, password);
            addUser.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException ex) {
            return false;
        } catch (SQLException ex) {
            System.err.println(ex);
            return false;
        }
        return true;
    }

    /**
     * method addData, add a new data to the database
     * @param username the username of the user that add the data
     * @param label the label of the data
     * @param file the file of the data
     */
    public void addData(String username, String label, String file) {
        int user_id = getUser(username, "");
        try {
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            addData.setInt(1, user_id);
            addData.setString(2, label);
            addData.setString(3, file);
            addData.setString(4, timestamp.toString());
            addData.executeUpdate();
        } catch (SQLException ex) {
            System.err.println(ex);
        }
    }
}