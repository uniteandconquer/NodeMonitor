package nodemonitor;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class BackgroundService 
{    
    protected DatabaseManager dbManager;
    protected static GUI GUI;
    private Notifier notifier;
    private Timer popupTimer;
    private TimerTask popupTask;
    private boolean timerRunning;
    private SystemTray tray;
    private TrayIcon trayIcon;
    protected char [] password;
    private static final Logger logger = Logger.getLogger("debug_log");  
    private FileHandler fileHandler;  
    protected static final String BUILDVERSION = "Node Monitor 1.0.1";    
    protected static boolean ISMAPPING;
    protected static SplashScreen SPLASH;
    protected static int totalApiCalls;
    
    public BackgroundService(boolean GUI_enabled,boolean isDaemon)
    {          
        try
        {
            String logName = isDaemon ? "daemon_log.txt" : "log.txt";
            // This block configures the logger with handler and formatter  
            fileHandler = new FileHandler(System.getProperty("user.dir") + "/" + logName);
            logger.addHandler(fileHandler);
            SimpleFormatter formatter = new SimpleFormatter();
            fileHandler.setFormatter(formatter);
            logger.setUseParentHandlers(false);//disable console output
        }
        catch (IOException | SecurityException e)
        {
            AppendLog(e);
        }       
        
        if(GUI_enabled)
        {    
            SPLASH = new SplashScreen();
            SPLASH.setVisible(true);         
            SetTrayIcon(true);  
            dbManager = new DatabaseManager();
            GUI = new GUI(this);     
            popupTimer = new Timer();
        }        
        else
        {
            //CLI version
            
            //Allow systray for daemon process, not for terminal
            //Systray will not work over SSH, no DISPLAY available
            if(isDaemon)
                SetTrayIcon(false);
            
             dbManager = new DatabaseManager();
             notifier = new Notifier(dbManager, null,isDaemon);
        }   
    }       

    
    private void SetTrayIcon(boolean GUI_enabled)
    {  
        if (SystemTray.isSupported())
        {
            URL imageURL = BackgroundService.class.getClassLoader().getResource("Images/icon.png");
            Image icon = Toolkit.getDefaultToolkit().getImage(imageURL);
            final PopupMenu popup = new PopupMenu();
            trayIcon = new TrayIcon(icon, "Node Monitor", popup);
            trayIcon.setImageAutoSize(true);
            tray = SystemTray.getSystemTray();
            
            if(GUI_enabled)
            {
                MenuItem guiItem = new MenuItem("Open UI");
                guiItem.addActionListener((ActionEvent e) ->{SetGUIEnabled(true);});     
                trayIcon.addActionListener((ActionEvent e) ->{SetGUIEnabled(true);});//double click action
                popup.add(guiItem);
            }
            MenuItem exitItem = new MenuItem("Exit");       
            exitItem.addActionListener((ActionEvent e) ->
            {
                if(GUI_enabled)
                    GUI.exitInitiated(false);//checks if running in background
                else
                    System.exit(0);
            });            
            
            popup.add(exitItem);
            trayIcon.setPopupMenu(popup);  
            
            //If no GUI -> tray icon is always active
            SetTrayIconActive(!GUI_enabled);
        }
    }
    
    private void SetTrayIconActive(boolean isActive)
    { 
        if(tray == null)
            return;
        
        try
        {
            if(isActive)
                tray.add(trayIcon);
            else
                tray.remove(trayIcon);
        }
        catch (AWTException e)
        {
            System.out.println(Main.BUNDLE.getString("trayIconWarning")); 
        }
    }

    //Using a seperate method in order to inform the user that the app is running in background on JFrame closure
    public void SetGUIEnabled(boolean enabled)
    {
        if(enabled)
        {
            GUI.setVisible(true);
            GUI.requestFocus(); 
            GUI.trayPopup.setVisible(false);
            SetTrayIconActive(false);
            GUI.setNodeMonitorEnabled(true);
        }
        else
        {
            //if reqording run in background, otherwise exit the program
//            if(!ISMAPPING)
//            {
//                System.exit(0);
//            }
            
            if(!SystemTray.isSupported())
            {
                GUI.setState(Frame.ICONIFIED);
                return;
            }
            GUI.setNodeMonitorEnabled(false);
            SetTrayIconActive(true);
            //Decided not to dispose of GUI on close, the resources saved do not warrant losing all the instance's values
            GUI.setVisible(false);                
            GUI.trayPopup.pack();
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            GUI.trayPopup.setLocation(screenSize.width - GUI.trayPopup.getBounds().width -35,
                    screenSize.height - GUI.trayPopup.getBounds().height - 50);
            
            GUI.trayPopup.setVisible(true);  
                                        
            if(timerRunning)
                return;            
            
            popupTask = new TimerTask()
            {                
                @Override
                public void run()
                {
                    GUI.trayPopup.setVisible(false);
                    timerRunning = false;
                }
            };    
            timerRunning = true;
            popupTimer.schedule(popupTask, 12000); 
        }
    }  
    
    public static void AppendLog(Exception e)
    {
        //Comment out when done de-bugging
//        e.printStackTrace();
        
        
        //comment 'setUseParentHandlers(false);'  in constructor
        //to enable console output for errors       
        
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        
        logger.info(sw.toString());
    }
    
    public static void AppendLog(String message)
    {  
        //Comment out when done de-bugging      
//        System.err.println(message);        
        
        logger.info(message);    
    }
    
}
