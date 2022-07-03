package nodemonitor;

public class AlertItem
{
    public String subject;
    public String message;
    public long timestamp;
    public boolean read;
    public boolean disregardLimit;
    
    public AlertItem(String subject,String message,long timestamp,boolean read,boolean disregardLimit)
    {
        this.subject = subject;
        this.message = message;
        this.timestamp = timestamp;
        this.read = read;
        this.disregardLimit = disregardLimit;
    }
    
    @Override
    public String toString()
    {
        return subject;
    }
    
}
