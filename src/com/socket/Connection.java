package com.socket;

/**
 * @author Samuele Facenda
 * tcp socket server class, the socket is passed in the constructor
 */

import com.crypto.AESUtils;
import com.crypto.JsonUtils;
import com.crypto.KeyManager;
import com.crypto.RSAUtils;
import com.dataClasses.*;
import com.managers.CommentsManager;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.net.*;
import java.security.PublicKey;
import java.sql.Timestamp;
import java.util.Random;

public class Connection extends Thread {
    private final Socket socket;
    private CommunicatioUtils cu;
    private static PostgresConnector pc;
    private static Semaforo s;
    private final int id;
    private static int totConnections = 0;
    private static String SERVER = "ServerSamu--", CLIENT = "ClientSamu--", ACK = "Recived";
    private SecretKey sessionKey;
    private IvParameterSpec sessionIV;
    private String user;
    private boolean isLogged = false;


    public Connection(Socket socket) {
        super("Connection" + totConnections++ + "   port: " + socket.getPort());
        id = totConnections;
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
        if(acceptConnection()){
            SessionMetadata sm = readMessage(SessionMetadata.class);
            if(sm.isNew())
                firstAccess();
            else if(sm.rsaTimestamp().before(KeyManager.getLastTS()))
                sendLastKey();
            writeAck();
            if(!reciveEncryptedKey())
                return;
            writeAck();
            int choice;
            do{
                choice = readInt();
                switch (choice) {
                    case 0 -> register();
                    case 1 -> login();
                    case 2 -> {
                        if(!upload()) return;
                    }
                    case 3 -> System.out.println(this.getName() + "  exiting...");
                    default -> System.out.println(this.getName() + " invalid choice");
                }
            }while(choice != 3);

            if(readBye())
                sendBye();
            else
                System.out.println(this.getName() + "  error in bye");
        }
        try {
            socket.close();
        } catch (IOException e) {
            System.out.println("Error in Connection run method, closing socket");
        }
    }

    private String readFromServer() throws IOException {
        return readFrom(SERVER);
    }

    private String readFrom(String name) throws IOException {
        String line = cu.readLine();
        if(line.startsWith(name)){
            return line.substring(name.length());
        }else
            throw new IOException("Invalid input from " + name);
    }

    private String readFromClient() throws IOException {
        return readFrom(CLIENT);
    }

    private boolean readAck() {
        try{
            String in = readFromServer();
            return in.equals(ACK);
        }catch(IOException e){
            System.out.println("Error in Connection readAck method");
            return false;
        }
    }

    private int readInt() {
        try{
            String in = readFromClient();
            return Integer.parseInt(in);
        }catch(IOException e){
            System.out.println("Error in Connection readInt method");
            return -1;
        }
    }

    private void writeAck() {
        cu.writeLine(SERVER + ACK);
    }

    private void writeInt(int i) {
        cu.writeLine(CLIENT + i);
    }

    private void askConnection(){
        cu.writeLine(CLIENT + "AskingForConnection");
    }

    private boolean acceptConnection(){
        try{
            String in = readFromClient();
            if(in.equals("AskingForConnection")) {
                cu.writeLine(SERVER + "ConnectionAccepted");
                return true;
            }

            //se è qualcuno a caso che scrive non rispondo
        }catch(IOException e){
            System.err.println("Error in Connection acceptConnection method");
        }
        return false;
    }

    private <T> T readMessage(Class<T> clazz) {
        try {
            String in = readFromClient();
            return JsonUtils.fromJson(in, clazz);
        } catch (Exception e) {
            System.err.println("Error in Connection readMessage method");
            return null;
        }
    }

    private <T> T readEncryptedMessage(Class<T> clazz, String prefix, String suffix) {
        try {
            String in = reciveAndDecrypt(prefix, suffix);
            return JsonUtils.fromJson(in, clazz);
        } catch (Exception e) {
            System.err.println("Error in Connection readEncryptedMessage method");
            return null;
        }
    }

    private <T> void writeMessage(T message) {
        cu.writeLine(CLIENT + JsonUtils.toJson(message));
    }

    private String reciveAndDecrypt(String prefix, String suffix) {
        try {
            String in = readFromClient();
            if(!in.startsWith(prefix) || !in.endsWith(suffix))
                return null;
            in = in.substring(prefix.length(), in.length() - suffix.length());
            return AESUtils.decrypt(in, sessionKey, sessionIV);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error in Connection reciveAndDecrypt method\n" + e.getMessage());
            return null;
        }
    }

    private void sendEncrypted(String message, String prefix, String suffix) {
        try {
            String encrypted = AESUtils.encrypt(message, sessionKey, sessionIV);
            cu.writeLine(prefix + encrypted + suffix);
        } catch (Exception e) {
            System.err.println("Error in Connection sendEncrypted method");
        }
    }

