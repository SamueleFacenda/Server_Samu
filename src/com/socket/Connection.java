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


/**
 * classe principale della connessione, estende thread per avere più connessioni parellele
 */
public class Connection extends Thread {
    private final Socket socket;//socket della comunicazione
    private CommunicatioUtils cu;//utilità di lettura e scrittura da stream
    private static PostgresConnector pc;//connessione col database
    private static Semaforo s;//semaforo per la connessione al database
    private final int id;
    private static int totConnections = 0;
    private static String SERVER = "ServerSamu--", CLIENT = "ClientSamu--", ACK = "Recived";//prefissi e codici usati nella comunicazione
    private static final int EXPIRATION_DAYS = 30 * 6;//6 mesi, scandenza di un token

    //dati per avere una crittazione aes
    private SecretKey sessionKey;
    private IvParameterSpec sessionIV;

    //informazioni sulla sessione
    private String user;
    private boolean isLogged = false;


    public Connection(Socket socket) {
        super("Connection" + totConnections++ + "   port: " + socket.getPort());
        id = totConnections++;
        System.out.println("Connection created:  " + this.getName());
        //inizializza il collegamento col database se è la prima connessione
        if(pc == null) {
            s = new Semaforo(1);
            pc = new PostgresConnector(isLocal());
        }

        //inizializza la comunicazione
        this.socket = socket;
        try {
            cu = new CommunicatioUtils(
                    new BufferedReader(new InputStreamReader(socket.getInputStream())),
                    new PrintWriter(socket.getOutputStream(), true)
            );

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error in Connection constructor");
        }
    }

    /**
     * controlla se sta venendo eseguito in locale(sul mio laptop) o sul server
     * @return posizione di esecuzione
     */
    private boolean isLocal(){
        return !System.getProperty("os.name").startsWith("Windows");
    }

    @Override
    public void run(){
        //scambio di connessione, sicuro perchè se il client non conosce il protocollo non riceve niente
        if(acceptConnection()){
            //ricevo i dati del client, se è il suo primo accesso e il timestamp della sua ultima chiave rsa ricevuta
            SessionMetadata sm = readMessage(SessionMetadata.class);
            System.out.println(sm);
            //se è necessario comunico al client e procedo con l'invio della chiave publbica rsa
            if(sm.isNew() || sm.rsaTimestamp().before(KeyManager.getLastTS())) {
                writeFromServer("true");
                System.out.println("Sending new key");
                sendLastKey();
            }else
                writeFromServer("false");
            writeAck();
            //ricevo i dati per la comunicazione aes crittato con rsa
            if(!reciveEncryptedKey())
                return;
            writeAck();
            int choice;
            //performo le azioni richieste dal client
            do{
                choice = readInt();
                writeAck();
                switch (choice) {
                    case 0 -> register();
                    case 1 -> login();
                    case 2 -> {
                        //se l'upload fallisce chiudo la connessione
                        if(!upload()) return;
                    }
                    case 3 -> System.out.println(this.getName() + "  exiting...");//exit
                    default -> System.out.println(this.getName() + " invalid choice");
                }
            }while(choice != 3);

            //quit procedure
            if(readBye())
                sendBye();
            else
                System.out.println(this.getName() + "  error in bye");
            System.out.println(this.getName() + "  exiting...");
        }
        try {
            socket.close();
        } catch (IOException e) {
            System.out.println("Error in Connection run method, closing socket");
        }
        System.out.println(this.getName() + "  closed");
    }

    /**
     * write a string to the client according to the protocol
     * @param s string to send
     */
    private void writeFromServer(String s){
        cu.writeLine(SERVER + s);
    }

    /**
     * recive a string from a user
     * @param name user that sent the message
     * @return string readed
     * @throws IOException
     */
    private String readFrom(String name) throws IOException {
        String line = cu.readLine();
        if(line.startsWith(name)){
            return line.substring(name.length());
        }else
            throw new IOException("Invalid input from " + name + " : " + line);
    }

    /**
     * legge una string dal client
     * @return string letta senza il prefisso CLIENT--
     * @throws IOException
     */
    private String readFromClient() throws IOException {
        return readFrom(CLIENT);
    }

    /**
     * leggo un valore in dal client
     * @return integer letto
     */
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


    /**
     * connection procedure,
     * leggo il messaggio di richiesta e rispondo con l'accettazione della stessa
     * @return se la pricedura è andata a buon fine
     */
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

    /**
     * ritorna un oggetto del package dataClasses
     * @param clazz classe dell'oggetto
     * @return oggetto letto
     * @param <T> tipo dell'oggetto
     */
    private <T> T readMessage(Class<T> clazz) {
        try {
            String in = readFromClient();
            return JsonUtils.fromJson(in, clazz);
        } catch (Exception e) {
            System.err.println("Error in Connection readMessage method");
            return null;
        }
    }

