package nzikic.pp1.util;

public class Result<T> 
{
    public Result(T t, String s)
    {
        m_result = t;
        m_info = s;
    }
    
    public Result()
    {
        m_result = null;
        m_info = null;
    }
    
    public void setResult(T result)
    {
        m_result = result;
    }
    
    public T getResult()
    {
        return m_result;
    }
    
    public void setString(String str)
    {
        m_info = str;
    }
    
    public String getString()
    {
        return m_info;
    }
    
    public void setOther(Object other)
    {
        m_other = other;
    }
    
    public Object getOther()
    {
        return m_other;
    }
    
    // private members
    private String m_info;
    private T m_result;
    private Object m_other;
}
