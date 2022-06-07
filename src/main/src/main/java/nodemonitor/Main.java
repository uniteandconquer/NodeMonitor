package nodemonitor;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ResourceBundle;
import oshi.SystemInfo;

public class Main 
{  
    protected static ResourceBundle BUNDLE;
    public static final int DAEMON_PORT = 54167;
    protected static final int TERMINAL_PORT = 54168;
    protected static final int UI_PORT = 54169;
    
     public static void main(String args[])
    {    
//        Locale locale = new Locale("nl", "NL");
//        Locale.setDefault(locale);
//        JOptionPane.setDefaultLocale(locale);
        BUNDLE = ResourceBundle.getBundle("i18n/Language"); 
        
        if(args[0].equals("-shutdown_daemon"))
        {
            initiateShutdownHook(DAEMON_PORT);
            return;
        }          
        if(args[0].equals("-daemon"))
        {
            CheckLaunch(false);
            System.out.println("Starting daemon process");

            //If daemon is already running, the socket at port 55667 will not be available
            try
            {
                ServerSocket server = new ServerSocket(DAEMON_PORT,0,InetAddress.getLoopbackAddress());   
                server.close();
            }
            catch (IOException e)
            {                
                System.out.println("Deamon process already running, closing this process.");
                //If cannot set up shutdown hook, exit the (daemon) program/process
                //This will be the case if multiple instances of the daemon are started, or if 
                //for some other reason the shutdown hook is unable to get started
                System.exit(0);
            }
            //Close UI if daemon is started when UI is running. Needs a separate port from terminal
            //hook, we want to enable concurrent terminal and daemon instances.
            initiateShutdownHook(UI_PORT);
            //start daemon
            BackgroundService bgs = new BackgroundService(false,true);            
            return;
        } 
        
        File dir = new File(System.getProperty("user.home"));
        if(!dir.exists())
            dir.mkdir();
        
        //Singleton implementation
        if(lockInstance(dir + "/nm_instance.lock"))
        {
            if(args.length == 0)
            {
                String message = BUNDLE.getString("invalidLaunch");
                JOptionPane.showMessageDialog(null, message);
                System.out.println(message);
                System.exit(0);
            }
            switch (args[0])
            {
                case "-setup":
                    if(new SystemInfo().getOperatingSystem().getFamily().equals("Windows"))
                    {
                         if(WinSetup.SetupLancher())
                            JOptionPane.showMessageDialog(null, BUNDLE.getString("setupComplete"));
                    }                   
                    else                        
                        JOptionPane.showMessageDialog(null, BUNDLE.getString("setupFailed"));
                    System.exit(0);
                    break;
                case "-gui":
                    //If daemon was running in background on this machine and user starts GUI instance
                    // the daemon process will be shut down when this socket connects to it
                    try (Socket socket = new Socket(InetAddress.getLoopbackAddress(),DAEMON_PORT))
                    {
                        System.out.println("Node Monitor notifier daemon process was running in the background\n"
                                + "and was shut down.");
                    }    
                    catch(IOException e){}//will usually throw exception
                    
                    CheckLaunch(true);      
                    BackgroundService bgs = new BackgroundService(true,false);  
                    break;
                case "-cli":
                    CheckLaunch(false);
                    bgs = new BackgroundService(false,false);
                    break;
                case "-shutdown_terminal":
                    //if lockfile doesn't exist but user ran the shutdown-terminal script
                    String message = "Node Monitor terminal is not running";
                    System.out.println(message);
                    System.exit(0);
                default:
                    message = BUNDLE.getString("invalidCommandLine");
                    //blocks user from starting the jar without using the proper commandline args
                    JOptionPane.showMessageDialog(null, message + args[0]);
                    System.out.println(message + args[0]);
                    break;
            }
        }
        else
        {
            switch (args[0])
            {
                case "-cli":
                    System.out.println("Node Monitor is already running on this machine.\n"
                            + "You can use the 'shutdown.sh' script for Mac and Linux\n"
                            + "or the 'shutdown-windows.bat' script to shutdown the app.");
                    System.exit(0);
                case "-shutdown_terminal":
                    initiateShutdownHook(TERMINAL_PORT);
                    break;
                default:
                    //If another instance of app is running, show a message dialog for x seconds, then close the dialog and app, or close app on close of dialog
                    JOptionPane jOptionPane = new JOptionPane(
                            Utilities.AllignCenterHTML(BUNDLE.getString("alreadyRunningMessage")),
                            JOptionPane.INFORMATION_MESSAGE);
                    
                    JDialog dlg = jOptionPane.createDialog(BUNDLE.getString("alreadyRunningTitle"));
                    dlg.addComponentListener(new ComponentAdapter()
                    {
                        @Override
                        public void componentShown(ComponentEvent e)
                        {
                            super.componentShown(e);
                            final Timer t = new Timer (12000, (ActionEvent e1) ->
                            {
                                dlg.dispose();
                            });
                            t.start();
                        }                
                    });
                    dlg.setVisible(true);
                    System.exit(0);
            }
        }        
    }   
     