    private void firstAccess(){
        try{
            Auth auth = readMessage(Auth.class);
            if (pc.addUser(auth.user(), auth.psw(), false)) {
                System.out.println(this.getName() + "  new user registered");
                cu.writeLine(SERVER + "OK");
            }else {
                System.out.println(this.getName() + "  error in firstAccess, user already existing");
                cu.writeLine(SERVER + "alreadyExist");
            }
        }catch(Exception e){
            cu.writeLine(SERVER + "Invalid");
        }
    }

    private void sendLastKey(){
        cu.writeLine(SERVER + "RSAKEY:" + KeyManager.getPublicKey() + "--ENDKEY");
    }

    private void reciveLastKey(){
        try{
            String in = readFromServer();
            if(in.startsWith("RSAKEY:") && in.endsWith("--ENDKEY")){
                in = in.substring(7, in.length() - 8);
                PublicKey pk = RSAUtils.fromBase64Public(in);
            }
        }catch(IOException e){
            System.err.println("Error in Connection reciveLastKey method");
        }catch(Exception e){
            System.err.println("Error in Connection reciveLastKey method, invalid key");
        }
    }

    private boolean reciveEncryptedKey(){
        try{
            String in = readFromServer();
            in = RSAUtils.decrypt(RSAUtils.fromBase64(in), KeyManager.getPrivateKey());
            AesKey aesKey = JsonUtils.fromJson(in, AesKey.class);
            sessionKey = AESUtils.fromBase64(aesKey.key());
            sessionIV = new IvParameterSpec(AESUtils.stringToByteArray(aesKey.iv()));
            return true;
        }catch (Exception e){
            System.err.println("Error in Connection reciveEncryptedKey method");
            e.printStackTrace();
            return false;
        }
    }

    private void sendEncryptedKey(){
        try{
            sessionIV = AESUtils.generateIv();
            sessionKey = AESUtils.generateKey(1024);
            String key = AESUtils.toBase64(sessionKey);
            String iv = AESUtils.byteArrayToString(sessionIV.getIV());
            String json = JsonUtils.toJson(new AesKey(key, iv));
            json = RSAUtils.toBase64(RSAUtils.encrypt(json, KeyManager.getPublicKey()));
            cu.writeLine(SERVER + json);
        }catch(Exception e){
            System.err.println("Error in Connection sendEncryptedKey method");
            e.printStackTrace();
        }
    }

    private void register(){
        Auth auth = readEncryptedMessage(Auth.class, CLIENT , "");
        try{
            if (pc.addUser(auth.user(), auth.psw(), false)) {
                System.out.println(this.getName() + "  new user registered");
                cu.writeLine(SERVER + "OK");
            } else {
                cu.writeLine(SERVER + "alreadyExist");
            }
        }catch (Exception e){
            cu.writeLine(SERVER + "Invalid");
            System.err.println("Error in Connection register method   " + e.getMessage());
        }
    }

    private void login(){
        Auth auth = readEncryptedMessage(Auth.class, CLIENT, "");
        try{
            if(pc.checkUser(auth.user(), auth.psw(), false)){
                //generate a random string lenght 100
                String token =  new Random().ints(48, 122 + 1)
                        .limit(100)
                        .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                        .toString();
                token = AESUtils.encrypt(token, sessionKey, sessionIV);
                pc.addToken(auth.user(), token, null);
                cu.writeLine(SERVER + "OK--" + token);
                System.out.println(this.getName() + "  user " + auth.user() + " logged in");
                isLogged = true;
                user = auth.user();
            }else if(pc.checkToken(auth.user(), auth.psw())){
                cu.writeLine(SERVER + "OK");
                System.out.println(this.getName() + "  user " + auth.user() + " logged in");
                isLogged = true;
                user = auth.user();
            }else{
                cu.writeLine(SERVER + "NO");
                System.out.println(this.getName() + "  user " + auth.user() + " failed to login");
                isLogged = false;
            }
        }catch(Exception e){
            System.out.println(this.getName() + "  user " + auth.user() + " failed to login, internal server error");
            System.err.println(e.getMessage());
            isLogged = false;

        }
    }

    private boolean upload(){
        Activity activity = readEncryptedMessage(Activity.class, CLIENT, "").setUser(user);
        try{
            if(!isLogged)
                return false;
            if(activity.ts() == null)
                activity = activity.setTs(new Timestamp(System.currentTimeMillis()));
            if(!activity.comment().equals(""))
                activity = activity.setComment(
                        CommentsManager.addComment(user, activity.comment(), activity.ts())
                );
            pc.addData(activity);
            return true;
        }catch(Exception e){
            System.err.println("Error in Connection upload method");
            e.printStackTrace();
            return false;
        }
    }

    private boolean readBye(){
        try{
            String in = readFromClient();
            return in.equals("Bye");
        }catch(IOException e){
            System.out.println("Error in Connection readBye method");
            return false;
        }
    }

    private void sendBye(){
        cu.writeLine(CLIENT + "Bye");
    }

    @Override
    public String toString() {
        return "Connection{" +
                "socket=" + socket +
                ", cu=" + cu +
                ", id=" + id +
                ", sessionKey=" + sessionKey +
                ", sessionIV=" + sessionIV +
                ", user='" + user + '\'' +
                ", isLogged=" + isLogged +
                '}';
    }
}
