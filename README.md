Example utilization
```java
int port = 40877;

RPCServer.instance().listen(port);


HelloHandler helloHandler = new HelloHandler();
RPCServer.instance().register(helloHandler);


try(HelloController helloController = new HelloController())
{
  helloController.connect("127.0.0.1", port);
  String response = helloController.chat();
  System.out.println(response);
}
```
