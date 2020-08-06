package com.test.host;

import com.r3.conclave.common.EnclaveInstanceInfo;
import com.r3.conclave.common.EnclaveMode;
import com.r3.conclave.common.OpaqueBytes;
import com.r3.conclave.host.EnclaveHost;
import com.r3.conclave.host.EnclaveLoadException;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class Host {
    public static void main(String[] args) throws EnclaveLoadException {

        try {
            EnclaveHost.checkPlatformSupportsEnclaves(true);
            System.out.println("This platform supports enclaves in simulation, debug and release mode.");
        } catch (EnclaveLoadException e) {
            System.out.println("This platform currently only supports enclaves in simulation mode: " + e.getMessage());
        }

        String className = "com.test.enclave.TestEnclave";
        try (EnclaveHost enclave = EnclaveHost.load(className)) {

            OpaqueBytes spid = enclave.getEnclaveMode() != EnclaveMode.SIMULATION ? OpaqueBytes.parse(args[0]) : null;
            String attestationKey = enclave.getEnclaveMode() != EnclaveMode.SIMULATION ? args[1] : null;

            // Start it up.
            AtomicReference<byte[]> requestToDeliver = new AtomicReference<>();

            enclave.start(spid, attestationKey, new EnclaveHost.MailCallbacks() {
                @Override
                public void postMail(byte[] encryptedBytes, String routingHint) {
                    requestToDeliver.set(encryptedBytes);
                }
            });

            System.out.println(callEnclave(enclave, "Hello world!"));
            // !dlrow olleH      :-)


            final EnclaveInstanceInfo attestation = enclave.getEnclaveInstanceInfo();
            final byte[] attestationBytes = attestation.serialize();
            System.out.println(EnclaveInstanceInfo.deserialize(attestationBytes));


            //In order to get the price from multiple parties, I will just use a for loop and open the connection for each
            int numberOfConnections = 3;


            // That's not very useful by itself. Enclaves only get interesting when remote clients can talk to them.
            // So now let's open a TCP socket and implement a trivial protocol that lets a remote client use it.
            int port = 9999;
            System.out.println("Listening on port " + port + ". Use the client app to send strings for reversal.");

            List<Double> prices = new ArrayList<>();

            for (int index = 1; index <= numberOfConnections; ++index) {
                try {

                    ServerSocketFactory acceptorFactory = SSLServerSocketFactory.getDefault();
                    ServerSocket acceptor = acceptorFactory.createServerSocket(port);
                    Socket connection = acceptor.accept();

                    // Just send the attestation straight to whoever connects. It's signed so that's MITM-safe.
                    DataOutputStream output = new DataOutputStream(connection.getOutputStream());
                    output.writeInt(attestationBytes.length);
                    output.write(attestationBytes);
                    output.flush();

                    // Now read some mail from the client.
                    DataInputStream input = new DataInputStream(connection.getInputStream());
                    byte[] mailBytes = new byte[input.readInt()];
                    input.readFully(mailBytes);

                    prices.add(ByteBuffer.wrap(mailBytes).getDouble());


                    // Closing the output stream closes the connection. Different clients will block each other but this
                    // is just a hello world sample.
                    output.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Deliver it. The enclave will give us some mail to reply with via the callback we passed in
            // to the start() method.

            byte[] bytes = new byte[prices.size() * Double.BYTES];
            for (int i = 0; i < prices.size(); i++)
                System.arraycopy(doubleToByteArray(prices.get(i)), 0, bytes, i * Double.BYTES, Double.BYTES);

            enclave.deliverMail(1, bytes);

            sendAverageBackToUsers(requestToDeliver.get());


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String callEnclave(EnclaveHost enclave, String input) {

        // We'll convert strings to bytes and back.
        final byte[] inputBytes = input.getBytes();

        final byte[] outputBytes = Objects.requireNonNull(enclave.callEnclave(inputBytes));
        return new String(outputBytes);
    }

    private static byte[] doubleToByteArray ( final double i ) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        dos.writeDouble(i);
        dos.flush();
        return bos.toByteArray();
    }

    private static void sendAverageBackToUsers(byte[] toSend) throws IOException {

        int port = 9999;
        ServerSocketFactory acceptorFactory = SSLServerSocketFactory.getDefault();
        ServerSocket acceptor = acceptorFactory.createServerSocket(port);
        Socket connection = acceptor.accept();

        DataOutputStream output = new DataOutputStream(connection.getOutputStream());
        output.writeInt(toSend.length);
        output.write(toSend);

    }
}