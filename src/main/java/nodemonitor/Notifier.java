package nodemonitor;

import enums.Folders;
import java.io.BufferedWriter;
import java.io.Console;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeoutException;
import javax.swing.JOptionPane;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Notifier
{    
    protected boolean emailEnabled;
    protected boolean emailLimitEnabled;
    protected boolean inAppEnabled;    
    protected boolean syncLostEnabled;
    protected boolean syncGainedEnabled;
    protected boolean mintLostEnabled;
    protected boolean mintGainedEnabled;
    protected boolean connectLostEnabled;
    protected boolean connectGainedEnabled;    
    protected boolean offlineEnabled;
    protected boolean onlineEnabled;
    protected int syncThreshold = 15;
    protected int connectThreshold = 5;
    protected int mintThreshold = 15;
    protected int emailLimit = 15;    
    
    private boolean wasOffline;
    private boolean wasOnline;
    private boolean wasSynced;
    private boolean wasUnsynced;
    private boolean hadGainedConnections;
    private boolean hadLostConnections;
    private boolean wasMinting;
    private boolean hasStoppedMinting;
    
    private int myBlockHeight;
    private int chainHeight;
    private int numberOfConnections;
    
    private String mintingAccount;
    private int lastBlocksMinted;
    private long stoppedMintingTime;
    private boolean mintingHaltedSent;   
    private boolean connectLostSent;   
    private boolean syncLostSent;    
        
    private DatabaseManager dbManager;
    private GUI gui;
    private Timer timer;
    protected Timer sendTimer;
    private long updateDelta = 60000;
    
    private ArrayList<AlertItem> alertsPool = new ArrayList<>();
    private ArrayList<AlertItem> sentMails = new ArrayList<>();    
         
    private int iterations = 0;
    
    private String appPw = "";
    
    protected boolean isSending;
    
    public Notifier(DatabaseManager dbManager,GUI gui, boolean isDaemon)
    {
        this.dbManager = dbManager;
        this.gui = gui;
        initialise(isDaemon);
    }
    
    private void initialise(boolean isDaemon)
    {
        initSettings();
        
        if(isDaemon)
        {
            attemptLogin();
            startShutdownHook(true);
            start();
        }
        else
        {
            startShutdownHook(false);
            
            if(gui == null)
                showConsoleMenu();
            //if gui is not null, the notifier will get started by the notificationspanel after setEmailPanel()
        }
        
    }
    
    private void attemptLogin()
    {   
        File mailFile = new File(System.getProperty("user.dir") + "/databases/mail_settings.mv.db");
        if(!mailFile.exists())
        {
            String message = "Please setup your mail server settings before launching the daemon, shutting down the program";
            BackgroundService.AppendLog(message);
            System.out.println(message);
            System.exit(0);
        }
        
        appPw = "";
        File tempFile = new File(System.getProperty("user.dir") + "/bin/daemon_tmp");        
        if(tempFile.exists())
        {
            String encryptedPw = Utilities.getSetting("1", "daemon_tmp").toString();
            String key = Utilities.getSetting("2", "daemon_tmp").toString();
            String salt = Utilities.getSetting("3", "daemon_tmp").toString();
            
            //if the user started the daemon directly and the appPassword is blank, canConnect below will 
            //return true. If appPassword is not blank but the values in daemon_tmp are blank (they are set to
            //blank after the password is dectypted), the daemon will not be able to access the mail_server 
            //database, and will be terminated
            if(!encryptedPw.isBlank())
            {            
                appPw = String.valueOf(Utilities.DecryptPassword(encryptedPw, key, salt));

                //instead of deleting the file, we clear the values. This is safer than moving the file to the trash bin.
                Utilities.updateSetting("1", "", "daemon_tmp");
                Utilities.updateSetting("2", "", "daemon_tmp");
                Utilities.updateSetting("3", "", "daemon_tmp");                
            }
        }
        
        if(!ConnectionDB.CanConnect("mail_settings", "node_monitor", appPw.toCharArray(), Folders.DB.get()))
        {
            String message = "Could not log in to mail server file, exiting daemon process.\n"
                    + "Launching the daemon directly is only possible if your mail_settings file is not encrypted.\n"
                    + "Please use the 'launch-terminal.sh' script to start the daemon with your password or to setup your mail settings.";
            System.out.println(message);
            BackgroundService.AppendLog(message);
            System.exit(0);
        }
    }
    
    private void initSettings()
    {
        
        File settingsFile = new File(System.getProperty("user.dir") + "/bin/notifications.json");
        if(!settingsFile.exists())
            Utilities.createSettingsFile(settingsFile);            
        
        try
        {
            String jsonString = Files.readString(settingsFile.toPath());
            if(jsonString != null)
            {     
                JSONObject jsonObject = new JSONObject(jsonString);
                
                if(jsonObject.has("runInBackground"))
                {
                    if(gui != null)
                        gui.notificationsPanel.inBackgroundBox.setSelected(Boolean.parseBoolean(jsonObject.getString("runInBackground")));
                }                 
                if(jsonObject.has("inAppEnabled"))
                {
                    inAppEnabled = Boolean.parseBoolean(jsonObject.getString("inAppEnabled"));
                    if(gui != null)
                        gui.notificationsPanel.inAppBox.setSelected(inAppEnabled);
                }                
                if(jsonObject.has("emailEnabled"))
                {
                    emailEnabled = Boolean.parseBoolean(jsonObject.getString("emailEnabled"));
                    if(gui != null)
                        gui.notificationsPanel.emailBox.setSelected(emailEnabled);
                }                
                if(jsonObject.has("syncLostEnabled"))
                {
                    syncLostEnabled = Boolean.parseBoolean(jsonObject.getString("syncLostEnabled"));
                    if(gui != null)
                        gui.notificationsPanel.syncLostBox.setSelected(syncLostEnabled);
                }                
                if(jsonObject.has("syncGainedEnabled"))
                {
                    syncGainedEnabled = Boolean.parseBoolean(jsonObject.getString("syncGainedEnabled"));
                    if(gui != null)
                        gui.notificationsPanel.syncGainBox.setSelected(syncGainedEnabled);
                }                
                if(jsonObject.has("connectLostEnabled"))
                {
                    connectLostEnabled =  Boolean.parseBoolean(jsonObject.getString("connectLostEnabled"));
                    if(gui != null)
                        gui.notificationsPanel.connectionsLostBox.setSelected(connectLostEnabled);
                }                
                if(jsonObject.has("connectGainedEnabled"))
                {
                    connectGainedEnabled = Boolean.parseBoolean(jsonObject.getString("connectGainedEnabled"));
                    if(gui != null)
                        gui.notificationsPanel.connectionsGainedBox.setSelected(connectGainedEnabled);
                }         
                if(jsonObject.has("mintLostEnabled"))
                {
                    mintLostEnabled =  Boolean.parseBoolean(jsonObject.getString("mintLostEnabled"));
                    if(gui != null)
                        gui.notificationsPanel.mintLostBox.setSelected(mintLostEnabled);
                }                
                if(jsonObject.has("mintGainedEnabled"))
                {
                    mintGainedEnabled = Boolean.parseBoolean(jsonObject.getString("mintGainedEnabled"));
                    if(gui != null)
                        gui.notificationsPanel.mintGainedBox.setSelected(mintGainedEnabled);
                }               
                if(jsonObject.has("offlineEnabled"))
                {
                    offlineEnabled = Boolean.parseBoolean(jsonObject.getString("offlineEnabled"));
                    if(gui != null)
                        gui.notificationsPanel.offlineBox.setSelected(offlineEnabled);
                }                
                if(jsonObject.has("onlineEnabled"))
                {
                    onlineEnabled = Boolean.parseBoolean(jsonObject.getString("onlineEnabled"));
                    if(gui != null)
                        gui.notificationsPanel.onlineBox.setSelected(onlineEnabled);
                }                 
                if(jsonObject.has("emailLimitEnabled"))
                {
                    emailLimitEnabled = Boolean.parseBoolean(jsonObject.getString("emailLimitEnabled"));
                    if(gui != null)
                        gui.notificationsPanel.emailLimitBox.setSelected(emailLimitEnabled);
                }   
                if(jsonObject.has("syncThreshold"))
                {
                    syncThreshold = Integer.parseInt(jsonObject.getString("syncThreshold"));
                    syncThreshold = syncThreshold < 5 ? 5 : syncThreshold;                    
                    syncThreshold = syncThreshold > 100 ? 100 : syncThreshold;
                    if(gui != null)
                        gui.notificationsPanel.syncSlider.setValue(syncThreshold);
                }     
                if(jsonObject.has("connectThreshold"))
                {
                    connectThreshold = Integer.parseInt(jsonObject.getString("connectThreshold"));
                    connectThreshold = connectThreshold < 1 ? 1 : connectThreshold;                    
                    connectThreshold = connectThreshold > 100 ? 100 : connectThreshold;
                    if(gui != null)
                        gui.notificationsPanel.connectionsSlider.setValue(connectThreshold);
                }   
                if(jsonObject.has("mintThreshold"))
                {
                    mintThreshold = Integer.parseInt(jsonObject.getString("mintThreshold"));
                    mintThreshold = mintThreshold < 5 ? 5 : mintThreshold;                    
                    mintThreshold = mintThreshold > 120 ? 120 : mintThreshold;
                    if(gui != null)
                        gui.notificationsPanel.mintSlider.setValue(mintThreshold);
                }
                if(jsonObject.has("emailLimit"))
                {
                    emailLimit = Integer.parseInt(jsonObject.getString("emailLimit"));
                    emailLimit = emailLimit < 5 ? 5 : emailLimit;                    
                    emailLimit = emailLimit > 100 ? 100 : emailLimit;
                    if(gui != null)
                        gui.notificationsPanel.emailLimitSlider.setValue(emailLimit);
                }
            }                
        }
        catch (IOException | JSONException e)
        {
            BackgroundService.AppendLog(e);
        }    
    }
    
    private void startShutdownHook(boolean isDaemon)
    {
        Thread thread = new Thread(()->
        {
            try
            {
                ServerSocket server;
                if(isDaemon)
                {
                    server = new ServerSocket(Main.DAEMON_PORT,0,InetAddress.getLoopbackAddress());
                    server.accept();       
                    System.out.println("Node Monitor daemon process was shutdown through shutdown script");
                    BackgroundService.AppendLog("Node Monitor daemon process was shutdown through shutdown script");
                }   
                else
                { 
                    //UI needs a separate port from terminal hook, we want to enable concurrent terminal and daemon instances.
                    int port = gui == null ? Main.TERMINAL_PORT : Main.UI_PORT;
                    String type = gui == null ? "terminal" : "UI";
                    server = new ServerSocket(port,0,InetAddress.getLoopbackAddress());
                    server.accept(); 
                    System.out.println("Node Monitor " + type + " was shutdown through shutdown script");
                    BackgroundService.AppendLog("Node Monitor " + type + " was shutdown through shutdown script");
                }   
                    
                System.exit(0);
            }
            catch (IOException e)
            {                
                BackgroundService.AppendLog("Could not initialize shutdown hook, shutting down now.\n" + e.toString());
                //If cannot set up shutdown hook, exit the (daemon) program/process
                //This will be the case if multiple instances of the daemon are started, or if 
                //for some other reason the shutdown hook is unable to get started
                System.exit(0);
            }
        });
        thread.start();
    }
    
    private void showConsoleMenu()
    {        
        System.out.println("Welcome to Node Monitor\n");
        
        String[] options =
        {
            "1- Start",
            "2- Notifications settings",
            "3- Show notifications settings",
            "4- Mail server settings",
            "5- Exit",
        };
        Scanner scanner = new Scanner(System.in);
        int option;
        while (true)
        {
            printMenu(options);
            System.out.println("\n");
            option = scanner.nextInt();
            
            switch(option)
            {
                case 1:
                    if(canStartMonitor())
                    {
                        startExtJarProgram();
                        
                        int choice = getChoice("Do you want to exit the terminal now?", scanner);
                        if(choice == 1)
                            System.exit(choice);
                    }
                    break;
                case 2:
                    setupNotifications(scanner);
                    break;
                case 3:
                    printNotificationSettings();
                    break;
                case 4:
                    setupMailServer(scanner);
                    break;
                case 5:
                    System.out.println("Node Monitor is closing, goodbye!");
                    System.exit(0);
                    break;
            }
        }
    }       
    
     private void printMenu(String[] options)
    {
        System.out.print("Choose your option : \n");
        for (String option : options)
        {
            System.out.println(option);
        }
    }  
     
     private void printNotificationSettings()
     {         
         File file = new File(System.getProperty("user.dir") + "/bin/notifications.json");
         if(!file.exists())
         {
                System.out.println("Please setup your notifications settings first.");
                return;  
         }
         
         try
        {
            String jsonString = Files.readString(file.toPath());
            if(jsonString != null)
            {     
                JSONObject jsonObject = new JSONObject(jsonString);
                System.out.println(jsonObject.toString(1));
            }                
        }
        catch (IOException | JSONException e)
        {
            BackgroundService.AppendLog(e);
        }
     }
     
     private boolean canStartMonitor()
     {
         File file = new File(System.getProperty("user.dir") + "/databases/mail_settings.mv.db");
         if(!file.exists())
         {
                System.out.println("Please setup your mail settings first.");
                return false;  
         }
         file = new File(System.getProperty("user.dir") + "/bin/notifications.json");
         if(!file.exists())
         {
                System.out.println("Please setup your notifications settings first.");
                return false;  
         }
         
        Console console = System.console();
        if(console == null)
        {
            System.out.println("Console not available, exiting program.");
            System.exit(0);
        }
        System.out.println("For safety reasons your password will not be shown as you type it.");
        System.out.println("Please enter your app password:");
        char[] pw = console.readPassword();
        if(ConnectionDB.CanConnect("mail_settings", "node_monitor", pw, Folders.DB.get()))
        {
            appPw = String.valueOf(pw);
            return true;
        }
        else
        {
            System.out.println("Invalid password, please start again or reset your mail settings.");
            return false;
        }
     }
     
     private void setupMailServer(Scanner scanner)
     {
         String message = 
           "\nTo keep your mail server credentials safe it is advised to encrypt the mail server\n"
            + "settings file with a password. Someone that gains access to an un-encrypted settings\n"
            + "file could potentially extract your mail server password from it.\n\n"
            + "Your mail server password will always be encrypted in the settings file, but the \n"
            + "file itself can only be encrypted if you provide an app password.If you encrypt your\n"
            + "mail settings file, you'll have to provide your app password every time that you start this app.\n"
            + "This is not neccesary if you don't use an app password.\n\n"
            +
            "Great care has been taken to ensure that your mail server password is stored in a secure \n"
            + "manner, however for convenience's sake the option to use an un-encrypted settings \n"
            + "file has been left up to the user. Your settings file will be stored locally and can only be \n"
            + "accessed by someone with access to your computer.\n"
            + "By continuing and saving your mail server settings you acknowledge that you are doing \n"
            + "so at your own responsibility and will not hold the developer of this app liable or accountable \n"
            + "for damages resulting from any eventual misappropriation of your mail server credentials.\n"
            + "For those who prefer to mitigate this risk it is advisable to use a throwaway e-mail account.\n\n"

            + "You can find the settings for your e-mail provider by performing an internet search. \n"
            + "Such a search might be 'smtp settings [your provider here]'. This will usually yield the required search result.\n"
            + "Please note that due to log-in restrictions by Google, Gmail is not supported unless those \n"
            + "restrictions are disabled in your Google account.\n";
         
         System.out.println(message);
         
         int choice = getChoice("Do you want to continue?",scanner);
         
         if(choice == 2)
         {
             System.out.println("Mail server settings not updated, returning to main menu...");
             return;
         }   
         
         System.out.println("For safety reasons your password will not be shown as you type it.");
         System.out.println("Please set a password for this app (leave blank for no password):");
         Console console = System.console();
         if (console == null)
         {
             System.out.println("Console not available, exiting program.");
             System.exit(0);
         }
         char[] appPassword = console.readPassword();   
         
         System.out.println("These are the settings that your e-mail provider needs in order to send \n"
                 + "an e-mail from your mail-account.\n"
                 + "The recipient is the e-mail address at which you'd like to receive the notifications.\n"
                 + "Enter recipient e-mail address:");
          String recipient = scanner.next();
          
          System.out.println("The SMTP server is the server through which the service is provided.\n"
                  + "Enter the SMTP server for your mail provider:");
          String smtp = scanner.next();
          
           System.out.println("The port number is the port at which the server listens for outgoing mail requests.\n"
                   + "Enter the port number:");
           String port = scanner.next();
           
           System.out.println("The mail username and mail password are the credentials that you use to log in to your mail provider.\n"
                   + "Enter your mail server username:");
           String username = scanner.next();      
           
           System.out.println("For safety reasons your password will not be shown as you type it.");
           System.out.println("Please enter your mail server password:");
           char[] mailPassword = console.readPassword();
         
         choice = getChoice("Do you want to send a test e-mail?",scanner);
         
         if(choice == 1)
         {      
             System.out.println("Attempting to send a test e-mail, please wait...");
             
             if (Utilities.SendEmail
                    (
                        recipient,
                        username,
                        String.copyValueOf(mailPassword),
                        smtp,
                        port,
                        "Node Monitor test mail",
                        "Congratulations, your Node Monitor mail notification settings are working properly."
                    ) )
                {
                    System.out.println("E-mail sent succesfully, check your inbox. You can now save these settings.");
                }
                else
                {
                    System.out.println("Failed to send test mail, are your settings correct?");
                }
             
             choice = getChoice("Save these settings?",scanner);             
             if(choice == 1)
                 saveSettings(recipient, smtp, port, username, mailPassword,appPassword);
             else if(choice == 2)
                 System.out.println("Mail server settings have not been saved, returning to main menu...");
         }
         else if(choice == 2)
             saveSettings(recipient, smtp, port, username, mailPassword,appPassword);
     }
     
     //Only for CLI version
     private void saveSettings(String recipient,String smtp,String port,String username, char[] mailPassword,char[] appPassword)
     {
         String[] keys = Utilities.EncryptPassword(mailPassword, "", "");
        if (keys != null)
        {
            saveCredentials(username,
                keys[0],
                smtp, 
                port,
                recipient, 
                keys[1], 
                keys[2],
                appPassword);
            
            System.out.println("Settings saved, returning to main menu...");
            appPw = String.valueOf(appPassword);
        }
        else
             System.out.println("Error while encrypting password");
     }
     
     private void setupNotifications(Scanner scanner)
     {
         System.out.println(
                 "Dependent on your notifications settings, it is possible that Node Monitor will send you a large number\n"
                + "of e-mail notifications. To avoid your inbox getting overwhelmed with notifications you can set a limit for\n"
                + "the number of e-mails that Node Monitor will send within a 24 hour period.\n\n"
                + "If you're receiving too many notifications you can try tweaking your notifications settings by enabling or disabling\n"
                + "certain notifications or by increasing or decreasing the threshold at which a notification will be sent.\n");
                  
         int choice = getChoice("Set the maximum number of e-mails per day (minimum 5, maximum 100):", scanner,5,100);
         emailLimit = choice;
         Utilities.updateSetting("emailLimit", String.valueOf(emailLimit), "notifications.json");
         
         choice = getChoice("Enable e-mail limit?:", scanner);
         emailLimitEnabled = choice == 1;
         Utilities.updateSetting("emailLimitEnabled", String.valueOf(emailLimitEnabled), "notifications.json");
         
         choice = getChoice("Send an e-mail when my node goes offline:", scanner);
         offlineEnabled = choice == 1;
         Utilities.updateSetting("offlineEnabled", String.valueOf(offlineEnabled), "notifications.json");
         
         if(offlineEnabled)
         {
            choice = getChoice("Send an e-mail when my node is back online:", scanner);         
            onlineEnabled = choice == 1;             
         }
         else
             onlineEnabled = false;
         Utilities.updateSetting("onlineEnabled", String.valueOf(onlineEnabled), "notifications.json");
         
         choice = getChoice("Send an e-mail when my node goes out of sync:", scanner);
         syncLostEnabled = choice == 1;
         Utilities.updateSetting("syncLostEnabled", String.valueOf(syncLostEnabled), "notifications.json");
         
         if(syncLostEnabled)
         {
            choice = getChoice("Send an e-mail when my node is back in sync:", scanner);
            syncGainedEnabled = choice == 1;     
            
            choice = getChoice("Out of sync threshold (minimum 5 blocks, maximum 100 blocks)", scanner,5,100);
            syncThreshold = choice;
            Utilities.updateSetting("syncThreshold", String.valueOf(syncThreshold), "notifications.json");     
         }
         else
             syncGainedEnabled = false;
         Utilities.updateSetting("syncGainedEnabled", String.valueOf(syncGainedEnabled), "notifications.json");
         
         choice = getChoice("Send an e-mail when my node stops minting:", scanner);
         mintLostEnabled = choice == 1;
         Utilities.updateSetting("mintLostEnabled", String.valueOf(mintLostEnabled), "notifications.json");
         
         if(mintLostEnabled)
         {
            choice = getChoice("Send an e-mail when my node resumes minting:", scanner);
            mintGainedEnabled = choice == 1;          
            
            choice = getChoice("Minting halted threshold (minimum 5 minutes, maximum 120 minutes)", scanner,5,120);
            mintThreshold = choice;
            Utilities.updateSetting("mintThreshold", String.valueOf(mintThreshold), "notifications.json"); 
         }
         else
             mintGainedEnabled = false;
         Utilities.updateSetting("mintGainedEnabled", String.valueOf(mintGainedEnabled), "notifications.json");
         
         choice = getChoice("Send an e-mail when my node loses connections:", scanner);
         connectLostEnabled = choice == 1;
         Utilities.updateSetting("connectLostEnabled", String.valueOf(connectLostEnabled), "notifications.json");
         
         if(connectLostEnabled)
         {
            choice = getChoice("Send an e-mail when my node re-gains connections:", scanner);
            connectGainedEnabled = choice == 1;  
            
            choice = getChoice("Connections threshold (minimum 1 peer, maximum 100 peers)", scanner,1,100);
            connectThreshold = choice;
            Utilities.updateSetting("connectThreshold", String.valueOf(connectThreshold), "notifications.json");
         }
         else
             connectGainedEnabled = false;
         Utilities.updateSetting("connectGainedEnabled", String.valueOf(connectGainedEnabled), "notifications.json");
         
         try
         {            
            choice = getChoice("Search for active minting accounts? (please make sure that the Qortal API is reachable)", scanner);
            if(choice == 2)
            {
                System.out.println("Notifications setup complete, returning to main menu...\n");
                return;
            }
            
            System.out.println("Searching for active minting accounts, please wait...");
            String jsonString = Utilities.ReadStringFromURL("http://" + dbManager.socket + "/admin/mintingaccounts");
            JSONArray jSONArray = new JSONArray(jsonString);            
            //If there's no minting account set we'll get a nullpointer exception
            if(jSONArray.length() > 0)
            {                 
               String[] accounts = new String[jSONArray.length()];
                for(int i = 0; i < jSONArray.length(); i++)
                {
                    JSONObject jso = jSONArray.getJSONObject(i);
                    accounts[i] = jso.getString("mintingAccount");
                }  
                String question = "Choose your minting account:\n";                
                for(int i = 0; i < accounts.length; i++)
                    question += (i + 1) + "- " + accounts[i];
                
                choice = getChoice(question, scanner, 1, accounts.length);
                mintingAccount = accounts[choice - 1];
                Utilities.updateSetting("mintingAccount", mintingAccount, "settings.json");       
                System.out.println("Notifications setup complete, returning to main menu...\n");
            }
            else
            {
                System.out.println("No active minting account found.");
                System.out.println("Notifications setup complete, returning to main menu...\n");                
            }
             
         }
         catch (IOException | TimeoutException | JSONException e)
         {
             System.out.println("Could not get minting accounts from API");
             System.out.println("Notifications setup complete, returning to main menu...\n");
         }         
     }
     
     private int getChoice(String question, Scanner scanner)
     {        
         int choice = 0;
         
         System.out.println(question + "\n"
                 + "1- Yes\n"
                 + "2- No\n");
         
         while(choice != 1 && choice != 2)
             choice = scanner.nextInt();
         
         return choice;
     }
     
     private int getChoice(String question, Scanner scanner, int lower, int upper)
     {        
         int choice = 0;
         
         System.out.println(question + "\n");
         
         while(choice < lower || choice > upper)
         {
             choice = scanner.nextInt();
             if(choice < lower)
                 System.out.println("Number is too low");
             if(choice > upper)
                 System.out.println("Number is too high");
         }
         
         return choice;
     }
    
    protected void start()
    {
                        
         if (timer == null)
        {
            timer = new Timer();
        }
        else
        {
            timer.cancel();
            timer = new Timer();
        }
         
         //these need to be true at start, they will trigger the code to 
         //set the actual state through the API calls
         wasSynced = true;
         wasUnsynced = true;
         hadGainedConnections = true;
         hadLostConnections = true;
         wasMinting = true;
         hasStoppedMinting = true;
         wasOnline = true;
         
         stoppedMintingTime = System.currentTimeMillis();   
         
         Object mintingAccountObject = Utilities.getSetting("mintingAccount","settings.json");
         if(mintingAccountObject == null)
             BackgroundService.AppendLog("Could not find an active minting account, minting notifications are not active");
         else
            mintingAccount = mintingAccountObject.toString();

        timer.scheduleAtFixedRate(new TimerTask()
        {
            @Override
            public void run()
            {  
                String durationString = "Node Monitor notifier has been running for " + Utilities.MillisToDayHrMin((iterations * 60000));
//                System.out.flush();
//                System.out.print("\r");
//                System.out.print(durationString); 
                if(iterations % 5 == 0)
                    BackgroundService.AppendLog(durationString);
                                
                if(!emailEnabled && !inAppEnabled)
                {
                    //This conditon does not apply for CLI mode
                    if(gui != null)
                    {                        
                        iterations = 0;
                        return;
                    }
                }
                        
                //don't check for notifications on first iteration
                if(iterations == 0)
                {
                    iterations++;
                    return;
                }
                
                try
                {         
                    String jsonString = Utilities.ReadStringFromURL("http://" + dbManager.socket + "/admin/status");
                    wasOnline = true; 
                    
                    //will not be reached if can't ping API, error will be thrown
                    if(wasOffline && onlineEnabled)
                    {
                        poolAlert("Node online", "Your Qortal node is back online.",System.currentTimeMillis());
//                        BackgroundService.AppendLog("SENDING ONLINE NOTIFICATION");
                    }
                    
                    wasOffline = false;
                    
                    //derived from admin/status
                    JSONObject jSONObject = new JSONObject(jsonString);
                    
                    if(connectLostEnabled || connectGainedEnabled)
                    {
                        numberOfConnections = jSONObject.getInt("numberOfConnections");
                        
                        if(hadGainedConnections && numberOfConnections < connectThreshold)
                        {
                            poolAlert(
                                    "Connections lost", 
                                    "Your Qortal node is now connected to " + numberOfConnections + " peers.\nThis is below the threshold "
                                    + "of " + connectThreshold + " that is set in your notifications settings.",
                                    System.currentTimeMillis());
                            
//                            BackgroundService.AppendLog("SENDING CONNECTIONS LOST NOTIFICATION");
                            hadGainedConnections = false;
                            hadLostConnections = true;
                            connectLostSent = true;
                        }
                        else if(hadLostConnections && numberOfConnections >= connectThreshold)
                        {
                            //don't send connections gained iteration on first notification iteration
                            //we only send these on re-gaining after hadLostConnections is true
                            if(connectGainedEnabled && connectLostSent)
                            {
                                connectLostSent = false;
                                
                                poolAlert(
                                        "Connections gained", 
                                        "Your Qortal node is now connected to " + numberOfConnections + " peers.\nThis is equal to or above the threshold "
                                        + "of " + connectThreshold + " that is set in your notifications settings.",
                                        System.currentTimeMillis());
                                
//                                BackgroundService.AppendLog("SENDING CONNECTIONS GAINED NOTIFICATION");
                            }
                            
                            hadGainedConnections = true;
                            hadLostConnections = false;
                            
                        }
                    }
                    
                    if(syncLostEnabled || syncGainedEnabled)
                    {
                        myBlockHeight = jSONObject.getInt("height");
                        chainHeight = findChainHeight();
                        
                        //chainheight is derived from peers and may not be accurate at all times
                        //for example, it could be 0 if no peers that broadcast their block height are found 
                        if(chainHeight >= myBlockHeight)
                        {
                            if(wasSynced && (chainHeight - myBlockHeight) > syncThreshold)
                            {
                                wasSynced = false;
                                wasUnsynced = true;
                                
                                if(syncLostEnabled)
                                {
                                    syncLostSent = true;
                                    
                                    poolAlert(
                                            "Sync lost", 
                                            "Your Qortal node is now " + (chainHeight - myBlockHeight) + " blocks behind the chain height.\n"
                                            + "This is above the threshold of " + syncThreshold + " that is set in your notifications settings.",
                                            System.currentTimeMillis());
                                    
//                                    BackgroundService.AppendLog("SENDING SYNC LOST NOTIFICATION");
                                }                                    
                            }                            
                            else if(wasUnsynced && (chainHeight - myBlockHeight) <= syncThreshold)
                            {
                                wasUnsynced = false;
                                wasSynced = true;
                                
                                //don't send sync gained notification on first notification iteration
                                //we only send these on re-gaining after wasUnsynced is true
                                if(syncGainedEnabled && syncLostSent)
                                {
                                    syncLostSent = false;
                                    
                                    poolAlert(
                                            "Sync gained", 
                                            "Your Qortal node is now " + (chainHeight - myBlockHeight) + " blocks behind the chain height.\n"
                                            + "This is equal to or below threshold of " + syncThreshold + " that is set in your notifications settings.",
                                            System.currentTimeMillis());
                                    
//                                    BackgroundService.AppendLog("SENDING SYNC GAINED NOTIFICATION");
                                }
                                    
                            }
                        }   
                    }                    
                    
                    if(mintLostEnabled || mintGainedEnabled)
                    {
                        if(mintingAccount != null)
                        {
                            jsonString = Utilities.ReadStringFromURL("http://" + dbManager.socket + "/addresses/" + mintingAccount);
                            jSONObject = new JSONObject(jsonString);
                            int blocksMinted = jSONObject.getInt("blocksMinted");
                            
                            lastBlocksMinted = lastBlocksMinted == 0 ? blocksMinted : lastBlocksMinted;
                            
                            if(wasMinting)
                            {
                                //separate from upper if to enable triggering the else below
                                if(blocksMinted <= lastBlocksMinted)
                                {                                   
                                    wasMinting = false;
                                    hasStoppedMinting = true;
                                    stoppedMintingTime = System.currentTimeMillis(); 
                                    mintingHaltedSent = false;
                                }                            
                            }
                            else if(hasStoppedMinting)
                            {
                                long notMintingTime = System.currentTimeMillis() - stoppedMintingTime;
                                notMintingTime /= 60000;
                                
                                if(blocksMinted > lastBlocksMinted)
                                {
                                    hasStoppedMinting = false;
                                    wasMinting = true;
                                    stoppedMintingTime = 0;
                                    
                                    if(mintingHaltedSent)
                                    {
                                        poolAlert(
                                            "Minting resumed", 
                                            "Minting account '" + mintingAccount + "' has resumed minting after " 
                                                    + Utilities.MillisToDayHrMinShort(notMintingTime * 60000)  + " of inactivity",
                                            System.currentTimeMillis());
                                    }
                                }
                                else
                                {
                                    
                                    if(notMintingTime >= mintThreshold && !mintingHaltedSent)
                                    {
                                        mintingHaltedSent = true;
                                        
                                        poolAlert(
                                            "Minting halted", 
                                            "Minting account '" + mintingAccount + "' has not been minting for " 
                                                    + Utilities.MillisToDayHrMinShort(notMintingTime * 60000) + ".",
                                            System.currentTimeMillis());
                                        
//                                        BackgroundService.AppendLog("SENDING MINTING HALTED NOTIFICATION");
                                    }
                                }
                            }
                            
                            lastBlocksMinted = blocksMinted;
                            
//                            if(iterations == 0)
//                                alertsPool.clear();
                            
                            sendAlertPool();
                            
                        }
                    }
                }
                catch(ConnectException e)
                {
                    wasOffline = true;
                    
                    if(wasOnline && offlineEnabled)
                    {                        
                        poolAlert("Lost connection", "Qortal Node Monitor has lost the connection to your Qortal core.\nPlease check whether "
                                + "your node is still online.",System.currentTimeMillis());
                        
//                        BackgroundService.AppendLog("SENDING OFFLINE NOTIFICATION");
                        
                        sendAlertPool();
                    }
                    
                    wasOnline = false;
                }
                catch (IOException | TimeoutException | JSONException e)
                {
                    BackgroundService.AppendLog(e);
                }
                
                iterations++;
                
            }
        }, 0, updateDelta);
    }
    
    /**Sending multiple e-mails concurrently is not allowed by mail server, we need to pool all alerts and send
     each one with a 5 second delay*/
    private void poolAlert(String subject, String message, long timestamp)
    {        
        alertsPool.add(new AlertItem(subject, message, timestamp, false));
    }    
    
    private void sendAlertPool()
    {   
        if(alertsPool.isEmpty())
            return;
        
        if(isSending)
        {
            BackgroundService.AppendLog("Send pool triggered while busy, current pool size is " + alertsPool.size());
            return;
        }
        
        isSending = true;
        
        sendTimer = new Timer();
        sendTimer.scheduleAtFixedRate(new TimerTask()
        {            
            @Override
            public void run()
            {
                sendAlert(alertsPool.get(0).subject, alertsPool.get(0).message, emailAllowed(alertsPool.get(0)));
                alertsPool.remove(0);
                
                if(alertsPool.isEmpty())
                {
                    sendTimer.cancel();
                    isSending = false;
                }
            }
        }, 0, 5000);
    }
    
    private boolean emailAllowed(AlertItem alertItem)
    {
        if(!emailLimitEnabled)
            return true;
        
        if(sentMails.isEmpty())
        {
            sentMails.add(alertItem);
            return true;
        }
        
        long day = 86400000;
        
        //first remove all mails older than 24 hours from the sent emails list
        for(int i = sentMails.size() - 1; i >= 0; i--)
        {
            if(System.currentTimeMillis() - sentMails.get(i).timestamp > day)BackgroundService.AppendLog("removing " + Utilities.MillisToDayHrMin(System.currentTimeMillis() - sentMails.get(i).timestamp) + " old mail" );
                sentMails.remove(i);
        }   
        
        //If the sent mails list size <= emailLimit after pruning older mails
        //it means the limit has not been reached
        if(sentMails.size() < emailLimit)
        {
            sentMails.add(alertItem);
            return true;
        }
        else
            return false;
    }
    
    private void sendAlert(String subject, String message, boolean emailAllowed)
    {       
        //separate from sendAlertToGui in order to occlude footer
        if(gui != null)
            gui.showInAppNotification(message);

        message += "\n\nAt time of notification:\n\n"
             + "Node blockheight: " + Utilities.numberFormat(myBlockHeight) +
                "\nChain height: " + Utilities.numberFormat(chainHeight) +
                "\nConnected peers: " + numberOfConnections;

        sendAlertToGui(subject, message);    
        
        if(!ConnectionDB.CanConnect("mail_settings", "node_monitor", appPw.toCharArray(), Folders.DB.get()))
        {
            //No sense in running the daemon if cannot acces e-mail settings
            //fail safe, should have shut down on attemptLogin()
            if(gui == null)
            {
                BackgroundService.AppendLog("Could not log in to mail_settings database, exiting program");
                System.exit(0);
            }
            else
            {
                //return and skip e-mail if cannot access e-mail settings
                Utilities.updateSetting("emailEnabled", "false", "notifications.json");
                emailEnabled = false;
                gui.notificationsPanel.emailBox.setSelected(false);
                BackgroundService.AppendLog("Could not log in to mail_settings database, e-mail was not sent.");
                return;
            }
        }
        
         try (Connection c = ConnectionDB.getConnection("mail_settings", "node_monitor", appPw.toCharArray(), Folders.DB.get()))
        {   
            String recipient = "               ";
            
            //if no mailserver was set up, recipient field will stay empty (padded)
            if(dbManager.TableExists("mail_server", c))
                recipient = (String)dbManager.GetItemValue("mail_server", "recipient", "id", "0", c);          
            
            if(!emailEnabled)
                return;          
            
            if(!emailAllowed)
                return;
            
//            System.out.println("UNCOMMENT @ SENDALERT");//DEBUGGING
            
            //send email
            char[] password = Utilities.DecryptPassword(
                    (String)dbManager.GetItemValue("mail_server", "password", "id", "0", c),
                    (String)dbManager.GetItemValue("mail_server", "key", "id", "0", c), 
                    (String)dbManager.GetItemValue("mail_server", "salt", "id", "0", c));
            
            if(password != null)
            {
                if(!Utilities.SendEmail(
                        recipient,
                        (String)dbManager.GetItemValue("mail_server", "username", "id", "0", c), 
                        String.copyValueOf(password),
                        (String)dbManager.GetItemValue("mail_server", "smtp", "id", "0", c),
                        (String)dbManager.GetItemValue("mail_server", "port", "id", "0", c),
                        subject,
                        message))
                {
                    String subject2 = Main.BUNDLE.getString("emailErrorSubject");
                    String message2 = String.format(Main.BUNDLE.getString("emailError") + "%s\n\n%s", subject,message);
                    if(gui != null)
                        sendAlertToGui(subject2, message2);
                }
                Arrays.fill(password, '\0');
            }
            
            c.close();
        }
        catch (NullPointerException | SQLException e)
        {
            BackgroundService.AppendLog(e);
        }        
    }
    
    private void sendAlertToGui(String subject,String message)
    {  
        try(Connection c = ConnectionDB.getConnection("notifications", Folders.DB.get()))
        {
            if(!dbManager.TableExists("notifications", c))
                   dbManager.CreateTable(new String[]{"notifications","timestamp","long",
                       "subject","varchar(255)","message","varchar(MAX)"}, c);

            if(message.contains("'"))
                   message = message.replace("'", "''");
            
            long timestamp = System.currentTimeMillis();
            //Prune milliseconds from timestamp, we need it to be in seconds to extract the long from the 
            //dateformat in the notifications table selection listener
            timestamp -= timestamp % 1000;
            
               dbManager.InsertIntoDB(new String[]{"notifications",
                   "timestamp",String.valueOf(timestamp),
                   "subject",Utilities.SingleQuotedString(subject),
                   "message",Utilities.SingleQuotedString(message),}, c); 
               
               if(gui != null)
                   gui.notificationsPanel.refreshNotifications();
        }
        catch (Exception e)
        {
            BackgroundService.AppendLog(e);
        }
    }
    
    protected void setAppPw(String pw)
    {
        appPw = pw;
    }
    
    /**When there aren't any peers that show their blockheight, this method will return 0
     * @return highest known blockheight of all connected peers*/ 
    private int findChainHeight()
    {    
        try
        {
            JSONArray jSONArray = new JSONArray(Utilities.ReadStringFromURL("http://" + dbManager.socket + "/peers"));
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
        
    protected void saveCredentials(String username, String password,String smtp,String port,String recipient, String key, String salt,char [] appPassword)
    {
        Thread thread = new Thread(()->
        {
            File file = new File(System.getProperty("user.dir") + "/databases/mail_settings.mv.db");
            if(file.exists())
            {
                //This ensures that the notifier is not trying to access the mail_settings file by sending
                //notifications while the file is being changed
                if(isSending)
                {
                    sendTimer.cancel();
                    isSending = false;
                }
                
                file.delete();
            }        

            try (Connection connection = ConnectionDB.getConnection("mail_settings", "node_monitor", appPassword, Folders.DB.get()))
            {     
                if(!dbManager.TableExists("mail_server", connection))
                    dbManager.CreateTable(new String[]{"mail_server","id","tinyint",
                        "username","varchar(255)","password","varchar(255)","smtp","varchar(255)","port","varchar(255)",
                        "recipient","varchar(255)","key","varchar(255)","salt","varchar(255)"}, 
                            connection);

    //            dbManager.ExecuteUpdate("delete from mail_server", connection);

                dbManager.InsertIntoDB(new String[]{"mail_server",
                    "id","0",
                    "username",Utilities.SingleQuotedString(username),
                    "password",Utilities.SingleQuotedString(password),
                    "smtp",Utilities.SingleQuotedString(smtp),
                    "port",Utilities.SingleQuotedString(port),
                    "recipient",Utilities.SingleQuotedString(recipient),
                    "key",Utilities.SingleQuotedString(key),
                    "salt",Utilities.SingleQuotedString(salt)}, connection);

                connection.close();
            }
            catch (NullPointerException | SQLException e)
            {
                if(gui != null)
                    JOptionPane.showMessageDialog(gui, "Error saving settings.\n" + e.toString());
                else
                    System.out.println("Error saving settings.\n" + e.toString());
                BackgroundService.AppendLog(e);
            }        
            
        });
        thread.start();            
    }
    
    public void startExtJarProgram()
    {
        try
        {
            ServerSocket server = new ServerSocket(55667, 0, InetAddress.getLoopbackAddress());
            server.close();
        }
        catch (IOException e)
        {
            System.out.println("Node monitor daemon is already running.");
            BackgroundService.AppendLog("Node monitor daemon is already running.");
            //If cannot set up shutdown hook, exit the (daemon) program/process
            //This will be the case if multiple instances of the daemon are started, or if 
            //for some other reason the shutdown hook is unable to get started
            return;
        }        
            
        //If the user has encrypted the mail_settings database with a password, we need a way to login to that
        //database from the daemon process. The user is prompted for a password when starting the daemon
        //we encrypt it to a one time file which gets cleared of the values immediately after the daemon is done with it
        if (!appPw.isBlank())
        {
            try
            {
                File tempFile = new File(System.getProperty("user.dir") + "/bin/daemon_tmp");

                JSONObject jsonObject = new JSONObject();
                String salt = Utilities.CreateRandomString(10);
                String key = Utilities.CreateRandomString(10);

                String[] keys = Utilities.EncryptPassword(appPw.toCharArray(), key, salt);
                jsonObject.put("1", keys[0]);
                jsonObject.put("2", key);
                jsonObject.put("3", salt);

                try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile)))
                {
                    writer.write(jsonObject.toString());
                    writer.close();
                }
            }
            catch (IOException | JSONException e)
            {
                BackgroundService.AppendLog(e);
            }
        }      
        
        String extJar = Paths.get(System.getProperty("user.dir") + "/daemon.sh").toString();
        ProcessBuilder processBuilder = new ProcessBuilder(extJar);
        processBuilder.redirectError(new File(Paths.get(System.getProperty("user.dir") + "/bin/extJar.txt").toString()));
        processBuilder.redirectInput();
        try
        {
            final Process process = processBuilder.start();
            
            System.out.println("\nStarting node monitor daemon, this process will run in the background and send\n"
                    + "e-mail notifications according to your notifications settings. You can terminate the daemon\n"
                    + "with the 'shutdown-daemon.sh' script. The terminal can be terminated remotely with the\n"
                    + "'shutdown-terminal.sh' script.\n");
            
            //This code is used when we want the current program to wait for the external program to exit, which is currently not our usecase
//            try
//            {
//                final int exitStatus = process.waitFor();
//                if (exitStatus == 0)
//                {
//                    System.out.println("External Jar Started Successfully.");
////                    System.exit(0); //or whatever suits 
//                }
//                else
//                {
//                    System.out.println("There was an error starting external Jar. Perhaps path issues. Use exit code " + exitStatus + " for details.");
//                    System.out.println("Check also " + System.getProperty("user.dir") + "/extJar.txt file for additional details.");
////                    System.exit(1);//whatever
//                }
//            }
//            catch (InterruptedException ex)
//            {
//                System.out.println("InterruptedException: " + ex.getMessage());
//            }
        }
        catch (IOException ex)
        {
            System.out.println("IOException. Faild to start process. Reason: " + ex.getMessage());
        }
        
//        System.out.println("Process Terminated.");
//        System.exit(0);

    }
    
}
