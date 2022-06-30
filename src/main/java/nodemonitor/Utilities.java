package nodemonitor;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;    
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.ConnectException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.Properties;
import java.util.Random;
import java.util.Scanner;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.swing.table.DefaultTableModel;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Utilities
{
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final int TIMEOUT_DURATION = 45;
    private static final String WEEKS =Main.BUNDLE.getString("weeks");
    private static final String DAYS =Main.BUNDLE.getString("days");
    private static final String HOURS =Main.BUNDLE.getString("hours");
    private static final String MINUTES =Main.BUNDLE.getString("minutes");
    private static final String SECONDS =Main.BUNDLE.getString("seconds");
    
    public static String CreateRandomString(int length)
    {
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        Random random = new Random();
        return random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(length)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();    
    }
    
    private static String ReadString(String requestURL) throws  ConnectException, IOException
    {   
        try (Scanner scanner = new Scanner(new URL(requestURL).openStream(),StandardCharsets.UTF_8.toString()))
        {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }
    } 
    
    public static String ReadStringFromURL(String requestURL) throws TimeoutException, IOException, ConnectException
    {
        BackgroundService.totalApiCalls++;
        
        Future<String> result = executor.submit(() -> ReadString(requestURL));

        try
        {
            return result.get(TIMEOUT_DURATION,TimeUnit.SECONDS);
        }
        catch(ExecutionException e)
        {
            if(e.getCause().toString().startsWith("java.net.ConnectException"))
                throw new ConnectException();
            if(e.getCause().toString().startsWith("java.io.IOException"))
                throw new IOException();
            
            BackgroundService.AppendLog(e.toString() + " for " + requestURL);
            BackgroundService.AppendLog(e);
            return null;
        }
        catch( InterruptedException  e)
        { 
            BackgroundService.AppendLog(e.toString() + " for " + requestURL);
            BackgroundService.AppendLog(e);
            return null;
        }
    }
    
    public static String integerFormat(int number)
    {
        return NumberFormat.getIntegerInstance().format(number);
    }
    
    public static String numberFormat(int number)
    {
        return NumberFormat.getNumberInstance().format(number);
    }
     
     /**Checks if an array of String (Objects) contains a case insensitive version of a value<br>
      *Operating System does not allow similar case insensitive names<br>
     * @param array
     * @param value
     * @return */
     public static boolean containsIgnoreCase(Object[] array, String value)
     {
         for(Object obj : array)    
         {
             if(obj.toString().equalsIgnoreCase(value))
                 return true;
         }
         
         return false;
     }
    
    public static boolean isNavKeyEvent(KeyEvent evt)
    {
        if(evt.getKeyCode() == KeyEvent.VK_DOWN)
            return true;
        if(evt.getKeyCode() == KeyEvent.VK_KP_DOWN)
            return true;
        if(evt.getKeyCode() == KeyEvent.VK_UP)
            return true;
        if(evt.getKeyCode() == KeyEvent.VK_KP_UP)
            return true;
        if(evt.getKeyCode() == KeyEvent.VK_PAGE_DOWN)
            return true;
        if(evt.getKeyCode() == KeyEvent.VK_PAGE_UP)
            return true;
        if(evt.getKeyCode() == KeyEvent.VK_LEFT)
            return true;
        if(evt.getKeyCode() == KeyEvent.VK_KP_LEFT)
            return true;
        if(evt.getKeyCode() == KeyEvent.VK_RIGHT)
            return true;
        if(evt.getKeyCode() == KeyEvent.VK_KP_RIGHT)
            return true;
        
        return false;
    }
    
    public static String MillisToDayHrMin(long milliseconds)
    {
        long days = TimeUnit.MILLISECONDS.toDays(milliseconds);  
        long hours = TimeUnit.MILLISECONDS.toHours(milliseconds) - TimeUnit.DAYS.toHours(TimeUnit.MILLISECONDS.toDays(milliseconds));  
        long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(milliseconds));
//        long seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds));
//        long ms = TimeUnit.MILLISECONDS.toMillis(milliseconds) - TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(milliseconds));
        
        if(days > 6)
        {
            long weeks = days / 7;
            return String.format("%d %s %d %s %d %s %d %s",weeks,WEEKS, days % 7,DAYS, hours,HOURS, minutes,MINUTES);
        }
        if(days >= 1)
            return String.format("%d %s %d %s %d %s", days % 7, DAYS, hours, HOURS, minutes, MINUTES);            
        if(days < 1 && hours > 0)
            return String.format("%d %s %d %s",hours, HOURS, minutes, MINUTES);
        if(hours < 1)
            return String.format("%d %s", minutes, MINUTES);
        
        return "invalid duration format"; //String.format("%d days %d hours %d minutes", days, hours, minutes);
     }
    
    public static String MillisToDayHrMinMinter(long milliseconds)
    {
        long days = TimeUnit.MILLISECONDS.toDays(milliseconds);  
        long hours = TimeUnit.MILLISECONDS.toHours(milliseconds) - TimeUnit.DAYS.toHours(TimeUnit.MILLISECONDS.toDays(milliseconds));  
        long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(milliseconds));
