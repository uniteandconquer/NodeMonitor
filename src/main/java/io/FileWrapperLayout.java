package io;

import org.h2.store.fs.FilePath;
import org.h2.store.fs.FilePathWrapper;

//Using a generic FileWrapper and passing the extension to it was not working. The only way the wrapper
//seems to work is when hardcoding the extension into the MAPPING array on initialisation or in a constructor
//without any arguments. This means that we need to create a separate wrapper class for every file type. 
public class FileWrapperLayout extends FilePathWrapper
{
    private final String[][] MAPPING =
    {
        {
            ".mv.db", ".layout"
        },
        {
            ".lock.db", ".layout.lock"
        }
    };

    @Override
    public String getScheme()
    {
        return "layout";
    }

    @Override
    public FilePathWrapper wrap(FilePath base)
    {
        // base.toString() returns base.name
        FileWrapperLayout wrapper = (FileWrapperLayout) super.wrap(base);
        wrapper.name = getPrefix() + wrapExtension(base.toString());
        return wrapper;
    }

    @Override
    protected FilePath unwrap(String path)
    {
        String newName = path.substring(getScheme().length() + 1);
        newName = unwrapExtension(newName);
        return FilePath.get(newName);
    }

    protected String wrapExtension(String fileName)
    {
        for (String[] pair : MAPPING)
        {            
            if (fileName.endsWith(pair[1]))
            {
                fileName = fileName.substring(0, fileName.length() - pair[1].length()) + pair[0];
                break;
            }
        }
        return fileName;
    }

    protected String unwrapExtension(String fileName)
    {
        for (String[] pair : MAPPING)
        {            
            if (fileName.endsWith(pair[0]))
            {
                fileName = fileName.substring(0, fileName.length() - pair[0].length()) + pair[1];
                break;
            }
        }
        return fileName;
    }
    
}