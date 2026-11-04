package io.github.michelep;

import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;

import java.io.*;
import java.net.Socket;

public class RPCController implements Closeable
{
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private final JSONSerializer serializer = new JSONSerializer();
    private final JSONDeserializer<RPCResponse> deserializer = new JSONDeserializer<>();


    public RPCController(String serverAddress, int serverPort) throws IOException
    {
        connect(serverAddress, serverPort);
    }

    public RPCController()
    {
    }

    public void connect(String serverAddress, int serverPort) throws IOException
    {
        this.socket = new Socket(serverAddress, serverPort);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }

    @Override
    public void close()
    {
        if(out != null)
        {
            out.flush();
            out.close();
            out = null;
        }

        if(in != null)
            try
            {
                in.close();
            }
            catch (IOException ignored) {}
            finally
            {
                in = null;
            }

        if(socket != null)
            try
            {
                socket.close();
            }
            catch (IOException ignored) {}
            finally
            {
                socket = null;
            }
    }

    public RPCResponse sendRequest(RPCRequest request) throws IOException
    {
        String line = serializer.deepSerialize(request);
        out.println(line);
        out.flush();
        String response = in.readLine();
        return deserializer.deserialize(response, RPCResponse.class);
    }
}