     private static void initiateShutdownHook(int port)
     {
         try
         {
            try (Socket socket = new Socket(InetAddress.getLoopbackAddress(),port)){}
             switch (port)
             {
                 case DAEMON_PORT:
                    System.out.println("Node Monitor daemon successfully shut down");
                     break;
                 case TERMINAL_PORT:
                    System.out.println("Node Monitor terminal successfully shut down");
                     break;
                 case UI_PORT:
                    System.out.println("Node Monitor UI successfully shut down");
                     break;
             }
                     
         }
         catch (IOException e)
         {  
             switch (port)
             {
                 case DAEMON_PORT:
                    System.out.println("Could not initiate shutdown. Is Node Monitor daemon running?");
                     break;
                 case TERMINAL_PORT:
                    System.out.println("Could not initiate shutdown. Is Node Monitor terminal running?");
                     break;
                 case UI_PORT:
                    System.out.println("Could not initiate shutdown. Is Node Monitor UI running?");
                     break;
             }
         }
     }
     private static void CheckLaunch(boolean isGUI)
     {
         if(System.getProperty("user.dir").endsWith("NetBeansProjects\\Node Monitor"))
             return;
         //only allow launch from main node-monitor folder, this to ensure database folder and bin folder are always accessible
         //when user clicks on .bat or .vsb file in bin folder the app will execute, but user.dir will be the bin folder, we want to avoid that
         if(!System.getProperty("user.dir").endsWith("node-monitor"))
        {
            if(new SystemInfo().getOperatingSystem().getFamily().equals("Windows"))
            {
                String message = BUNDLE.getString("invalidLaunchWin");
                if(isGUI)
                    JOptionPane.showMessageDialog(null, Utilities.AllignCenterHTML(message));
                System.out.println( message);
            }
            else
            {
                String message = BUNDLE.getString("invalidLaunchOther");
                System.out.println(message);    
                if(isGUI)
                    JOptionPane.showMessageDialog(null, Utilities.AllignCenterHTML(message));
            }                
            System.exit(0);
        }
         
     }
   
    private static boolean lockInstance(final String lockFile)
    {
        try
        {
            final File file = new File(lockFile);
            final RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw"); //rw = read/write mode
            final FileLock fileLock = randomAccessFile.getChannel().tryLock();
            if (fileLock != null)
            {
                Runtime.getRuntime().addShutdownHook(new Thread()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            fileLock.release();
                            randomAccessFile.close();
                            file.delete();
                        }
                        catch (IOException e)
                        {
                            JOptionPane.showMessageDialog(null, BUNDLE.getString("removeLockFail") 
                                    + lockFile + "\n" +  e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                });
                return true;
            }
        }
        catch (IOException e)
        {
                JOptionPane.showMessageDialog(null, 
                        BUNDLE.getString("lockFileError") + lockFile + "\n" +  e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }
     
}
