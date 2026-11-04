package io.github.michelep;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class RPCRequest
{
    private String endpoint;
    private Map<String, Object> data = new LinkedHashMap<>();


    public String getEndpoint()
    {
        return endpoint;
    }

    public RPCRequest setEndpoint(String endpoint)
    {
        this.endpoint = endpoint;
        return this;
    }

    public Map<String, Object> getData()
    {
        return Collections.unmodifiableMap(data);
    }

    public RPCRequest setData(String key, Object value)
    {
        this.data.put(key, value);
        return this;
    }

    public void setData(Map<String, Object> data)
    {
        this.data = new LinkedHashMap<>(data);
    }
}
