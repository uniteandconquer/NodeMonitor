package nodemonitor;

import enums.Extensions;
import enums.Folders;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import org.json.JSONException;
import org.json.JSONObject;

public class GUI extends javax.swing.JFrame
{
    protected final BackgroundService backgroundService;
    protected final DatabaseManager dbManager;    
    private String currentCard = "monitorPanel"; 
    private TimerTask popupTask;
    private boolean timerRunning;
    private Timer popupTimer;
    private boolean initComplete;
    
    public GUI(BackgroundService bgs)
    {              
        backgroundService = bgs;
        dbManager = bgs.dbManager;
//        setLastLookAndFeel();//do this before initComponents() & after setting dbManager
        initComponents();
        nodeMonitorPanel.CreateMonitorTree();
        initFrame();    
        
        nodeMonitorPanel.RestartTimer();
        nodeMonitorPanel.startTime = System.currentTimeMillis(); 
        initComplete = true;
        
    }//end constructor
    
    private void initFrame()
    {
        getSettings();    
        //put the frame at middle of the screen,add icon and set visible
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        URL imageURL = GUI.class.getClassLoader().getResource("Images/icon.png");
        Image icon = Toolkit.getDefaultToolkit().getImage(imageURL);
        setTitle(BackgroundService.BUILDVERSION);
        setIconImage(icon);        
        
        Object widthObject = Utilities.getSetting("frameWidth", "settings.json");
        Object heightObject = Utilities.getSetting("frameHeight", "settings.json");
        if(widthObject != null && heightObject != null)
        {
            int frameWidth = Integer.parseInt(widthObject.toString());
            int frameHeight = Integer.parseInt(heightObject.toString());
            if(dim.width >= frameWidth && dim.height >= frameHeight)
                setSize(new Dimension(frameWidth, frameHeight));
        }
        
        setLocation(dim.width / 2 - getSize().width / 2, dim.height / 2 - getSize().height / 2);
        appearancePanel.updateStylesList();
        appearancePanel.updateGuiItems();
        
        setVisible(true);
        BackgroundService.SPLASH.setVisible(false);  
    }
    
