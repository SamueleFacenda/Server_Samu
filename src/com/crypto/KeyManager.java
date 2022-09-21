package com.crypto;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.security.KeyPair;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Scanner;

public class KeyManager {
    private static final String FOLDER = "keys";
    private static final String EXTENSION = ".samu";
    private static final String PUB = ".pub" , PRIV = ".priv";
    private static int last = 0;

    public static void initialize(){
        File f;
        do{
            f = new File(FOLDER + File.separator + last++ + EXTENSION + PUB);
        }while(f.exists());
        last--;
    }

    public static String getPrivateKeyPath(){
        return FOLDER + File.separator + last + EXTENSION + PRIV;
    }

    public static String getPublicKeyPath(){
        return FOLDER + File.separator + last + EXTENSION + PUB;
    }

    private static void writeKey(String path,String time, String key){
        //write on the file "path" the time, a line and the key
        try{
            File f = new File(path);
            f.createNewFile();
            try(FileWriter fw = new FileWriter(f)){
                fw.write(time + "\n---------------------------\n" + key);
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public static void AddKey(){
        try {
            KeyPair kp = RSAUtils.RSAKeyPairGenerator();
            String pub = RSAUtils.toBase64(kp.getPublic());
            String priv = RSAUtils.toBase64(kp.getPrivate());
            Timestamp ts = new Timestamp(System.currentTimeMillis());
            last++;
            writeKey(getPrivateKeyPath(),ts.toString(),priv);
            writeKey(getPublicKeyPath(),ts.toString(),pub);
        }catch (Exception e){
            e.printStackTrace();
            System.err.println(e.getLocalizedMessage());
            System.out.println("Error in KeyManager AddKey method");
        }
    }

    private static String getKey(Timestamp ts, String acces){
        //return the first private key generated after the timestamp ts
        File f;
        Timestamp key_ts;
        String key, tmp;
        Scanner in;
        for(int i=last; i >= 0; i++){
            f = new File(FOLDER + File.separator + i + EXTENSION + acces);
            try {
                in = new Scanner(f);
                key_ts = Timestamp.valueOf(in.nextLine());
                if(ts.after(key_ts) || ts.equals(key_ts)){
                    in.nextLine();//---------------------------
                    key = in.nextLine();
                    in.close();
                    return key;
                }else
                    in.close();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }

        }
        return null;
    }

    private static String getKey(String access){

        File f = new File(FOLDER + File.separator + last + EXTENSION + access);
        try(Scanner in = new Scanner(f)) {
            in.nextLine();//timestamp
            in.nextLine();//---------------------------
            return in.nextLine();
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    public static String getPrivateKey(Timestamp ts){
        return getKey(ts,PRIV);
    }

    public static String getPublicKey(Timestamp ts){
        return getKey(ts,PUB);
    }

    public static String getPrivateKey(){
        return getKey(PRIV);
    }

    public static String getPublicKey(){
        return getKey(PUB);
    }

    public static Timestamp getLastTS(){
        try{
            Scanner in = new Scanner(new File(getPrivateKeyPath()));
            return  Timestamp.valueOf(in.nextLine());
        }catch(FileNotFoundException e){
            System.err.println("non ci sono ancora chiavi");
            throw new RuntimeException(e);
        }
    }
}
