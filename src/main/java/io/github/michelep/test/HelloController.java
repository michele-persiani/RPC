package io.github.michelep.test;

import io.github.michelep.RPCController;
import io.github.michelep.RPCRequest;
import io.github.michelep.RPCResponse;

import java.io.IOException;

public class HelloController extends RPCController
{

    public String chat() throws IOException
    {
        RPCRequest request = new RPCRequest()
                .setEndpoint("/hello")
                .setData("message", "Hi there");
        RPCResponse response = sendRequest(request);

        return response.getData().get("message").toString();
    }
}