//        long seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds));
//        long ms = TimeUnit.MILLISECONDS.toMillis(milliseconds) - TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(milliseconds));
        
        if(days > 365)
        {
            long years = days / 365;
            long months = (days % 365) / 30;
            long weeks = (days % 30) / 7;
            return String.format("%d yrs %d months %d wks %d days",years,months, weeks, days % 7);
        }
        if(days > 30)
        {
            long months = days / 30;
            long weeks = (days % 30) / 7;   
            
            return String.format("%d months %d weeks %d days",months, weeks, days % 7);
        }
        if(days > 6)
        {
            long weeks = days / 7;
            return String.format("%d weeks %d days ",weeks, days % 7);
        }
        if(days >= 1)
            return String.format("%d days %d hours", days % 7, hours);            
        if(days < 1 && hours > 0)
            return String.format("%d hours %d minutes",hours,minutes);
        if(hours < 1)
            return String.format("%d minutes", minutes);
        
        return "invalid duration format"; //String.format("%d days %d hours %d minutes", days, hours, minutes);
     }
    
    public static String MillisToDayHrMinShort(long milliseconds)
    {
        long days = TimeUnit.MILLISECONDS.toDays(milliseconds);  
        long hours = TimeUnit.MILLISECONDS.toHours(milliseconds) - TimeUnit.DAYS.toHours(TimeUnit.MILLISECONDS.toDays(milliseconds));  
        long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(milliseconds));
//        long seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds));
//        long ms = TimeUnit.MILLISECONDS.toMillis(milliseconds) - TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(milliseconds));
        
        if(days > 365)
        {
            long years = days / 365;
            long months = (days % 365) / 30;
            long weeks = (days % 30) / 7;
            return String.format("%d y %d m %d w %d d %d h %d m",years,months, weeks, days % 7, hours, minutes);
        }
        if(days > 30)
        {
            long months = days / 30;
            long weeks = (days % 30) / 7;            
            return String.format("%d m %d w %d d %d h %d m",months, weeks, days % 7, hours, minutes);
        }
        if(days > 6)
        {
            long weeks = days / 7;
            return String.format("%d w %d d %d hrs %d min",weeks, days % 7, hours, minutes);
        }
        if(days >= 1)
            return String.format("%d d %d hrs %d min", days % 7, hours, minutes);            
        if(days < 1 && hours > 0)
            return String.format("%d hrs %d min",hours,minutes);
        if(hours < 1)
            return String.format("%d min", minutes);
        
        return "invalid duration format"; //String.format("%d days %d hours %d minutes", days, hours, minutes);
     }
    
    public static String MillisToDayHrMinShortFormat(long milliseconds)
    {
        long days = TimeUnit.MILLISECONDS.toDays(milliseconds);  
        long hours = TimeUnit.MILLISECONDS.toHours(milliseconds) - TimeUnit.DAYS.toHours(TimeUnit.MILLISECONDS.toDays(milliseconds));  
        long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(milliseconds));
        
        if(days > 6)
        {
            long weeks = days / 7;
            return String.format("%d wk %d d %d hr %d min",weeks, days % 7, hours, minutes);
        }
        if(days >= 1)
            return String.format("%d days %d hrs %d min", days % 7, hours, minutes);            
        if(days < 1 && hours > 0)
            return String.format("%d %s %d %s",hours,HOURS,minutes, MINUTES);
        if(hours < 1)
            return String.format("%d %s", minutes, MINUTES);
        
        return "invalid duration format"; //String.format("%d days %d hours %d minutes", days, hours, minutes);
     }
    
    public static String MillisToDayHrMinSec(long milliseconds)
    {
       long days = TimeUnit.MILLISECONDS.toDays(milliseconds);  
       long hours = TimeUnit.MILLISECONDS.toHours(milliseconds) - TimeUnit.DAYS.toHours(TimeUnit.MILLISECONDS.toDays(milliseconds));  
       long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(milliseconds));
        long seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds));
