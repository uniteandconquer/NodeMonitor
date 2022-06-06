package enums;

public enum Folders 
{
    DB("databases"),
    BIN("bin"),
    STYLES("UI/styles");    
 
    private String get;
 
    Folders(String folder) 
    {
        this.get = folder;
    }
 
    public String get()
    {
        return get;
    }
}