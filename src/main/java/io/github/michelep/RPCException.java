package io.github.michelep;

public class RPCException extends Exception
{
  public RPCException(String message)
  {
    super(message);
  }

  public RPCException(Throwable cause)
  {
    super(cause);
  }

  public RPCException(String message, Throwable cause)
  {
    super(message, cause);
  }
}
