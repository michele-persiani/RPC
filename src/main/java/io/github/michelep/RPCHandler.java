package io.github.michelep;

public interface RPCHandler
{
    String endpoint();

    void handleRequest(RPCRequest request, RPCResponse response);
}