    private void getSettings()
    {        
        File settingsFile = new File(System.getProperty("user.dir") + "/bin/settings.json");
        if(!settingsFile.exists())
        {
            createSettingsFile(settingsFile);
            
            //set default style and L&F if settings file doesn't exist
            File styleFile = new File(System.getProperty("user.dir") + "/UI/styles/GiseleH.style");
            if(styleFile.exists())
                Utilities.updateSetting("currentStyle", "GiseleH", "settings.json");
            
             Utilities.updateSetting("currentLnf", "Nimbus", "settings.json");
        }
        
        try
        {
            String jsonString = Files.readString(settingsFile.toPath());
            if(jsonString != null)
            {     
                JSONObject jsonObject = new JSONObject(jsonString);

                String currentStyle = jsonObject.optString("currentStyle");
                if(!currentStyle.isBlank())
                {
                    File folder = new File(System.getProperty("user.dir") + "/" + Folders.STYLES.get());
                    if(!folder.isDirectory())
                        folder.mkdir();

                   File[] listOfFiles = folder.listFiles();

                   for (File file : listOfFiles)
                   {
                       if(file.isDirectory())
                           continue;

                       if(!file.getName().endsWith(".style"))
                           continue;

                       String dbName = file.getName().substring(0,file.getName().length() - 6);
                       if(currentStyle.equals(dbName))
                       {
                           appearancePanel.setGuiValues(dbName, Folders.STYLES.get(), Extensions.STYLE);
                           appearancePanel.updateGuiItems();
                       }
                   }  
                } 
                String currentLnf = jsonObject.optString("currentLnf");
                if(!currentLnf.isBlank())
                {
                    setLookAndFeel(currentLnf);
                }                
                
                int newLoginCount;

                String dismissed = jsonObject.optString("donateDismissed");
                if(dismissed.isBlank())
                    jsonObject.put("donateDismissed", "false");  

                String loginCount = jsonObject.optString("loginCount");
                if(loginCount.isBlank())
                {
                    jsonObject.put("loginCount", "1");
                    newLoginCount = 1;
                }
                else
                {
                    newLoginCount = 1 + Integer.parseInt(loginCount);
                    jsonObject.put("loginCount", String.valueOf(newLoginCount));                      
                }   
                
                //MUST write to json before opening (modal) dialog, otherwise it will overwrite
                //the user's dismiss donate pref after clicking dismissButton
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(settingsFile)))
                {
                    writer.write(jsonObject.toString(1));
                    writer.close();
                }                          
                if(dismissed.equals("false") && newLoginCount % 20 == 0)
                {
                    donateDialog.pack();
                    int x = getX() + ((getWidth() / 2) - (donateDialog.getWidth() / 2));
                    int y = getY() + ((getHeight() / 2) - (donateDialog.getHeight() / 2));
                    donateDialog.setLocation(x, y);
                    donateDialog.setVisible(true);   
                }  
            }                
        }
        catch (IOException | JSONException e)
        {
            BackgroundService.AppendLog(e);
        }
    }    
    
    private void createSettingsFile(File settingsFile)
    {
        try
        {                    
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("donateDismissed", "false");  
            jsonObject.put("loginCount", "0"); 
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
         
    protected void showDonateDialog()
    {
        SwingUtilities.invokeLater(()->
        {
            donateDialog.pack();
            int x = getX() + ((getWidth() / 2) - (donateDialog.getWidth() / 2));
            int y = getY() + ((getHeight() / 2) - (donateDialog.getHeight() / 2));
            donateDialog.setLocation(x, y);
            donateDialog.setVisible(true);   
        });
    }
    
    protected void updateStyle(Color bgColor, Color cmpColor, Color fontColor, Color cmpFontColor, String fontType)
    {
        toolbar.setBackground(bgColor);

        for (Component c : toolbar.getComponents())
        {
            if (c instanceof JButton)
            {
                c.setBackground(cmpColor);
                c.setFont(new Font(fontType, c.getFont().getStyle(), c.getFont().getSize()));
                c.setForeground(cmpFontColor);
            }
        }

        JPanel monitorPanel = (JPanel) nodeMonitorPanel.getComponent(0);
        monitorPanel.setBackground(bgColor);
        for (Component c : monitorPanel.getComponents())
        {
            if (c instanceof JScrollPane)
            {
                //Jtree is child of viewport
                JViewport viewPort = (JViewport) ((JScrollPane) c).getComponent(0);
                JTree tree = (JTree) viewPort.getComponent(0);
                tree.setBackground(bgColor);
                tree.setFont(new Font(fontType, tree.getFont().getStyle(), tree.getFont().getSize()));
                
                //We have to set the foreground color for the label of every node's NodeInfo (userObject)
                var treeModel = (DefaultTreeModel) tree.getModel();
                var node = (DefaultMutableTreeNode) treeModel.getRoot();
                var nodeInfo = (NodeInfo) node.getUserObject();
                nodeInfo.setForeground(fontColor);
                for (int currentChild = 0; currentChild < node.getChildCount(); currentChild++)
                {
                    var child = (DefaultMutableTreeNode) node.getChildAt(currentChild);
                    nodeInfo = (NodeInfo) child.getUserObject();
                    nodeInfo.setForeground(fontColor);

                    for (int currentGrandChild = 0; currentGrandChild < child.getChildCount(); currentGrandChild++)
                    {
                        var grandChild = (DefaultMutableTreeNode) child.getChildAt(currentGrandChild);
                        nodeInfo = (NodeInfo) grandChild.getUserObject();
                        nodeInfo.setForeground(fontColor);
                        nodeInfo.setBackground(bgColor);
                    }
                }
            }

            if (c instanceof JLabel)
            {
                c.setFont(new Font(fontType, c.getFont().getStyle(), c.getFont().getSize()));
                c.setForeground(fontColor);
            }
            if (c instanceof JButton)
            {
                c.setBackground(cmpColor);
                c.setFont(new Font(fontType, c.getFont().getStyle(), c.getFont().getSize()));
                c.setForeground(cmpFontColor);
            }
            if(c instanceof JSlider)
                c.setBackground(bgColor);
        }
        SwingUtilities.updateComponentTreeUI(monitorPanel);

        tipJarPanel.setBackground(bgColor);
        for (Component c : tipJarPanel.getComponents())
        {
            if (c instanceof JLabel)
            {
                c.setFont(new Font(fontType, c.getFont().getStyle(), c.getFont().getSize()));
                c.setForeground(fontColor);
            }
            if (c instanceof JTextField)
            {
                c.setBackground(cmpColor);
                c.setFont(new Font(fontType, c.getFont().getStyle(), c.getFont().getSize()));
                c.setForeground(cmpFontColor);
            }
        }

        //setting popupmenu bg color doesn't work, using a lineborder removes the insets with default color
        appearanceMenu.setBorder(BorderFactory.createLineBorder(bgColor));
        appearanceMenu.setBackground(bgColor);
        for (Component c : appearanceMenu.getComponents())
        {

            if (c instanceof JRadioButtonMenuItem)
            {
                c.setBackground(cmpColor);
                ((JRadioButtonMenuItem) c).setOpaque(true);
                c.setFont(new Font(fontType, c.getFont().getStyle(), c.getFont().getSize()));
                c.setForeground(cmpFontColor);
            }
        }
        
        donatePanel.setBackground(bgColor);
        LineBorder lineBorder =  new LineBorder(cmpColor, 5, true); 
        donatePanel.setBorder(BorderFactory.createCompoundBorder(
                lineBorder, BorderFactory.createBevelBorder(BevelBorder.LOWERED)));
        walletsButton.setBackground(cmpColor);
        walletsButton.setForeground(cmpFontColor);
        remindLaterButton.setBackground(cmpColor);
        remindLaterButton.setForeground(cmpFontColor);
        dismissButton.setBackground(cmpColor);
        dismissButton.setForeground(cmpFontColor);
        donateLabel.setForeground(fontColor);             
        
        hintDialogPanel.setBackground(bgColor);
        hintDialogLabel.setBackground(bgColor);
        hintDialogLabel.setOpaque(true);
        hintDialogLabel.setForeground(fontColor);
        hintDialogLabel.setFont(new Font(fontType, hintDialogLabel.getFont().getStyle(), hintDialogLabel.getFont().getSize()));
        
        popUpLabel.setBackground(bgColor);
        popUpLabel.setForeground(fontColor);
        popUpLabel.setFont(new Font(fontType, popUpLabel.getFont().getStyle(), popUpLabel.getFont().getSize()));
        popUpLabel.setBorder(BorderFactory.createCompoundBorder(new LineBorder(cmpColor, 4, true), new SoftBevelBorder(BevelBorder.LOWERED)));

        notificationPopupLabel.setBackground(bgColor);
        notificationPopupLabel.setForeground(fontColor);
        notificationPopupLabel.setFont(new Font(fontType, notificationPopupLabel.getFont().getStyle(), notificationPopupLabel.getFont().getSize()));
        notificationPopupPanel.setBackground(bgColor);
        notificationPopupPanel.setBorder(BorderFactory.createCompoundBorder(new LineBorder(cmpColor, 4, true), new SoftBevelBorder(BevelBorder.LOWERED)));
        
        notificationsPanel.updateStyle(bgColor, cmpColor, fontColor, cmpFontColor, fontType);        
    }
    
    protected void showInAppNotification(String message)
    {
        notificationPopupLabel.setText(Utilities.AllignHTML(message,"justify"));
        
        notificationPopup.pack();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        notificationPopup.setLocation(screenSize.width - notificationPopup.getBounds().width - 35,
                screenSize.height - notificationPopup.getBounds().height - 50);

        notificationPopup.setVisible(true);

        if (timerRunning)
            return;

        popupTimer = new Timer();
        popupTask = new TimerTask()
        {
            @Override
            public void run()
            {
                notificationPopup.setVisible(false);
                timerRunning = false;
            }
        };
        timerRunning = true;
        popupTimer.schedule(popupTask, 12000);                
    }
    
    protected void showHintDialog(MouseEvent evt, String message)
    {
        hintDialogLabel.setText(Utilities.AllignHTML(message,"justify"));

        int x = (int)evt.getLocationOnScreen().getX();
        int y = (int)evt.getLocationOnScreen().getY();
        setInfoDialogBounds(x, y, 6);
    }
    
    /**
     *The size of the dialog needs to be adjusted to the font type.<br>
     *We get the width of the entire string and divide that by the width of the label<br>
     *to find out how many lines the string will have. Then we add the line breaks in<br>
     *the string to find the actual lines. We multiply the number of actual lines by the<br>
     *string's actual height and add some margin to get the dialog height*/ 
     private void setInfoDialogBounds(int x, int y, int lineBreaks)
     {
        FontRenderContext frc = new FontRenderContext(new AffineTransform(), true, true);
        int textwidth = (int)(hintDialogLabel.getFont().getStringBounds(hintDialogLabel.getText(), frc).getWidth());
        int textheight = (int)(hintDialogLabel.getFont().getStringBounds(hintDialogLabel.getText(), frc).getHeight());        
        int lines = textwidth / (400 - 60);//width dialog - label left/right insets
        lines += lineBreaks;        
        //Dialog will always be 400 pixels wide, the height will depend on content and font type
        hintDialog.setPreferredSize(new Dimension(400, (textheight * lines) + 150));
        hintDialog.pack();
        
        hintDialog.setLocation(x - (hintDialog.getWidth()/ 2),  y - (hintDialog.getHeight() /2));
        
        hintDialog.setVisible(true);
     }
    
    protected void ExpandTree(JTree tree, int nodeLevel)
    {
        var currentNode = (DefaultMutableTreeNode) tree.getModel().getRoot();        
        
        do
        {               
            if (currentNode.getLevel() == nodeLevel) 
            {
                tree.expandPath(new TreePath(currentNode.getPath()));
            }
            
            currentNode = currentNode.getNextNode();
        } 
        while (currentNode != null);
    }
    
    protected void ExpandNode(JTree tree, DefaultMutableTreeNode currentNode,int nodeLevel)
    {        
        DefaultMutableTreeNode original = currentNode;
        do
        {
            if (currentNode.getLevel() == nodeLevel) 
                tree.expandPath(new TreePath(currentNode.getPath()));
            
            currentNode = currentNode.getNextNode().isNodeAncestor(original) ? currentNode.getNextNode() : null;            
        } 
        while (currentNode != null);
    }
    
    protected void setNodeMonitorEnabled(boolean enabled)
    {
        if(enabled && currentCard.equals("nodeMonitor"))
            nodeMonitorPanel.RestartTimer();
        
        if(!enabled)
            nodeMonitorPanel.timer.cancel();
    }

    protected void setLookAndFeel(String styleString)
    {
        try
        {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels())
            {
                if (styleString.equals(info.getName()))
                {
                    //in case nimbus dark mode button text is not visible
//                    if(styleString.equals("Nimbus"))
//                        UIManager.getLookAndFeelDefaults().put("Button.textForeground", Color.BLACK);  
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    SwingUtilities.updateComponentTreeUI(this);
                    if(donateDialog != null)
                        SwingUtilities.updateComponentTreeUI(donateDialog);
                    if(appearanceMenu != null)
                        SwingUtilities.updateComponentTreeUI(appearanceMenu);
                    if(appearancePanel != null)
                    {
                        SwingUtilities.updateComponentTreeUI(appearancePanel.colorChooser);
                        appearancePanel.updateGuiItems();//style and layout menu size needs update for diff L&F
                    }
                    break;
                }
            }
        }
        catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex)
        {
            java.util.logging.Logger.getLogger(GUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
    }
    
    protected void exitInitiated(boolean isWindowEvent)
    {                 
        if(isWindowEvent && notificationsPanel.inBackgroundBox.isSelected())
        {
            backgroundService.SetGUIEnabled(false);
        }
        else
        {
            if (BackgroundService.ISMAPPING)
            {
                if (JOptionPane.showConfirmDialog(
                        this,
                        Utilities.AllignCenterHTML(Main.BUNDLE.getString("exitConfirm")),
                        Main.BUNDLE.getString("exitConfirmTitle"),
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.PLAIN_MESSAGE) == JOptionPane.YES_OPTION)
                {
                    //Reserved in case run in background gets implemented later
                }
            }
            else
                System.exit(0);
        }
    }
    
    private void pasteToLabel(String coin)
    {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable t = clipboard.getContents(this);
        if (t == null)
            return;
        try
        {
            clipboardLabel.setText(coin + " address copied to clipboard: " + (String) t.getTransferData(DataFlavor.stringFlavor));
        }
        catch (UnsupportedFlavorException | IOException e)
        {
            BackgroundService.AppendLog(e);
        }
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {
        java.awt.GridBagConstraints gridBagConstraints;

        appearanceGroup = new javax.swing.ButtonGroup();
        appearanceMenu = new javax.swing.JPopupMenu();
        trayPopup = new javax.swing.JDialog();
        popUpLabel = new javax.swing.JLabel();
        donateDialog = new javax.swing.JDialog();
        donatePanel = new javax.swing.JPanel();
        donateLabel = new javax.swing.JLabel();
        walletsButton = new javax.swing.JButton();
        remindLaterButton = new javax.swing.JButton();
        dismissButton = new javax.swing.JButton();
        hintDialog = new javax.swing.JDialog();
        hintDialogLabel = new javax.swing.JLabel();
        hintDialogPanel = new javax.swing.JPanel();
        notificationPopup = new javax.swing.JDialog();
        notificationPopupLabel = new javax.swing.JLabel();
        notificationPopupPanel = new javax.swing.JPanel();
        mainPanel = new javax.swing.JPanel();
        nodeMonitorPanel = new nodemonitor.MonitorPanel();
        nodeMonitorPanel.Initialise(this);
        notificationsPanel = new nodemonitor.NotificationsPanel();
        notificationsPanel.initialise(dbManager,this);
        appearancePanel = new nodemonitor.AppearancePanel();
        appearancePanel.initialise(dbManager, this);
        tipJarScrollPane = new javax.swing.JScrollPane();
        tipJarScrollPane.getVerticalScrollBar().setUnitIncrement(10);
        tipJarPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        btcField = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        dogeField = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        ltcField = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        qortField = new javax.swing.JTextField();
        jSeparator1 = new javax.swing.JSeparator();
        jSeparator2 = new javax.swing.JSeparator();
        jSeparator3 = new javax.swing.JSeparator();
        clipboardLabel = new javax.swing.JLabel();
        toolbar = new javax.swing.JPanel();
        nodeMonitorButton = new javax.swing.JButton();
        notificationsButton = new javax.swing.JButton();
        styleButton = new javax.swing.JButton();
        donateButton = new javax.swing.JButton();
        exitButton = new javax.swing.JButton();

        appearanceMenu.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseExited(java.awt.event.MouseEvent evt)
            {
                appearanceMenuMouseExited(evt);
            }
        });

        trayPopup.setUndecorated(true);

        popUpLabel.setBackground(new java.awt.Color(204, 202, 202));
        popUpLabel.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        popUpLabel.setForeground(new java.awt.Color(0, 0, 0));
        popUpLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        popUpLabel.setText("<html><div style='text-align: center;'>Node Monitor is running in the background<br/><br/> Double click on the system tray icon to open the UI<br/><br/> To exit the program, click 'Exit' in the menu bar<br/> You can also right click the system tray icon and click 'Exit'</div><html>");
        popUpLabel.setBorder(javax.swing.BorderFactory.createCompoundBorder(new javax.swing.border.LineBorder(new java.awt.Color(49, 0, 0), 4, true), new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED)));
        popUpLabel.setOpaque(true);
        popUpLabel.setPreferredSize(new java.awt.Dimension(380, 120));
        popUpLabel.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseClicked(java.awt.event.MouseEvent evt)
            {
                popUpLabelMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout trayPopupLayout = new javax.swing.GroupLayout(trayPopup.getContentPane());
        trayPopup.getContentPane().setLayout(trayPopupLayout);
        trayPopupLayout.setHorizontalGroup(
            trayPopupLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(popUpLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 417, Short.MAX_VALUE)
        );
        trayPopupLayout.setVerticalGroup(
            trayPopupLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(popUpLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 177, Short.MAX_VALUE)
        );

        donateDialog.setModal(true);
        donateDialog.setUndecorated(true);
        donateDialog.setResizable(false);

        donatePanel.setBorder(javax.swing.BorderFactory.createCompoundBorder(new javax.swing.border.LineBorder(new java.awt.Color(22, 162, 22), 5, true), javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED)));
        java.awt.GridBagLayout donatePanelLayout = new java.awt.GridBagLayout();
        donatePanelLayout.columnWidths = new int[] {0};
        donatePanelLayout.rowHeights = new int[] {0, 12, 0, 12, 0, 12, 0, 12, 0, 12, 0, 12, 0};
        donatePanel.setLayout(donatePanelLayout);

        donateLabel.setFont(new java.awt.Font("Bahnschrift", 0, 14)); // NOI18N
        donateLabel.setText("<html><div style='text-align: center;'>Enjoying Node Monitor?<br/><br/>\n\nPlease consider supporting the creation and maintenance of<br/>\nmore Qortal apps by sending a tip to one of my Qortal wallets.<br/><br/>\n\nYou can find the wallet addresses on the wallets page.</div><html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        donatePanel.add(donateLabel, gridBagConstraints);

        walletsButton.setFont(new java.awt.Font("Bahnschrift", 0, 12)); // NOI18N
        walletsButton.setText("Go to wallets page");
        walletsButton.setPreferredSize(new java.awt.Dimension(150, 45));
        walletsButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                walletsButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        donatePanel.add(walletsButton, gridBagConstraints);

        remindLaterButton.setFont(new java.awt.Font("Bahnschrift", 0, 12)); // NOI18N
        remindLaterButton.setText("Remind me later");
        remindLaterButton.setMinimumSize(new java.awt.Dimension(122, 22));
        remindLaterButton.setPreferredSize(new java.awt.Dimension(150, 45));
        remindLaterButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                remindLaterButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        donatePanel.add(remindLaterButton, gridBagConstraints);

        dismissButton.setFont(new java.awt.Font("Bahnschrift", 0, 12)); // NOI18N
        dismissButton.setText("<html><div style='text-align: center;'>No thanks<br/>Don't show again</div><html>");
        dismissButton.setPreferredSize(new java.awt.Dimension(150, 45));
        dismissButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                dismissButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        donatePanel.add(dismissButton, gridBagConstraints);

        javax.swing.GroupLayout donateDialogLayout = new javax.swing.GroupLayout(donateDialog.getContentPane());
        donateDialog.getContentPane().setLayout(donateDialogLayout);
        donateDialogLayout.setHorizontalGroup(
            donateDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 478, Short.MAX_VALUE)
            .addGroup(donateDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(donatePanel, javax.swing.GroupLayout.DEFAULT_SIZE, 478, Short.MAX_VALUE))
        );
        donateDialogLayout.setVerticalGroup(
            donateDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 448, Short.MAX_VALUE)
            .addGroup(donateDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(donatePanel, javax.swing.GroupLayout.DEFAULT_SIZE, 448, Short.MAX_VALUE))
        );

        hintDialog.setAlwaysOnTop(true);
        hintDialog.setType(java.awt.Window.Type.UTILITY);
        hintDialog.addFocusListener(new java.awt.event.FocusAdapter()
        {
            public void focusLost(java.awt.event.FocusEvent evt)
            {
                hintDialogFocusLost(evt);
            }
        });
        hintDialog.getContentPane().setLayout(new java.awt.GridBagLayout());

        hintDialogLabel.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        hintDialogLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        hintDialogLabel.setText("jLabel4");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.weighty = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(5, 30, 5, 30);
        hintDialog.getContentPane().add(hintDialogLabel, gridBagConstraints);

        hintDialogPanel.setPreferredSize(new java.awt.Dimension(252, 170));

        javax.swing.GroupLayout hintDialogPanelLayout = new javax.swing.GroupLayout(hintDialogPanel);
        hintDialogPanel.setLayout(hintDialogPanelLayout);
        hintDialogPanelLayout.setHorizontalGroup(
            hintDialogPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 458, Short.MAX_VALUE)
        );
        hintDialogPanelLayout.setVerticalGroup(
            hintDialogPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 312, Short.MAX_VALUE)
        );

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.weighty = 0.1;
        hintDialog.getContentPane().add(hintDialogPanel, gridBagConstraints);

        notificationPopup.setUndecorated(true);
        notificationPopup.getContentPane().setLayout(new java.awt.GridBagLayout());

        notificationPopupLabel.setBackground(new java.awt.Color(204, 202, 202));
        notificationPopupLabel.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        notificationPopupLabel.setForeground(new java.awt.Color(0, 0, 0));
        notificationPopupLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        notificationPopupLabel.setText("<html><div style='text-align: center;'>Node Monitor is running in the background<br/><br/> Double click on the system tray icon to open the UI<br/><br/> To exit the program, click 'Exit' in the menu bar<br/> You can also right click the system tray icon and click 'Exit'</div><html>");
        notificationPopupLabel.setOpaque(true);
        notificationPopupLabel.setPreferredSize(new java.awt.Dimension(380, 120));
        notificationPopupLabel.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseClicked(java.awt.event.MouseEvent evt)
            {
                notificationPopupLabelMouseClicked(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(10, 20, 10, 20);
        notificationPopup.getContentPane().add(notificationPopupLabel, gridBagConstraints);

        javax.swing.GroupLayout notificationPopupPanelLayout = new javax.swing.GroupLayout(notificationPopupPanel);
        notificationPopupPanel.setLayout(notificationPopupPanelLayout);
        notificationPopupPanelLayout.setHorizontalGroup(
            notificationPopupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );
        notificationPopupPanelLayout.setVerticalGroup(
            notificationPopupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        notificationPopup.getContentPane().add(notificationPopupPanel, gridBagConstraints);

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(500, 600));
        setPreferredSize(new java.awt.Dimension(600, 600));
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                windowHandler(evt);
            }
        });
        getContentPane().setLayout(new java.awt.GridBagLayout());

        mainPanel.setMinimumSize(new java.awt.Dimension(0, 62));
        mainPanel.addComponentListener(new java.awt.event.ComponentAdapter()
        {
            public void componentResized(java.awt.event.ComponentEvent evt)
            {
                mainPanelComponentResized(evt);
            }
        });
        mainPanel.setLayout(new java.awt.CardLayout());

        nodeMonitorPanel.setMinimumSize(new java.awt.Dimension(0, 0));
        mainPanel.add(nodeMonitorPanel, "monitorPanel");
        mainPanel.add(notificationsPanel, "notificationsPanel");
        mainPanel.add(appearancePanel, "stylePanel");

        tipJarPanel.setLayout(new java.awt.GridBagLayout());

        jLabel1.setFont(new java.awt.Font("Bahnschrift", 1, 18)); // NOI18N
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("Leave a tip for the developer");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 20, 0);
        tipJarPanel.add(jLabel1, gridBagConstraints);

        jLabel2.setFont(new java.awt.Font("Bahnschrift", 0, 14)); // NOI18N
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText("Bitcoin");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 14;
        tipJarPanel.add(jLabel2, gridBagConstraints);

        btcField.setEditable(false);
        btcField.setFont(new java.awt.Font("Bahnschrift", 0, 14)); // NOI18N
        btcField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        btcField.setText("1HiZ8Msb6ps2E7jV4c7F2Du8XaVwaM4vsk");
        btcField.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseReleased(java.awt.event.MouseEvent evt)
            {
                btcFieldMouseReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.ipadx = 150;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
        tipJarPanel.add(btcField, gridBagConstraints);

        jLabel3.setFont(new java.awt.Font("Bahnschrift", 0, 14)); // NOI18N
        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel3.setText("Dogecoin");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        tipJarPanel.add(jLabel3, gridBagConstraints);

        dogeField.setEditable(false);
        dogeField.setFont(new java.awt.Font("Bahnschrift", 0, 14)); // NOI18N
        dogeField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        dogeField.setText("DDJFZx2V4QR6NSCicnEAVjx4kFDTDgNpKG");
        dogeField.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseReleased(java.awt.event.MouseEvent evt)
            {
                dogeFieldMouseReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.ipadx = 150;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
        tipJarPanel.add(dogeField, gridBagConstraints);

        jLabel4.setFont(new java.awt.Font("Bahnschrift", 0, 14)); // NOI18N
        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel4.setText("Litecoin");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        tipJarPanel.add(jLabel4, gridBagConstraints);

        ltcField.setEditable(false);
        ltcField.setFont(new java.awt.Font("Bahnschrift", 0, 14)); // NOI18N
        ltcField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        ltcField.setText("LUZyf6JjERiqCbAhFCiXPP3ydngUSsYUXX");
        ltcField.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseReleased(java.awt.event.MouseEvent evt)
            {
                ltcFieldMouseReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.ipadx = 150;
        gridBagConstraints.insets = new java.awt.Insets(11, 0, 11, 0);
        tipJarPanel.add(ltcField, gridBagConstraints);

        jLabel5.setFont(new java.awt.Font("Bahnschrift", 0, 14)); // NOI18N
        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel5.setText("QORT");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        tipJarPanel.add(jLabel5, gridBagConstraints);

        qortField.setEditable(false);
        qortField.setFont(new java.awt.Font("Bahnschrift", 0, 14)); // NOI18N
        qortField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        qortField.setText("QMErmoE4HooVsM6WF6HKrZQ6DtBTrwGcyn");
        qortField.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseReleased(java.awt.event.MouseEvent evt)
            {
                qortFieldMouseReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.ipadx = 150;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
        tipJarPanel.add(qortField, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(11, 0, 10, 0);
        tipJarPanel.add(jSeparator1, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
        tipJarPanel.add(jSeparator2, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
        tipJarPanel.add(jSeparator3, gridBagConstraints);

        clipboardLabel.setFont(new java.awt.Font("Bahnschrift", 0, 14)); // NOI18N
        clipboardLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        clipboardLabel.setText("Click on an address to copy it to your clipboard");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 20, 0);
        tipJarPanel.add(clipboardLabel, gridBagConstraints);

        tipJarScrollPane.setViewportView(tipJarPanel);

        mainPanel.add(tipJarScrollPane, "tipJarPanel");

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.weighty = 0.1;
        getContentPane().add(mainPanel, gridBagConstraints);

        toolbar.setMaximumSize(new java.awt.Dimension(0, 55));
        toolbar.setMinimumSize(new java.awt.Dimension(0, 55));
        toolbar.setPreferredSize(new java.awt.Dimension(0, 55));
        toolbar.setLayout(new javax.swing.BoxLayout(toolbar, javax.swing.BoxLayout.LINE_AXIS));

        nodeMonitorButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/monitor.png"))); // NOI18N
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("i18n/Language"); // NOI18N
        nodeMonitorButton.setText(bundle.getString("nodeMonitorButton")); // NOI18N
        nodeMonitorButton.setToolTipText("Current info on you node's status");
        nodeMonitorButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        nodeMonitorButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        nodeMonitorButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                nodeMonitorButtonActionPerformed(evt);
            }
        });
        toolbar.add(nodeMonitorButton);

        notificationsButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/alerts.png"))); // NOI18N
        notificationsButton.setText("Notifications");
        notificationsButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        notificationsButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        notificationsButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                notificationsButtonActionPerformed(evt);
            }
        });
        toolbar.add(notificationsButton);

        styleButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/Appearance.png"))); // NOI18N
        styleButton.setText("Appearance");
        styleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        styleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        styleButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                styleButtonActionPerformed(evt);
            }
        });
        toolbar.add(styleButton);

        donateButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/donate.png"))); // NOI18N
        donateButton.setText(bundle.getString("donateButton")); // NOI18N
        donateButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        donateButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        donateButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                donateButtonActionPerformed(evt);
            }
        });
        toolbar.add(donateButton);

        exitButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/exit.png"))); // NOI18N
        exitButton.setText(bundle.getString("exitButton")); // NOI18N
        exitButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        exitButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        exitButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                exitButtonActionPerformed(evt);
            }
        });
        toolbar.add(exitButton);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        getContentPane().add(toolbar, gridBagConstraints);

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void windowHandler(java.awt.event.WindowEvent evt)//GEN-FIRST:event_windowHandler
    {//GEN-HEADEREND:event_windowHandler
        exitInitiated(true);        
    }//GEN-LAST:event_windowHandler

    private void appearanceMenuMouseExited(java.awt.event.MouseEvent evt)//GEN-FIRST:event_appearanceMenuMouseExited
    {//GEN-HEADEREND:event_appearanceMenuMouseExited
        appearanceMenu.setVisible(false);
    }//GEN-LAST:event_appearanceMenuMouseExited

    private void popUpLabelMouseClicked(java.awt.event.MouseEvent evt)//GEN-FIRST:event_popUpLabelMouseClicked
    {//GEN-HEADEREND:event_popUpLabelMouseClicked
        backgroundService.SetGUIEnabled(true);
    }//GEN-LAST:event_popUpLabelMouseClicked

    private void walletsButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_walletsButtonActionPerformed
    {//GEN-HEADEREND:event_walletsButtonActionPerformed
        donateDialog.setVisible(false);
        CardLayout card = (CardLayout) mainPanel.getLayout();
        currentCard = "tipJarPanel";
        card.show(mainPanel, currentCard);
    }//GEN-LAST:event_walletsButtonActionPerformed

    private void remindLaterButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_remindLaterButtonActionPerformed
    {//GEN-HEADEREND:event_remindLaterButtonActionPerformed
        donateDialog.setVisible(false);
    }//GEN-LAST:event_remindLaterButtonActionPerformed

    private void dismissButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_dismissButtonActionPerformed
    {//GEN-HEADEREND:event_dismissButtonActionPerformed
        donateDialog.setVisible(false);
        File settingsFile = new File(System.getProperty("user.dir") + "/bin/settings.json");
        //Faiil safe: exists check should be redundant, this function is called from a dialog that is only shown if json file exists
        if(settingsFile.exists())
        {
            try
            {
                String jsonString = Files.readString(settingsFile.toPath());
                if(jsonString != null)
                {
                    JSONObject jsonObject = new JSONObject(jsonString);
                    jsonObject.put("donateDismissed", "true");   
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
    }//GEN-LAST:event_dismissButtonActionPerformed

    private void qortFieldMouseReleased(java.awt.event.MouseEvent evt)//GEN-FIRST:event_qortFieldMouseReleased
    {//GEN-HEADEREND:event_qortFieldMouseReleased
        Utilities.copyToClipboard(qortField.getText());
        pasteToLabel("QORT");
    }//GEN-LAST:event_qortFieldMouseReleased

    private void ltcFieldMouseReleased(java.awt.event.MouseEvent evt)//GEN-FIRST:event_ltcFieldMouseReleased
    {//GEN-HEADEREND:event_ltcFieldMouseReleased
        Utilities.copyToClipboard(ltcField.getText());
        pasteToLabel("Litecoin");
    }//GEN-LAST:event_ltcFieldMouseReleased

    private void dogeFieldMouseReleased(java.awt.event.MouseEvent evt)//GEN-FIRST:event_dogeFieldMouseReleased
    {//GEN-HEADEREND:event_dogeFieldMouseReleased
        Utilities.copyToClipboard(dogeField.getText());
        pasteToLabel("Dogecoin");
    }//GEN-LAST:event_dogeFieldMouseReleased

    private void btcFieldMouseReleased(java.awt.event.MouseEvent evt)//GEN-FIRST:event_btcFieldMouseReleased
    {//GEN-HEADEREND:event_btcFieldMouseReleased
        Utilities.copyToClipboard(btcField.getText());
        pasteToLabel("Bitcoin");
    }//GEN-LAST:event_btcFieldMouseReleased

    private void exitButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_exitButtonActionPerformed
    {//GEN-HEADEREND:event_exitButtonActionPerformed
        exitInitiated(false);
    }//GEN-LAST:event_exitButtonActionPerformed

    private void donateButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_donateButtonActionPerformed
    {//GEN-HEADEREND:event_donateButtonActionPerformed
        clipboardLabel.setText("Click on an address to copy it to your clipboard");

        CardLayout card = (CardLayout) mainPanel.getLayout();
        currentCard = "tipJarPanel";
        card.show(mainPanel, currentCard);
        if (nodeMonitorPanel.timer != null)
        nodeMonitorPanel.timer.cancel();
    }//GEN-LAST:event_donateButtonActionPerformed

    private void styleButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_styleButtonActionPerformed
    {//GEN-HEADEREND:event_styleButtonActionPerformed
        CardLayout card = (CardLayout) mainPanel.getLayout();
        currentCard = "stylePanel";
        card.show(mainPanel, currentCard);
        if (nodeMonitorPanel.timer != null)
        nodeMonitorPanel.timer.cancel();
    }//GEN-LAST:event_styleButtonActionPerformed

    private void nodeMonitorButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_nodeMonitorButtonActionPerformed
    {//GEN-HEADEREND:event_nodeMonitorButtonActionPerformed
        nodeMonitorPanel.isSynced = true; //first ping this flag must be true to activate time approximation
        CardLayout card = (CardLayout) mainPanel.getLayout();
        //We only need to run the GUI timer if monitorPanel is selected/in focus
        if (!currentCard.equals("monitorPanel"))
            nodeMonitorPanel.RestartTimer();

        currentCard = "monitorPanel";
        card.show(mainPanel, currentCard);
        if(nodeMonitorPanel.startTime == 0)
            nodeMonitorPanel.startTime = System.currentTimeMillis();
    }//GEN-LAST:event_nodeMonitorButtonActionPerformed

    private void notificationsButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_notificationsButtonActionPerformed
    {//GEN-HEADEREND:event_notificationsButtonActionPerformed
        //don't swith tabs on notifications button click on first click
        if(currentCard.equals("notificationsPanel"))
            notificationsPanel.switchTab();
        
        CardLayout card = (CardLayout) mainPanel.getLayout();
        currentCard = "notificationsPanel";
        card.show(mainPanel, currentCard);
        if (nodeMonitorPanel.timer != null)
            nodeMonitorPanel.timer.cancel();
    }//GEN-LAST:event_notificationsButtonActionPerformed

    private void hintDialogFocusLost(java.awt.event.FocusEvent evt)//GEN-FIRST:event_hintDialogFocusLost
    {//GEN-HEADEREND:event_hintDialogFocusLost
        if(evt.getOppositeComponent() instanceof  JDialog)
            return;

        hintDialog.setVisible(false);
    }//GEN-LAST:event_hintDialogFocusLost

    private void notificationPopupLabelMouseClicked(java.awt.event.MouseEvent evt)//GEN-FIRST:event_notificationPopupLabelMouseClicked
    {//GEN-HEADEREND:event_notificationPopupLabelMouseClicked
        backgroundService.SetGUIEnabled(true);       
    }//GEN-LAST:event_notificationPopupLabelMouseClicked

    private void mainPanelComponentResized(java.awt.event.ComponentEvent evt)//GEN-FIRST:event_mainPanelComponentResized
    {//GEN-HEADEREND:event_mainPanelComponentResized
        if(!initComplete)
            return;
        
        int state = getExtendedState();
        if(state == JFrame.MAXIMIZED_BOTH || state == JFrame.MAXIMIZED_HORIZ || state == JFrame.MAXIMIZED_VERT)
            return;
        
        Utilities.updateSetting("frameWidth", String.valueOf(getWidth()), "settings.json");
        Utilities.updateSetting("frameHeight", String.valueOf(getHeight()), "settings.json");   
    }//GEN-LAST:event_mainPanelComponentResized


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup appearanceGroup;
    protected javax.swing.JPopupMenu appearanceMenu;
    protected nodemonitor.AppearancePanel appearancePanel;
    private javax.swing.JTextField btcField;
    private javax.swing.JLabel clipboardLabel;
    private javax.swing.JButton dismissButton;
    private javax.swing.JTextField dogeField;
    private javax.swing.JButton donateButton;
    private javax.swing.JDialog donateDialog;
    private javax.swing.JLabel donateLabel;
    private javax.swing.JPanel donatePanel;
    private javax.swing.JButton exitButton;
    protected javax.swing.JDialog hintDialog;
    private javax.swing.JLabel hintDialogLabel;
    private javax.swing.JPanel hintDialogPanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JTextField ltcField;
    protected javax.swing.JPanel mainPanel;
    private javax.swing.JButton nodeMonitorButton;
    protected nodemonitor.MonitorPanel nodeMonitorPanel;
    public javax.swing.JDialog notificationPopup;
    private javax.swing.JLabel notificationPopupLabel;
    private javax.swing.JPanel notificationPopupPanel;
    private javax.swing.JButton notificationsButton;
    protected nodemonitor.NotificationsPanel notificationsPanel;
    private javax.swing.JLabel popUpLabel;
    private javax.swing.JTextField qortField;
    private javax.swing.JButton remindLaterButton;
    private javax.swing.JButton styleButton;
    protected javax.swing.JPanel tipJarPanel;
    private javax.swing.JScrollPane tipJarScrollPane;
    protected javax.swing.JPanel toolbar;
    public javax.swing.JDialog trayPopup;
    private javax.swing.JButton walletsButton;
    // End of variables declaration//GEN-END:variables

        
}//end class GUI




