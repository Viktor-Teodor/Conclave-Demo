package com.test.enclave;


import com.r3.conclave.common.EnclaveCall;
import com.r3.conclave.enclave.Enclave;
import com.r3.conclave.mail.EnclaveMail;
import com.r3.conclave.mail.MutableMail;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.OptionalDouble;


public class TestEnclave extends Enclave implements EnclaveCall {

    @Override
    public byte[] invoke(byte[] bytes) {

        double[] prices = convertByteArrayToDoubleArray(bytes);
        byte[] result = new byte[0];

        try {
            result = doubleToByteArray(Arrays.stream(prices).average().getAsDouble());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    @Override
    protected void receiveMail(long id, EnclaveMail mail) {

        byte[] average = invoke(mail.getBodyAsBytes());

        MutableMail reply = createMail(mail.getAuthenticatedSender(), average);

        postMail(reply, null);
    }

    private static double convertByteArrayToDouble(byte[] doubleBytes){
        ByteBuffer byteBuffer = ByteBuffer.allocate(Double.BYTES);
        byteBuffer.put(doubleBytes);
        byteBuffer.flip();
        return byteBuffer.getDouble();
    }

    public static double[] convertByteArrayToDoubleArray(byte[] data) {

        if (data == null || data.length % Double.BYTES != 0) return null;

        double[] doubles = new double[data.length / Double.BYTES];

        //Because a double is stored on 8 bytes, we take chunks of 8 bytes to reconstruct the doubles
        for (int i = 0; i < doubles.length; i++)
            doubles[i] = ( convertByteArrayToDouble(new byte[] {
                    data[(i*Double.BYTES)],
                    data[(i*Double.BYTES)+1],
                    data[(i*Double.BYTES)+2],
                    data[(i*Double.BYTES)+3],
                    data[(i*Double.BYTES)+4],
                    data[(i*Double.BYTES)+5],
                    data[(i*Double.BYTES)+6],
                    data[(i*Double.BYTES)+7],
            } ));
        return doubles;
    }

    private static byte[] doubleToByteArray ( final double i ) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        dos.writeDouble(i);
        dos.flush();
        return bos.toByteArray();
    }

}