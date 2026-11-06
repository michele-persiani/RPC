package io.github.michelep.test;

import io.github.michelep.RPCServer;


public class Main
{
    public static void main(String[] args)
    {
        int port = 40877;

        try
        {
            RPCServer.instance().listen(port);
            HelloHandler helloHandler = new HelloHandler();
            RPCServer.instance().register(helloHandler);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }



        try(HelloController helloController = new HelloController())
        {
            helloController.connect("127.0.0.1", port);
            String response = helloController.chat();
            System.out.println(response);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
