package tcpclient;
import java.net.*;
import java.io.*;

public class TCPClient
{
    public boolean shutdown;
    public Integer timeout;
    public Integer limit;
    private final byte[] fromServerBuffer;
    private final ByteArrayOutputStream inputBuffer;
    private int bufferSize;

    public TCPClient(boolean shutdown, Integer timeout, Integer limit)
    {
        this.shutdown = shutdown;
        this.timeout = timeout;
        this.limit = limit;
        this.bufferSize = 1024;
        if (limit != null)
        {
            bufferSize = limit;
        }
        fromServerBuffer = new byte[bufferSize];
        inputBuffer = new ByteArrayOutputStream();
    }

    public byte[] askServer(String hostname, int port, byte[] toServerBytes) throws IOException
    {
        Socket socket = new Socket(hostname, port);
        try
        {
            if (timeout != null)
            {
                socket.setSoTimeout(timeout);
            }

            socket.getOutputStream().write(toServerBytes);

            if (shutdown)
            {
                socket.shutdownOutput();
            }

            int fromServerLength;
            while ((fromServerLength = socket.getInputStream().read(fromServerBuffer, 0, bufferSize)) != -1)
            {
                inputBuffer.write(fromServerBuffer, 0, fromServerLength);
                if (limit != null && inputBuffer.size() >= limit)
                {
                    break;
                }
            }
        } catch (SocketTimeoutException ex)
        {
            socket.close();

        }

        if (limit != null && inputBuffer.size() >= limit)
        {
            socket.close();
        }
        return inputBuffer.toByteArray();
    }
}
