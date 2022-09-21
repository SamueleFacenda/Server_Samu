package com.crypto;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class RSAUtils {
    private static final int KEY_SIZE = 1024;
    private static final String ALGORITHM = "RSA";
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    private static final String ENCRYPTION_ALGORITHM = "RSA/ECB/PKCS1Padding";

    public static KeyPair RSAKeyPairGenerator() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM);
        keyGen.initialize(KEY_SIZE);
        return keyGen.generateKeyPair();
    }

    public static void writeToFile(String path, byte[] key) throws IOException {
        File f = new File(path);
        f.getParentFile().mkdirs();

        FileOutputStream fos = new FileOutputStream(f);
        fos.write(key);
        fos.flush();
        fos.close();
    }

    public static String toBase64(PrivateKey in){
        return Base64.getEncoder().encodeToString(in.getEncoded());
    }
    public static String toBase64(PublicKey in){
        return Base64.getEncoder().encodeToString(in.getEncoded());
    }

    public static PrivateKey fromBase64Private(String in) throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeyFactory kf = KeyFactory.getInstance(ALGORITHM);
        return kf.generatePrivate(new X509EncodedKeySpec(Base64.getDecoder().decode(in.getBytes())));
    }

    public static PublicKey fromBase64Public(String in) throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeyFactory kf = KeyFactory.getInstance(ALGORITHM);
        return kf.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(in.getBytes())));
    }

    public static byte[] encrypt(String data, String publicKey) throws BadPaddingException, IllegalBlockSizeException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeySpecException {
        Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, fromBase64Public(publicKey));
        return cipher.doFinal(data.getBytes());
    }

    public static String toBase64(byte[] in){
        return Base64.getEncoder().encodeToString(in);
    }

    public static byte[] fromBase64(String in){
        return Base64.getDecoder().decode(in.getBytes());
    }

    public static String decrypt(byte[] data, String privateKey) throws BadPaddingException, IllegalBlockSizeException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeySpecException {
        Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, fromBase64Private(privateKey));
        return new String(cipher.doFinal(data));
    }

}
