package io.github.michelep.test;

import io.github.michelep.RPCHandler;
import io.github.michelep.RPCRequest;
import io.github.michelep.RPCResponse;


public class HelloHandler implements RPCHandler
{
    @Override
    public String endpoint()
    {
        return "/hello";
    }

    @Override
    public void handleRequest(RPCRequest request, RPCResponse response)
    {
        response.setData("message", "Hello!");
    }
}
