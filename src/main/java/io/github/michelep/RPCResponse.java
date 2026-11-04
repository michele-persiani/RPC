package io.github.michelep;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class RPCResponse
{
    private int status;

    private Map<String, Object> data = new LinkedHashMap<>();

    public int getStatus()
    {
        return status;
    }

    public void setStatus(int status)
    {
        this.status = status;
    }

    public Map<String, Object> getData()
    {
        return Collections.unmodifiableMap(data);
    }

    public void setData(String key, Object value)
    {
        data.put(key, value);
    }

    public void setData(Map<String, Object> data)
    {
        this.data = new LinkedHashMap<>(data);
    }
}