    /**
     * idem a readMessage, ma la stringa dell'oggetto è criptata con aes
     * @param clazz
     * @param prefix prefisso non criptato del messaggio
     * @param suffix suffisso non criptato del messaggio
     * @return
     * @param <T>
     */
    private <T> T readEncryptedMessage(Class<T> clazz, String prefix, String suffix) {
        try {;
            String in = reciveAndDecrypt(prefix, suffix);
            return JsonUtils.fromJson(in, clazz);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * riceve una stringa criptata con aes e la ritorna decriptata
     * @param prefix prefisso non criptato
     * @param suffix suffisso non criptato
     * @return
     */
    private String reciveAndDecrypt(String prefix, String suffix) {
        try {
            String in = cu.readLine();
            //controllo i prefissi e i suffissi
            if(!in.startsWith(prefix) || !in.endsWith(suffix))
                return null;
            //taglio la string e la decritto
            in = in.substring(prefix.length(), in.length() - suffix.length());
            return AESUtils.decrypt(in, sessionKey, sessionIV);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error in Connection reciveAndDecrypt method\n" + e.getMessage());
            return null;
        }
    }

    /**
     * invio l'ultima chiave rsa con la giusta procedura
     */
    private void sendLastKey(){
        cu.writeLine(SERVER + "RSAKEY:" + KeyManager.getPublicKey() + "--ENDKEY");
    }

    /**
     * ricevo la chiave criptata con rsa
     * @return se la procedura è andata a buon fine
     */
    private boolean reciveEncryptedKey(){
        try{
            String in = readFromClient();
            //converto il messaggio da base64 e lo decritto con la chiave privata
            in = RSAUtils.decrypt(RSAUtils.fromBase64(in), KeyManager.getPrivateKey());
            //creo l'oggetto dal json così ottenuto
            AesKey aesKey = JsonUtils.fromJson(in, AesKey.class);
            //converto le stringe in oggetti con le mie utilità
            sessionKey = AESUtils.fromBase64(aesKey.key());
            sessionIV = new IvParameterSpec(AESUtils.stringToByteArray(aesKey.iv()));
            return true;
        }catch (Exception e){
            System.err.println("Error in Connection reciveEncryptedKey method");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * registro un nuovo utente
     */
    private void register(){
        //ricevo un oggetto Auth, che ha nome e password
        Auth auth = readEncryptedMessage(Auth.class, CLIENT , "");
        try{
            //provo ad aggiungerlo al database, scrivo un messaggio di risposta
            if (pc.addUser(auth.user(), auth.psw(), false)) {
                System.out.println(this.getName() + "  new user registered");
                cu.writeLine(SERVER + "OK");
            } else
                cu.writeLine(SERVER + "alreadyExist");
        }catch (Exception e){
            cu.writeLine(SERVER + "Invalid");
            System.err.println("Error in Connection register method   " + e.getMessage());
        }
    }

    /**
     * metodo grosso per il login di un utente, sia via password che via token(prova prima con la password)
     */
    private void login(){
        //ricevo username e password/token
        Auth auth = readEncryptedMessage(Auth.class, CLIENT, "");
        try{
            //controllo prima con nome utente e password
            if(pc.checkUser(auth.user(), auth.psw(), false)){
                //se è andato a buon fine, creo un token di accesso e lo invio(tipo cookie), cosí da non salvare la password sul dispositivo
                //generate a random string length 100
                String token =  new Random().ints(48, 122 + 1)
                        .limit(100)
                        .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                        .toString();
                //genero un timestamp di EXPIRATION_DAYS più avanti di adesso
                Timestamp expiration = new Timestamp(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * EXPIRATION_DAYS);
                //aggiungo il token per l'utente nel database
                pc.addToken(auth.user(), token, expiration);
                token = AESUtils.encrypt(token, sessionKey, sessionIV);
                //invio il token criptato con aes
                cu.writeLine(SERVER + "OK--" + token);
                System.out.println(this.getName() + "  user " + auth.user() + " logged in");
                isLogged = true;
                user = auth.user();
            //se non va bene, controllo con il token
            }else if(pc.checkToken(auth.user(), auth.psw())){
                cu.writeLine(SERVER + "OK");
                System.out.println(this.getName() + "  user " + auth.user() + " logged in");
                isLogged = true;
                user = auth.user();
            }else{
                //tutti e due i metodi sono falliti
                cu.writeLine(SERVER + "NO");
                System.out.println(this.getName() + "  user " + auth.user() + " failed to login");
                isLogged = false;
            }
        }catch(Exception e){
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.out.println(auth);
            System.out.println(this.getName() + "  user " + auth.user() + " failed to login, internal server error");
            isLogged = false;
        }
    }

    /**
     * aggiungo una label al database
     * @return se l'opearazione è andata a buon fine
     */
    private boolean upload(){
        //leggo l'oggetto con le informazioni
        Activity activity = readEncryptedMessage(Activity.class, CLIENT, "").setUser(user);
        try{
            //posso farlo solo se sono loggato
            if(!isLogged)
                return false;
            //se il timestamp è null, lo setto ad adesso
            if(activity.ts() == null)
                activity = activity.setTs(new Timestamp(System.currentTimeMillis()));
            //se c'è un commento lo salvo in un file e salvo il path nel database nella colonna commento
            if(!activity.comment().equals(""))
                activity = activity.setComment(
                        CommentsManager.addComment(user, activity.comment(), activity.ts())
                );
            //aggiungo l'attività al database
            pc.addData(activity);
            return true;
        }catch(Exception e){
            System.err.println("Error in Connection upload method");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * leggo il bye del client
     * @return se l'operazione è andata a buon fine
     */
    private boolean readBye(){
        try{
            String in = readFromClient();
            return in.equals("Bye");
        }catch(IOException e){
            System.out.println("Error in Connection readBye method");
            return false;
        }
    }

    /**
     * invio un bye al client
     */
    private void sendBye(){
        cu.writeLine(SERVER + "Bye");
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
