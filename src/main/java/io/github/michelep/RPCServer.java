package io.github.michelep;

import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RPCServer
{
    private ServerSocket serverSocket;
    private Thread serverThread;
    private ExecutorService executor = null;
    private final List<Connection> connections = new ArrayList<>();
    private final JSONDeserializer<RPCRequest> deserializer = new JSONDeserializer<>();
    private final JSONSerializer serializer = new JSONSerializer();
    private final List<RPCHandler> handlers = new ArrayList<>();
    private final Map<String, Integer> servedRequests = new LinkedHashMap<>();
    private final Logger logger = Logger.getLogger(RPCServer.class.getName());
    private static RPCServer instance;



    private RPCServer()
    {
    }

    public static RPCServer instance()
    {
        if(instance == null)
            instance = new RPCServer();
        return instance;
    }

    public RPCServer register(RPCHandler handler)
    {
        if(handlers.stream().anyMatch(h -> h.endpoint().equals(handler.endpoint())))
            throw new IllegalArgumentException(String.format("Endpoint %s is already registered", handler.endpoint()));
        handlers.add(handler);
        servedRequests.put(handler.endpoint(), 0);
        logger.info("Registered handler for " + handler.endpoint());
        return this;
    }

    public RPCServer unregister(RPCHandler handler)
    {
        handlers.remove(handler);
        servedRequests.remove(handler.endpoint());
        logger.info("Unregistered handler for " + handler.endpoint());
        return this;
    }


    public void listen(int port) throws RPCException
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
            } catch (IOException ignored){}
            finally
            {
                serverSocket = null;
            }
        }

        try
        {
            serverSocket = new ServerSocket(port);
        } catch (IOException e)
        {
            logger.severe("Failed to listen on port " + port);
            throw new RPCException("Error while creating the server socket", e);
        }

        serverThread = new Thread(() -> {
            while(!Thread.interrupted())
            {
                try
                {
                    Socket sock = serverSocket.accept();
                    logger.info("Accepted connection from " + sock.getRemoteSocketAddress());
                    Connection connection = new Connection(sock);

                    synchronized(connections)
                    {
                        connections.add(connection);
                    }
                    if(executor == null)
                        executor = Executors.newCachedThreadPool();
                    executor.submit(connection);

                } catch (IOException ignored){}
            }
        });

        serverThread.start();
        logger.info("Listening on port " + port);
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

        connections.forEach(Connection::close);
        connections.clear();
        if(executor != null)
            executor.shutdownNow();
        executor = null;
        logger.info("Stopped");
    }

    public Map<String, Integer> servedRequests()
    {
        return Collections.unmodifiableMap(servedRequests);
    }

    public int aliveConnections()
    {
        return connections.size();
    }


    private class Connection implements Runnable, Closeable
    {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;

        private Connection(Socket socket) throws IOException
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

            logger.info("Closed connection for " + socket.getRemoteSocketAddress());
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

        private void handleCommand(BufferedReader in, PrintWriter out) throws IOException
        {
            String line = in.readLine();
            if(line == null)
                throw new IOException("Socket closed");
            RPCRequest request = deserializer.deserialize(line, RPCRequest.class);
            RPCResponse response = new RPCResponse();
            response.setStatus(RPCStatus.OK.ordinal());

            logger.info("Request received for " + request.getEndpoint());

            Optional<RPCHandler> handlerOpt = handlers.stream()
                    .filter(h -> h.endpoint().equals(request.getEndpoint()))
                    .findFirst();

            if(handlerOpt.isEmpty())
            {
                response.setStatus(RPCStatus.NOT_IMPLEMENTED.ordinal());
                String json = serializer.serialize(response);
                out.println(json);
                return;
            }

            RPCHandler handler = handlerOpt.get();
            handler.handleRequest(request, response);

            String json = serializer.deepSerialize(response);
            out.println(json);

            String endpoint = handler.endpoint();
            servedRequests.put(endpoint, servedRequests.get(endpoint) + 1);
        }
    }
}
