package io.github.michelep.test;

import io.github.michelep.*;


public class HelloController extends RPCController
{

    public String chat() throws RPCException
    {
        RPCRequest request = new RPCRequest()
                .setEndpoint("/hello")
                .setData("message", "Hi there");
        RPCResponse response = send(request);

        if(response.getStatus() != RPCStatus.OK.ordinal())
            throw new RPCException("RPC failed. Status: " + response.getStatus());

        return response.getData().get("message").toString();
    }
}
