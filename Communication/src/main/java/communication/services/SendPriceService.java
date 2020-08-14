package communication.services;

import com.r3.conclave.common.EnclaveInstanceInfo;
import com.r3.conclave.mail.Curve25519KeyPairGenerator;
import com.r3.conclave.mail.EnclaveMail;
import com.r3.conclave.mail.MutableMail;
import communication.CommunicationApplication;
import org.springframework.stereotype.Service;
import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.util.UUID;

@Service
public class SendPriceService {

    private static double price;
    private static DataInputStream fromHost;
    private static DataOutputStream toHost;
    private static EnclaveInstanceInfo attestation;
    private static KeyPair myKey;

    static{

        int port = 9000;
        String host = "127.0.0.1";

        // Generate our own Curve25519 keypair so we can receive a response.
        myKey = new Curve25519KeyPairGenerator().generateKeyPair();

        while (true) {
            try {

                System.out.println("Attempting to connect to localhost:" + port);
                SocketFactory socketFactory = SocketFactory.getDefault();
                Socket connection = socketFactory.createSocket(host, port);
                fromHost = new DataInputStream(connection.getInputStream());
                toHost = new DataOutputStream(connection.getOutputStream());
                break;

            } catch (Exception e) {
                System.err.println("Retrying: " + e.getMessage());
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException interruptedException) {
                    interruptedException.printStackTrace();
                }
            }
        }
    }

    public void setPrice(double priceWrapper){
        this.price = price;
    }

    //The first thing we need to do is to read the certificate
    public void receiveEnclaveCertificate() throws IOException, InterruptedException {

        byte[] attestationBytes = new byte[fromHost.readInt()];
        fromHost.readFully(attestationBytes);
        attestation = EnclaveInstanceInfo.deserialize(attestationBytes);
    }

    //Then we need to send the price to the enclave
    public boolean sendPriceToEnclave() throws IOException {

        // Now we checked the enclave's identity and are satisfied it's the enclave from this project,
        // we can send mail to it. We will provide our own private key whilst encrypting, so the enclave
        // gets our public key and can encrypt a reply.
        byte[] bytesOfPrice = new byte[]{(byte) price};

        MutableMail mail = attestation.createMail(bytesOfPrice);
        mail.setPrivateKey(myKey.getPrivate());

        // Set a random topic, so we can re-run this program against the same server.
        mail.setTopic(UUID.randomUUID().toString());
        byte[] encryptedMail = mail.encrypt();

        System.out.println("Sending the encrypted mail to the host.");

        toHost.writeInt(encryptedMail.length);
        toHost.write(encryptedMail);

        return true;
    }

    public double receiveAverageFromEnclave() throws IOException {

        int myPort = CommunicationApplication.port;

        ServerSocketFactory acceptorFactory = ServerSocketFactory.getDefault();
        ServerSocket acceptor = acceptorFactory.createServerSocket(myPort);
        Socket connection = acceptor.accept();

        //Read the average from the enclave
        DataInputStream input = new DataInputStream(connection.getInputStream());
        byte[] mailBytes = new byte[input.readInt()];

        System.out.println("Reading reply mail of length " + mailBytes.length + " bytes.");

        input.readFully(mailBytes);

        EnclaveMail reply = attestation.decryptMail(mailBytes, myKey.getPrivate());

        return ByteBuffer.wrap(reply.getBodyAsBytes()).getDouble();

    }


}
