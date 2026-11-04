package io.github.michelep;

import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RPCServer
{
    private ServerSocket serverSocket;
    private Thread serverThread;

    private ExecutorService executor = null;

    private final List<SocketEntry> connections = new ArrayList<>();

    private final JSONDeserializer<RPCRequest> deserializer = new JSONDeserializer<>();

    private final JSONSerializer serializer = new JSONSerializer();

    private final List<RPCHandler> handlers = new ArrayList<>();

    private static RPCServer instance;

    private RPCServer(){}

    public static RPCServer instance()
    {
        if(instance == null)
            instance = new RPCServer();
        return instance;
    }

    public RPCServer register(RPCHandler handler)
    {
        handlers.add(handler);
        return this;
    }


    public void listen(int port) throws IOException
    {
        if(serverThread != null)
        {
            serverThread.interrupt();
            serverThread = null;
        }
        if(serverSocket != null)
        {
            try
            {
                serverSocket.close();
            } catch (IOException e){}
            finally
            {
                serverSocket = null;
            }
        }

        serverSocket = new ServerSocket(port);

        serverThread = new Thread(() -> {
            while(!Thread.interrupted())
            {
                try
                {
                    Socket sock = serverSocket.accept();
                    SocketEntry socketEntry = new SocketEntry(sock);

                    synchronized(connections)
                    {
                        connections.add(socketEntry);
                    }
                    if(executor == null)
                        executor = Executors.newCachedThreadPool();
                    executor.submit(socketEntry);

                } catch (IOException ignored){}
            }
        });

        serverThread.start();
    }

    public void stop()
    {
        if(serverThread != null)
        {
            serverThread.interrupt();
            serverThread = null;
        }
        if(serverSocket != null)
        {
            try
            {
                serverSocket.close();
            } catch (IOException ignored) {}
            finally
            {
                serverSocket = null;
            }
        }

        connections.forEach(SocketEntry::close);
        connections.clear();
        if(executor != null)
            executor.shutdownNow();
        executor = null;
    }

    private void handleCommand(BufferedReader in, PrintWriter out) throws IOException
    {
        String line = in.readLine();
        RPCRequest request = deserializer.deserialize(line, RPCRequest.class);
        RPCResponse response = new RPCResponse();
        response.setStatus(RPCStatus.OK.ordinal());

        Optional<RPCHandler> handlerOpt = handlers.stream()
                .filter(h -> h.endpoint().equals(request.getEndpoint()))
                .findFirst();

        if(handlerOpt.isEmpty())
        {
            response.setStatus(RPCStatus.ERROR_NOT_IMPLEMENTED.ordinal());
            String json = serializer.serialize(response);
            out.println(json);
            return;
        }

        RPCHandler handler = handlerOpt.get();
        handler.handleRequest(request, response);

        String json = serializer.deepSerialize(response);
        out.println(json);
    }

    private class SocketEntry implements Runnable, Closeable
    {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;

        private SocketEntry(Socket socket) throws IOException
        {
            this.socket = socket;
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
        }

        @Override
        public void close()
        {
            synchronized (connections)
            {
                connections.remove(this);
            }

            out.flush();
            out.close();
            out = null;

            try
            {
                in.close();
            }
            catch (IOException ignored) {}
            finally
            {
                in = null;
            }

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

        @Override
        public void run()
        {
            while(!Thread.interrupted())
            {
                try
                {
                    handleCommand(in, out);
                } catch (IOException e)
                {
                    close();
                    return;
                }
            }
        }
    }
}