//        long ms = TimeUnit.MILLISECONDS.toMillis(milliseconds) - TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(milliseconds));

       if(days > 6)
       {
           long weeks = days / 7;
           return String.format("%d %s %d %s %d %s %d %s",weeks, WEEKS, days % 7, DAYS, hours, HOURS, minutes, MINUTES);
       }
       if(days >= 1)
           return String.format("%d %s %d %s %d %s", days % 7, DAYS, hours, HOURS, minutes, MINUTES);            
       if(days < 1 && hours > 0)
           return String.format("%d %s %d %s",hours, HOURS, minutes, MINUTES);
       if(hours < 1 && minutes > 0)
           return String.format("%d %s %d %s", minutes, MINUTES, seconds, SECONDS);
       if(minutes < 1)
           return String.format("%d %s", seconds, SECONDS);
           

       return "invalid duration format"; //String.format("%d days %d hours %d minutes", days, hours, minutes);
    }
    
    public static String TimeFormat(long timeMillisec)
    {        
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss z");
        return dateFormat.format(timeMillisec);
    }
    
    public static String DateFormat(long timeMillisec)
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy 'at' HH:mm:ss z");
        return dateFormat.format(timeMillisec);
    }
    
    public static String DateFormatPath(long timeMillisec)
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d yyyy 'at' HH.mm.ss");
        return dateFormat.format(timeMillisec);
    }
    
     public static String DateFormatFile(long timeMillisec)
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM-d-yy_HH.mm");
        return dateFormat.format(timeMillisec);
    }
    
    public static String DateFormatShort(double timeMillisec)
    {
        //DON'T CHANGE THIS FORMAT WITHOUT CHANGING THE INVERSE METHOD TOO
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd yyyy 'at' HH:mm");
        return dateFormat.format(timeMillisec);
    }
    
    public static long dateFormatShortToTimestamp(String date)
    {
        SimpleDateFormat f = new SimpleDateFormat("MMM dd yyyy 'at' HH:mm");
        try
        {
            Date d = f.parse(date);
            return d.getTime();
        }
        catch (ParseException e)
        {
            BackgroundService.AppendLog(e);
            return 0;
        }
    }
    
    public static long dateFormatToTimestamp(String date)
    {
        SimpleDateFormat f = new SimpleDateFormat("MMM d, yyyy 'at' HH:mm:ss z");
        try
        {
            Date d = f.parse(date);
            return d.getTime();
        }
        catch (ParseException e)
        {
            BackgroundService.AppendLog(e);
            return 0;
        }
    }
    
    public static String AllignCenterHTML(String s)
    {
        return "<html><div style='text-align: center;'>" + s + "</div><html>";
    }
    
    public static String AllignHTML(String s, String type)
    {
        return "<html><div style='text-align: " + type + ";'>" + s + "</div><html>";
    }
    
    public static double round(double value, int places)
    {
        if (places < 0)
            throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(Double.toString(value));
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
    
    public static double add(double value1, double value2)
    {
        BigDecimal bd1 = new BigDecimal(Double.toString(value1));
        BigDecimal bd2 = new BigDecimal(Double.toString(value2));
        
        return bd1.add(bd2).doubleValue();
    }
    
    public static double add(double[] values)
    {
        BigDecimal sum = new BigDecimal("0");
        
        for(double value : values)
        {
            BigDecimal bd = new BigDecimal(Double.toString(value));
            sum = sum.add(bd);
        }
        
        return sum.doubleValue();
    }    
    
    public static double subtract(double value, double subtrahend)
    {
        BigDecimal bd1 = new BigDecimal(Double.toString(value));
        BigDecimal bd2 = new BigDecimal(Double.toString(subtrahend));
        
        return bd1.subtract(bd2).doubleValue();
    }
    
    public static double divide(double numerator, double denominator)
    {
        BigDecimal bd1 = new BigDecimal(Double.toString(numerator));
        BigDecimal bd2 = new BigDecimal(Double.toString(denominator));
        
        return bd1.divide(bd2,10,RoundingMode.HALF_UP).doubleValue();
    }
    
    public static double multiply(double value1, double value2)
    {
        BigDecimal bd1 = new BigDecimal(Double.toString(value1));
        BigDecimal bd2 = new BigDecimal(Double.toString(value2));
        
        return bd1.multiply(bd2).doubleValue();
    }
    
    /**Using Double.toString() will remove trailing zeroes, but will not add<br>
     * locale specific grouping,this method does both<br>
     * Accuracy up to 10 decimals
     * @param d
     * @return double without trailing zeroes and with locale specific grouping*/
    public static String removeTrZeroes(double d)
    {
        String converted = String.format("%,.10f", d);
        int endIndex = converted.length() - 1;
        for(int i = endIndex; i >= 0; i--)
        {
            if(converted.charAt(i) != '0')
                break;            
            
            endIndex = i;
        }
        if(converted.charAt(endIndex - 1) == ',' || converted.charAt(endIndex - 1) == '.')
            endIndex -= 1;
        
        return converted.substring(0,endIndex);
    }
    
    public static void copyToClipboard(String text)
    {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        StringSelection selection = new StringSelection(text);
        clipboard.setContents(selection, selection);
    }
    
    public static boolean SendEmail(String recipient, String username,String password,String smtp, String port,String subject, String message)
    {      
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", smtp);
        props.put("mail.smtp.port", port);

        Session session = Session.getInstance(props,
                new javax.mail.Authenticator()
        {
            @Override
            protected PasswordAuthentication getPasswordAuthentication()
            {
                return new PasswordAuthentication(username, password);
            }
        });
//        session.setDebug(true);

        try
        {
            Message mimeMessage = new MimeMessage(session);
            mimeMessage.setFrom(new InternetAddress(username));
            mimeMessage.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));  
            mimeMessage.setSubject(subject);
            mimeMessage.setText(message);

            Transport.send(mimeMessage);

            return true;
        }
        catch (MessagingException e)
        {
            BackgroundService.AppendLog(e);
        }        
        return false;
    }
    
    /**When there aren't any peers that show their blockheight, this method will return 0
     * @return highest known blockheight of all connected peers*/ 
    public static int FindChainHeight()
    {    
        try
        {
            JSONArray jSONArray = new JSONArray(ReadStringFromURL("http://" + BackgroundService.GUI.dbManager.socket + "/peers"));
            int highest = 0;
            int current = 0;

            for (int i = 0; i < jSONArray.length(); i++)
            {
                    if (jSONArray.getJSONObject(i).has("lastHeight"))
                    {
                        current = jSONArray.getJSONObject(i).getInt("lastHeight");
                    }
                    if (current > highest)
                    {
                        highest = current;
                    }
                }

            return highest;
        }
        catch (IOException | TimeoutException | JSONException e)
        {
                    //some peers will not have the "lastHeight" value in their JSON, so the log will show that an error was thrown
                    BackgroundService.AppendLog(e.toString() + " @ FindChainHeight() (ignore lastHeight warning)");
                    BackgroundService.AppendLog(e);
        }    
        
        return 0;       
    }
    
    public static void CopyFile( File from, File to )
    {
        try 
        {
            Files.copy( from.toPath(), to.toPath() );            
        } 
        catch (IOException e) 
        {
            BackgroundService.AppendLog(e);
        }
    } 
     
    public static void OverwriteFile( File from, File to )
    {
        try 
        {
            Files.copy( from.toPath(), to.toPath() , StandardCopyOption.REPLACE_EXISTING);            
        } 
        catch (IOException e) 
        {
            BackgroundService.AppendLog(e);
        }
    } 
    public static DefaultTableModel BuildTableModel(String table,ResultSet rs)
    {
        try
        {            
            ResultSetMetaData metaData = rs.getMetaData();

            int timeStampIndex = 0;
            int uptimeIndex = 0;
            // names of columns
            @SuppressWarnings("UseOfObsoleteCollectionType")
            Vector<String> columnNames = new Vector<>();
            int columnCount = metaData.getColumnCount();
            for (int column = 1; column <= columnCount; column++)
            {                
                columnNames.add(metaData.getColumnName(column));
                
//                if(table.equals("NODE_PREFS"))//timestamp and uptime are booleans in this table
//                    continue;
//                
//                if(metaData.getColumnName(column).contains("TIMESTAMP"))
//                    timeStampIndex = column;
//                if(metaData.getColumnName(column).equals("UPTIME"))
//                    uptimeIndex = column;
            }

            // data of the table
            @SuppressWarnings("UseOfObsoleteCollectionType")
            Vector<Vector<Object>> data = new Vector<>();
            while (rs.next())
            {
                @SuppressWarnings("UseOfObsoleteCollectionType")
                Vector<Object> vector = new Vector<>();
                for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++)
                {
                    if(rs.getMetaData().getColumnName(columnIndex).contains("TIMESTAMP"))// if(columnIndex == timeStampIndex)
                        vector.add(DateFormat((long) rs.getObject(columnIndex)));
                    else if(rs.getMetaData().getColumnName(columnIndex).contains("UPTIME") && !table.equals("NODE_PREFS")) //if (columnIndex == uptimeIndex)
                        vector.add(MillisToDayHrMinShort((long) rs.getObject(columnIndex)));
                    else if(rs.getMetaData().getColumnName(columnIndex).equals("DURATION")) //if (columnIndex == uptimeIndex)
                        vector.add(MillisToDayHrMinShort((long) rs.getObject(columnIndex)));
                    else if(rs.getMetaData().getColumnName(columnIndex).equals("LEVEL_DURATION")) //if (columnIndex == uptimeIndex)
                        vector.add(MillisToDayHrMinMinter((long) rs.getObject(columnIndex)));
                    else
                        vector.add(rs.getObject(columnIndex));
                }
                data.add(vector);
            }

            return new DefaultTableModel(data, columnNames);
        }
        catch (SQLException e)
        {
            BackgroundService.AppendLog(e);
        }
        
        return  null;
    }
    
    /**
     * Adds a single quote to the beginning and end of the string<br>
     * Mostly used to insert a varchar into the H2 database, which needs <br>
     * to be encased in single quotes
     * @param s
     * @return supplied string in single quotes
     */
    public static String SingleQuotedString(String s)
    {
        return "'" + s + "'";
    }
    
    public static String GenerateLabelString(String label, double value)
    {
        String returnString = "";
        
        switch(label)
        {
            case "Litecoin to QORT price":
            case "Litecoin to US Dollar price":
            case "LTC to QORT trades (in LTC)":
                returnString = String.format("%,.5f LTC",value);
                break;    
            case "QORT to Litecoin price":
            case "QORT to Bitcoin price":
            case "QORT to Dogecoin price":   
            case "QORT to US Dollar price":  
            case "LTC to QORT trades (in QORT)": 
            case "BTC to QORT trades (in QORT)":  
            case "DOGE to QORT trades (in QORT)":
                returnString = String.format("%,.5f QORT",value);
                break;  
            case "Bitcoin to QORT price":        
            case "Bitcoin to US Dollar price": 
            case "BTC to QORT trades (in BTC)":
                returnString = String.format("%,.5f BTC",value);
                break; 
            case "Dogecoin to QORT price":
            case "Dogecoin to US Dollar price":    
            case "DOGE to QORT trades (in DOGE)":
                returnString = String.format("%,.5f DOGE",value);
                break; 
            case "US Dollar to QORT price":  
            case "US Dollar to Litecoin price":
            case "US Dollar to Dogecoin price":
            case "US Dollar to Bitcoin price":
                returnString = String.format("%,.5f USD",value);
                break; 
            case "BTC to QORT trades (Total)":
            case "LTC to QORT trades (Total)":
            case "DOGE to QORT trades (Total)":
            case "All trades (Total)":
                returnString = String.format("%,d trades", (int)value);
                break;                
            case "Total QORT to LTC traded":
            case "Total QORT to DOGE traded":
            case "Total QORT to BTC traded":
            case "Total QORT traded":      
                returnString = String.format("%,.2f QORT<br/>", (double) value);
                break;
            case "Total LTC to QORT traded":
                returnString = String.format("%,.2f LTC<br/>", (double) value);
                break;
            case "Total BTC to QORT traded":
                returnString = String.format("%,.2f BTC<br/>", (double) value);
                break;
            case "Total DOGE to QORT traded":
                returnString = String.format("%,.2f DOGE<br/>", (double) value);
                break;
            case "moving average":
                returnString = String.format("%,.5f avg", value);
                break;
            case "integer average":
                returnString = String.format("%,d avg", (int)value);
                break;
        }
        
        return returnString;
    }
   
    public static NumberFormat GetDoubleFormat(int decimals)
    {
        NumberFormat df = NumberFormat.getInstance();
        df.setMaximumFractionDigits(5);
        df.setMinimumFractionDigits(5);
        return  df;
    } 
    
    public static String[] EncryptPassword(char[] password, String SECRET_KEY, String SALT)
    {
        try
        {            
            SECRET_KEY = SECRET_KEY.isBlank() ? Utilities.CreateRandomString(10) : SECRET_KEY;            
            SALT = SALT.isBlank() ? Utilities.CreateRandomString(10) : SALT;
            SecureRandom random = new SecureRandom();
            
            File dir = new File(System.getProperty("user.dir") + "/bin");
            if (!dir.exists())
                dir.mkdir();            
            File outputFile = new File(dir + "/init");
            byte[] iv = new byte[16];
            //file init should only be created once, the first time a password or hash is encrypted
            //after that we just read the bytes from the file
            if(outputFile.exists())
            {
                iv = Files.readAllBytes(outputFile.toPath());
            }
            else               
            {
                random.nextBytes(iv);      
                Files.write(outputFile.toPath(), iv);
            }            
            IvParameterSpec ivspec = new IvParameterSpec(iv);

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(SECRET_KEY.toCharArray(), SALT.getBytes(), 65536, 256);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivspec);
            String encryptedPassword = Base64.getEncoder().encodeToString(
                    cipher.doFinal(charsToBytes(password)));
            
            return new String[]{encryptedPassword,SECRET_KEY,SALT};                
        }
        catch (IOException | InvalidAlgorithmParameterException | InvalidKeyException | NoSuchAlgorithmException |
                InvalidKeySpecException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e)
        {
            BackgroundService.AppendLog("Error encrypting: " + e.toString());
            BackgroundService.AppendLog(e);
        }        
        return null;
    }
    
    public static char[] DecryptPassword(String encryptedPassword, String key, String salt)
    {
          try
        {                
            File newFile = new File(System.getProperty("user.dir") + "/bin/init");
            if(newFile.exists())
            {
                byte[] iv = Files.readAllBytes(newFile.toPath());

                SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
                KeySpec spec = new PBEKeySpec(key.toCharArray(), salt.getBytes(), 65536, 256);
                SecretKey tmp = factory.generateSecret(spec);
                SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");
                IvParameterSpec ivspec = new IvParameterSpec(iv);
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipher.init(Cipher.DECRYPT_MODE, secretKey, ivspec);
                
                return bytesToChars(cipher.doFinal(Base64.getDecoder().decode(encryptedPassword)));                 
            }
            else
            {
                BackgroundService.AppendLog(newFile.getPath() + " not found");
            }            
        }
        catch (IOException | InvalidAlgorithmParameterException | InvalidKeyException | NoSuchAlgorithmException |
                InvalidKeySpecException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e)
        {
            BackgroundService.AppendLog("Error decrypting: " + e.toString());
            BackgroundService.AppendLog(e);
        }   
        return  null;
    } 
        
    public static String GeneratePasswordHash(char [] password, int iterations)
    {
        try
        {
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);

            //set hash length to 64, itertations to 655236
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, 64 * 8);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = skf.generateSecret(spec).getEncoded();
            
            return iterations + ":" + toHex(salt) + ":" + toHex(hash);
        }
        catch (NoSuchAlgorithmException | InvalidKeySpecException e)
        {
            BackgroundService.AppendLog(e);            
        }
        
        return  null;
    }       
    
    private static String toHex(byte[] array) 
    {
        BigInteger bi = new BigInteger(1, array);
        String hex = bi.toString(16);
        int paddingLength = (array.length * 2) - hex.length();
        if (paddingLength > 0)
        {
            return String.format("%0" + paddingLength + "d", 0) + hex;
        }
        else
        {
            return hex;
        }
    }
   
    protected static boolean PasswordValid(char [] originalPassword, String storedHash) 
    {
        try
        {
            String[] parts = storedHash.split(":");
            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = fromHex(parts[1]);
            byte[] hash = fromHex(parts[2]);

            PBEKeySpec spec = new PBEKeySpec(originalPassword, salt, iterations, hash.length * 8);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] testHash = skf.generateSecret(spec).getEncoded();

            int diff = hash.length ^ testHash.length;
            for (int i = 0; i < hash.length && i < testHash.length; i++)
            {
                diff |= hash[i] ^ testHash[i];
            }
            return diff == 0;

        }
        catch (NumberFormatException | NoSuchAlgorithmException | InvalidKeySpecException e)
        {
            BackgroundService.AppendLog(e);
        }
        
        return false;
    }

    private static byte[] fromHex(String hex)
    {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++)
        {
            bytes[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return bytes;
    }
    
    public static byte[] charsToBytes(char[] chars)
    {
        final ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(CharBuffer.wrap(chars));
        return Arrays.copyOf(byteBuffer.array(), byteBuffer.limit());
    }

    public static char[] bytesToChars(byte[] bytes)
    {
        final CharBuffer charBuffer = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(bytes));
        return Arrays.copyOf(charBuffer.array(), charBuffer.limit());
    }
    
    public static Object getSetting(String key,String filename)
    {
        File settingsFile = new File(System.getProperty("user.dir") + "/bin/" + filename);
        if(!settingsFile.exists())
            return null;
        
        try
        {
            String jsonString = Files.readString(settingsFile.toPath());
            if(jsonString != null)
            {     
                JSONObject jsonObject = new JSONObject(jsonString);
                if(jsonObject.has(key))
                    return jsonObject.get(key);
                else
                    return null; 
            }                
        }
        catch (IOException | JSONException e)
        {
            BackgroundService.AppendLog(e);
        }
        
        return null;
    }
    
    public static void updateSetting(String key, String value,String filename)
    {        
        File settingsFile = new File(System.getProperty("user.dir") + "/bin/" + filename);
        if(!settingsFile.exists())
            createSettingsFile(settingsFile);
        
        try
        {
            String jsonString = Files.readString(settingsFile.toPath());
            if(jsonString != null)
            {     
                JSONObject jsonObject = new JSONObject(jsonString);
                jsonObject.put(key, value);
                
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(settingsFile)))
                {
                    writer.write(jsonObject.toString(1));
                    writer.close();
                }     
            }                
        }
        catch (IOException | JSONException e)
        {
            BackgroundService.AppendLog(e);
        }
    }   
    
    public static void createSettingsFile(File settingsFile)
    {
        try
        {                    
            JSONObject jsonObject = new JSONObject();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(settingsFile)))
            {
                writer.write(jsonObject.toString(1));
                writer.close();
            }                   
        }
        catch (IOException | JSONException e)
        {
            BackgroundService.AppendLog(e);
        }
    }
    
    
    protected static long getDirectorySize(File folder)
    {
        long length = 0;
        File[] files = folder.listFiles();
        if (files != null)
        {
            for (File file : files)
            {
                if (file.isFile())
                {
                    length += file.length();
                }
                else
                {
                    length += getDirectorySize(file);
                }
            }
        }
        return length;
    }
    
    public static void ZipFiles(ArrayList<File> srcFiles, File zipFile) throws IOException
    {
        try (FileOutputStream fos = new FileOutputStream(zipFile); 
                ZipOutputStream zipOut = new ZipOutputStream(fos))
        {
            for (File fileToZip : srcFiles) 
            {
                try (FileInputStream fis = new FileInputStream(fileToZip))
                {
                    ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
                    zipOut.putNextEntry(zipEntry);
                    
                    byte[] bytes = new byte[1024];
                    int length;
                    while((length = fis.read(bytes)) >= 0) {
                        zipOut.write(bytes, 0, length);
                    }
                }
            }
        }
    }    
}

