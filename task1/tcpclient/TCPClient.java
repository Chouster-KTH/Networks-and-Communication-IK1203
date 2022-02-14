package tcpclient;

import java.net.*;
import java.io.*;

public class TCPClient
{

    private final byte[] fromServerBuffer;
    private final ByteArrayOutputStream inputBuffer;

    public TCPClient()
    {
        fromServerBuffer = new byte[1024];
        inputBuffer = new ByteArrayOutputStream();
    }

    public byte[] askServer(String hostname, int port, byte[] bytesToServer) throws IOException
    {

        try (Socket socket = new Socket(hostname, port))
        {
            socket.getOutputStream().write(bytesToServer);

            int fromServerLength;
            while ((fromServerLength = socket.getInputStream().read(fromServerBuffer)) != -1)
            {
                inputBuffer.write(fromServerBuffer, 0, fromServerLength);

            }
        }

        return inputBuffer.toByteArray();
    }

}
