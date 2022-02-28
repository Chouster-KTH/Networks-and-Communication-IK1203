import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

class TCPClient
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
        System.out.println(inputBuffer);
        return inputBuffer.toByteArray();
    }
}

class MyRunnable implements Runnable
{

    private final Socket client;
    private static int number;

    public MyRunnable(Socket currentSocket)
    {
        // store parameter for later user
        this.client = currentSocket;
    }

    public void run()
    {
        number++;
        System.out.println("server number: " + number);
        serverRun();
    }

    private void serverRun()
    {
        String responseHEAD = null;
        String responseOK = "HTTP/1.1 200 OK\nConnection: close\nContent-Type: text/plain;\n\n";
        String responseBR = "HTTP/1.1 400 Bad Request\nConnection: close\nContent-Type: text/plain;\n\n";
        String responseNF = "HTTP/1.1 404 Not Found\nConnection: close\nContent-Type: text/plain;\n\n";

        try
        {
            byte[] userQuery = new byte[1024];
            int userQueryLength = client.getInputStream().read(userQuery);
            String userQueryString = new String(userQuery, StandardCharsets.UTF_8);
            String getRequest = userQueryString.split("\n")[0];
            boolean isGetRequest = getRequest.contains("GET");
            boolean isAsk = getRequest.contains("/ask?");
            if (!isGetRequest || !isAsk)
            {
                responseHEAD = responseBR;
            } else
            {

                String split = getRequest.split(" ")[1].split("\\?")[1];
                String[] parametersArray = split.split("&");
                String hostname = null;
                Integer userPort = null;
                byte[] userInputBytes = new byte[0];
                boolean shutdown = false;
                Integer limit = null;
                Integer timeout = null;
                for (int i = 0; i < parametersArray.length; i++)
                {
                    String s = parametersArray[i].split(" ")[0];

                    if (s.contains("hostname"))
                    {
                        hostname = getParameters("hostname=", s);
                        continue;
                    }

                    if (s.contains("port"))
                    {
                        userPort = Integer.parseInt(getParameters("port=", s));
                        continue;
                    }

                    if (s.contains("string"))
                    {
                        userInputBytes = getParameters("string=", s).getBytes();
                        continue;
                    }

                    if (s.contains("shutdown"))
                    {
                        shutdown = Boolean.parseBoolean(getParameters("shutdown=", s));
                        continue;
                    }

                    if (s.contains("limit"))
                    {
                        limit = Integer.parseInt(getParameters("limit=", s));
                        continue;
                    }

                    if (s.contains("timeout"))
                    {
                        timeout = Integer.parseInt(getParameters("timeout=", s));
                    }

                }
                try
                {
                    TCPClient tcpClient = new TCPClient(shutdown, timeout, limit);
                    byte[] serverBytes = tcpClient.askServer(hostname, userPort, userInputBytes);
                    String serverOutput = new String(serverBytes);
                    if ("".equals(serverOutput))
                    {
                        responseHEAD = responseNF;
                    } else
                    {
                        responseHEAD = responseOK + serverOutput;
                    }
                } catch (UnknownHostException | ConnectException ex)
                {
                    responseHEAD = responseNF;
                }
            }
            client.getOutputStream().write(responseHEAD.getBytes(StandardCharsets.UTF_8));
            client.close();

        } catch (IOException ex)
        {
            Logger.getLogger(ConcHTTPAsk.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    String getParameters(String a, String s)
    {
        int y = s.indexOf(a);
        return s.substring(y + a.length(), s.length());
    }

}

public class ConcHTTPAsk
{
    public static void main(String[] args) throws IOException
    {
        int port = Integer.parseInt(args[0]);
        ServerSocket socket = new ServerSocket(port);
        while (true)
        {
            Socket connectionSocket = socket.accept();
            MyRunnable concClient = new MyRunnable(connectionSocket);
            Thread conClientThread = new Thread(concClient);
            conClientThread.start();
        }
    }
}
