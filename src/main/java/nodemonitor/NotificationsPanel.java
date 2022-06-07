package nodemonitor;

import enums.Folders;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.io.File;
import java.sql.Connection;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;

public class NotificationsPanel extends javax.swing.JPanel
{
    private DatabaseManager dbManager;
    private GUI gui;
    private String appPassword = "";
    private Notifier notifier;
    
    public NotificationsPanel()
    {
        initComponents();
    }
    
    protected void initialise(DatabaseManager dbManager,GUI gui)
    {
        this.dbManager = dbManager;
        this.gui = gui;
        notifier = new Notifier(dbManager,gui,false);
        setEmailPanel();
        notifier.start();
        refreshNotifications();   
        initListeners();     
        getSettings();
    }
    
    private void initListeners()
    {   
        //Mouse listener not needed (no double click functionality implemented)
        notificationsTable.getSelectionModel().addListSelectionListener((ListSelectionEvent event) ->
        {
            if(event.getValueIsAdjusting())
                return;
            
            notificationsTableSelected();
        });        
    }
    
    private void notificationsTableSelected()
    {

        if(notificationsTable.getSelectedRow() < 0)
            return;
        
        try (Connection c = ConnectionDB.getConnection("notifications", Folders.DB.get()))
        {
            if (dbManager.TableExists("notifications", c))
            {
                String date = (String) notificationsTable.getValueAt(notificationsTable.getSelectedRow(), 0);

                long timestamp = Utilities.dateFormatToTimestamp(date);

                String message = (String) dbManager.GetItemValue("notifications", "message", "timestamp", String.valueOf(timestamp), c);
                textArea.setText(message);
            }
        }
        catch (Exception e)
        {
            BackgroundService.AppendLog(e);
        }
    }
    
    private void fillSettingsFields()
    {
        File file = new File(System.getProperty("user.dir") + "/databases/mail_settings.mv.db");
        if(!file.exists())
            return;
        
        if(ConnectionDB.CanConnect("mail_settings", "node_monitor", appPassword.toCharArray(), Folders.DB.get()))
        {
            try(Connection connection = ConnectionDB.getConnection("mail_settings", "node_monitor", appPassword.toCharArray(), Folders.DB.get()))
            {
                recipientInput.setText((String)dbManager.GetFirstItem("mail_server", "recipient", connection));
                smtpServerInput.setText((String)dbManager.GetFirstItem("mail_server", "smtp", connection));
                portInput.setText((String)dbManager.GetFirstItem("mail_server", "port", connection));
                usernameInput.setText((String)dbManager.GetFirstItem("mail_server", "username", connection));
            }
            catch (Exception e)
            {
                BackgroundService.AppendLog(e);
            }
        }     
    }
    
    protected void switchTab()
    {
        if(tabbedPane.getSelectedIndex() == 0)
            tabbedPane.setSelectedIndex(1);
        else
            tabbedPane.setSelectedIndex(0);
    }
    
    protected void refreshNotifications()
    {
        try(Connection c = ConnectionDB.getConnection("notifications", Folders.DB.get()))
        {
            if(dbManager.TableExists("notifications", c))
            {
                dbManager.FillJTableOrder("notifications", "timestamp", "desc", notificationsTable, c);
                notificationsTable.revalidate();
                notificationsTable.repaint();
            }
        }
        catch (Exception e)
        {
            BackgroundService.AppendLog(e);
        }
    }
    
    private void setEmailPanel()
    {
        if(emailBox.isSelected())
        {
            emailEnabledLabel.setVisible(true);
            
            File file = new File(System.getProperty("user.dir") + "/databases/mail_settings.mv.db");
            if(file.exists())
            {
                if(ConnectionDB.CanConnect("mail_settings", "node_monitor", appPassword.toCharArray(), Folders.DB.get()))
                {
                    loginPanel.setVisible(false);
                    if(!settingsToggleButton.isSelected())
                        setupServerPanel.setVisible(false);
                    emailEnabledLabel.setText("Email notifications are enabled");  
                    
                    //This is the only case in which emails can be sent, the following flag can only be set here.
                    //The json setting can be set to true (it's the user's preference) while the actual sending of 
                    //emails can be disabled if the server settings are not complete/correct.
                    notifier.emailEnabled = true;
                }
                else
                {
                    loginPanel.setVisible(true);
                    if(!settingsToggleButton.isSelected())
                        setupServerPanel.setVisible(false); 
                    emailEnabledLabel.setText(Utilities.AllignCenterHTML("Email notifications are NOT enabled<br/><br/>"
                            + "Setup your mail server or log in to enable them"));      
                    notifier.emailEnabled = false;           
                }
            }
            else
            {
                loginPanel.setVisible(false);
                setupServerPanel.setVisible(true);    
                settingsToggleButton.setSelected(true);
                settingsToggleButton.setText("Hide mail server settings");
                emailEnabledLabel.setText(Utilities.AllignCenterHTML("Email notifications are NOT enabled<br/><br/>"
                            + "Setup your mail server or log in to enable them"));   
                notifier.emailEnabled = false;
            }
            Utilities.updateSetting("emailEnabled", "true","notifications.json");
            
        }
        else
        {
            emailEnabledLabel.setVisible(false);
            
            loginPanel.setVisible(false);
            setupServerPanel.setVisible(false);  
            settingsToggleButton.setSelected(false);   
            settingsToggleButton.setText("Show mail server settings");
            emailEnabledLabel.setText(Utilities.AllignCenterHTML("Email notifications are NOT enabled<br/><br/>"
                            + "Setup your mail server or log in to enable them"));  
            Utilities.updateSetting("emailEnabled", "false","notifications.json");    
            notifier.emailEnabled = false;
        }
    }
    
    protected void updateStyle(Color bgColor, Color cmpColor, Color fontColor, Color cmpFontColor, String fontType)
    {
         //does not work for all L&F (Nimbus / windows)
        tabbedPane.setBackground(bgColor);
        tabbedPane.setFont(new Font(fontType, Font.PLAIN, tabbedPane.getFont().getSize()));
        tabbedPane.setForeground(fontColor);

        notificationsMainPanel.setBackground(bgColor);
        clearAllButton.setBackground(cmpColor);
        clearAllButton.setFont(new Font(fontType, clearAllButton.getFont().getStyle(), clearAllButton.getFont().getSize()));
        clearAllButton.setForeground(cmpFontColor);

        notificationsTable.setBackground(bgColor);
        notificationsTable.setFont(new Font(fontType, notificationsTable.getFont().getStyle(), notificationsTable.getFont().getSize()));
        notificationsTable.setForeground(fontColor);
        notificationsTable.setSelectionBackground(cmpColor);
        notificationsTable.setSelectionForeground(cmpFontColor);
        notificationsTable.setShowGrid(true);
        notificationsTable.setGridColor(fontColor);
        
        textArea.setBackground(bgColor);
        textArea.setFont(new Font(fontType, Font.PLAIN, textArea.getFont().getSize()));
        textArea.setForeground(fontColor);
         
         for(int i = 0; i < tabbedPane.getTabCount(); i++)
         {
            //does not work for all L&F (Nimbus / windows)
             tabbedPane.setBackgroundAt(i, bgColor);
         }
        
        for(Component panel : mainPanel.getComponents())
        {
            if(panel instanceof JPanel)
            {
                panel.setBackground(bgColor);
                
                for(Component c : ((JPanel) panel).getComponents())
                {
                    if (c instanceof JButton || c instanceof JTextField || c instanceof JToggleButton)
                    {
                        c.setBackground(cmpColor);
                        c.setFont(new Font(fontType, c.getFont().getStyle(), c.getFont().getSize()));
                        c.setForeground(cmpFontColor);
                    }
                    if (c instanceof JLabel)
                    {
                        c.setFont(new Font(fontType, c.getFont().getStyle(), c.getFont().getSize()));
                        c.setForeground(fontColor);
                    }
                    if (c instanceof JCheckBox)
                    {
                        c.setBackground(bgColor);
                        c.setFont(new Font(fontType, c.getFont().getStyle(), c.getFont().getSize()));
                        c.setForeground(fontColor);
                    }
                    if(c instanceof  JSlider)
                        c.setBackground(bgColor);
                }
            }
        }
    }
    
    private void getSettings()
    {
        inBackgroundBox.setSelected(notifier.runInBackground);
        inAppBox.setSelected(notifier.inAppEnabled);
        emailBox.setSelected(notifier.emailEnabled);
        syncGainBox.setSelected(notifier.syncGainedEnabled);
        connectionsGainedBox.setSelected(notifier.connectGainedEnabled);
        mintGainedBox.setSelected(notifier.mintGainedEnabled);
        offlineBox.setSelected(notifier.offlineEnabled);
        emailLimitBox.setSelected(notifier.emailLimitEnabled);
        syncSlider.setValue(notifier.syncThreshold);
        connectionsSlider.setValue(notifier.connectThreshold);
        mintSlider.setValue(notifier.mintThreshold);
        emailLimitSlider.setValue(notifier.emailLimit);
        
        onlineBox.setSelected(notifier.onlineEnabled);
        checkToggle(onlineBox, offlineBox, "onlineEnabled");
        
        mintLostBox.setSelected(notifier.mintLostEnabled);   
        checkToggle(mintLostBox, mintGainedBox, "mintGainedEnabled");   
        
        syncLostBox.setSelected(notifier.syncLostEnabled);
        checkToggle(syncLostBox, syncGainBox, "syncGainedEnabled");
        
        connectionsLostBox.setSelected(notifier.connectLostEnabled);
        checkToggle(connectionsLostBox, connectionsGainedBox, "connectGainedEnabled");
    }
    
    private void checkToggle(JCheckBox source, JCheckBox target, String key)
    {        
        if(!source.isSelected())
        {
            target.setEnabled(false);
            target.setSelected(false);
            Utilities.updateSetting(key, "false", "notifications.json");
        }
        target.setEnabled(source.isSelected());
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

        tabbedPane = new javax.swing.JTabbedPane();
        settingsScrollPane = new javax.swing.JScrollPane();
        settingsScrollPane.getVerticalScrollBar().setUnitIncrement(10);
        mainPanel = new javax.swing.JPanel();
        headerPanel = new javax.swing.JPanel();
        emailBox = new javax.swing.JCheckBox();
        emailEnabledLabel = new javax.swing.JLabel();
        settingsToggleButton = new javax.swing.JToggleButton();
        inAppBox = new javax.swing.JCheckBox();
        inBackgroundBox = new javax.swing.JCheckBox();
        settingsPanel = new javax.swing.JPanel();
        syncLostBox = new javax.swing.JCheckBox();
        syncGainBox = new javax.swing.JCheckBox();
        connectionsLostBox = new javax.swing.JCheckBox();
        connectionsGainedBox = new javax.swing.JCheckBox();
        onlineBox = new javax.swing.JCheckBox();
        offlineBox = new javax.swing.JCheckBox();
        syncSlider = new javax.swing.JSlider();
        jLabel1 = new javax.swing.JLabel();
        connectionsSlider = new javax.swing.JSlider();
        jLabel2 = new javax.swing.JLabel();
        jSeparator2 = new javax.swing.JSeparator();
        jSeparator4 = new javax.swing.JSeparator();
        mintSlider = new javax.swing.JSlider();
        jLabel4 = new javax.swing.JLabel();
        mintGainedBox = new javax.swing.JCheckBox();
        mintLostBox = new javax.swing.JCheckBox();
        jSeparator8 = new javax.swing.JSeparator();
        jLabel5 = new javax.swing.JLabel();
        jSeparator9 = new javax.swing.JSeparator();
        jSeparator3 = new javax.swing.JSeparator();
        emailLimitSlider = new javax.swing.JSlider();
        jLabel6 = new javax.swing.JLabel();
        emailLimitBox = new javax.swing.JCheckBox();
        emailLimitHintLabel = new javax.swing.JLabel();
        loginPanel = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        loginPasswordField = new javax.swing.JPasswordField();
        loginButton = new javax.swing.JButton();
        setupServerPanel = new javax.swing.JPanel();
        smtpServerInput = new javax.swing.JTextField();
        portInput = new javax.swing.JTextField();
        mailPasswordField = new javax.swing.JPasswordField();
        usernameInput = new javax.swing.JTextField();
        saveMailServerButton = new javax.swing.JButton();
        setupMailLabel = new javax.swing.JLabel();
        testMailServerButton = new javax.swing.JButton();
        recipientInput = new javax.swing.JTextField();
        recipientLabel = new javax.swing.JLabel();
        smtpLabel = new javax.swing.JLabel();
        portLabel = new javax.swing.JLabel();
        userLabel = new javax.swing.JLabel();
        passwordLabel = new javax.swing.JLabel();
        receivedMailCheckbox = new javax.swing.JCheckBox();
        disclaimerCheckbox = new javax.swing.JCheckBox();
        encryptBox = new javax.swing.JCheckBox();
        appPasswordLabel = new javax.swing.JLabel();
        appPasswordField = new javax.swing.JPasswordField();
        confirmPasswordLabel = new javax.swing.JLabel();
        confirmPasswordField = new javax.swing.JPasswordField();
        passwordHintLabel = new javax.swing.JLabel();
        jSeparator5 = new javax.swing.JSeparator();
        jSeparator6 = new javax.swing.JSeparator();
        jSeparator7 = new javax.swing.JSeparator();
        settingsHintLabel = new javax.swing.JLabel();
        disclaimerLabel = new javax.swing.JLabel();
        notificationsMainPanel = new javax.swing.JPanel();
        clearAllButton = new javax.swing.JButton();
        jSplitPane1 = new javax.swing.JSplitPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        notificationsTable = new javax.swing.JTable();
        notificationsTable.getTableHeader().setReorderingAllowed(false);
        jScrollPane2 = new javax.swing.JScrollPane();
        textArea = new javax.swing.JTextArea();

        tabbedPane.setOpaque(true);

        mainPanel.setLayout(new java.awt.GridBagLayout());

        headerPanel.setLayout(new java.awt.GridBagLayout());

        emailBox.setText("I want to receive E-mail notifications");
        emailBox.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                emailBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 2;
        headerPanel.add(emailBox, gridBagConstraints);

        emailEnabledLabel.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        emailEnabledLabel.setText("Email notifications are NOT enabled. ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 5, 0);
        headerPanel.add(emailEnabledLabel, gridBagConstraints);

        settingsToggleButton.setText("Show mail server settings");
        settingsToggleButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                settingsToggleButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
        headerPanel.add(settingsToggleButton, gridBagConstraints);

        inAppBox.setText("Enable in-app notifications");
        inAppBox.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                inAppBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 10, 0);
        headerPanel.add(inAppBox, gridBagConstraints);

        inBackgroundBox.setText("Run in background");
        inBackgroundBox.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                inBackgroundBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(20, 0, 10, 0);
        headerPanel.add(inBackgroundBox, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.weighty = 0.1;
        mainPanel.add(headerPanel, gridBagConstraints);

        settingsPanel.setLayout(new java.awt.GridBagLayout());

        syncLostBox.setText(" is 15 blocks or more behind the chain height");
        syncLostBox.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                syncLostBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
        settingsPanel.add(syncLostBox, gridBagConstraints);

        syncGainBox.setText("has returned to less than 15 blocks behind the chain height");
        syncGainBox.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                syncGainBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        settingsPanel.add(syncGainBox, gridBagConstraints);

        connectionsLostBox.setText("has less than 5 connections");
        connectionsLostBox.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                connectionsLostBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 21;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        settingsPanel.add(connectionsLostBox, gridBagConstraints);

        connectionsGainedBox.setText("has regained 5 or more connections");
        connectionsGainedBox.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                connectionsGainedBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 22;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        settingsPanel.add(connectionsGainedBox, gridBagConstraints);

        onlineBox.setText("is back online");
        onlineBox.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                onlineBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        settingsPanel.add(onlineBox, gridBagConstraints);

        offlineBox.setText("has gone offline");
        offlineBox.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                offlineBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        settingsPanel.add(offlineBox, gridBagConstraints);

        syncSlider.setMinimum(5);
        syncSlider.setValue(15);
        syncSlider.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                syncSliderStateChanged(evt);
            }
        });
        syncSlider.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseReleased(java.awt.event.MouseEvent evt)
            {
                syncSliderMouseReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        settingsPanel.add(syncSlider, gridBagConstraints);

        jLabel1.setText("Set syncing treshold");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        settingsPanel.add(jLabel1, gridBagConstraints);

        connectionsSlider.setMinimum(1);
        connectionsSlider.setValue(5);
        connectionsSlider.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                connectionsSliderStateChanged(evt);
            }
        });
        connectionsSlider.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseReleased(java.awt.event.MouseEvent evt)
            {
                connectionsSliderMouseReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 24;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 30, 0);
        settingsPanel.add(connectionsSlider, gridBagConstraints);

        jLabel2.setText("Set connections treshold");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 23;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        settingsPanel.add(jLabel2, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
        settingsPanel.add(jSeparator2, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 15, 0);
        settingsPanel.add(jSeparator4, gridBagConstraints);

        mintSlider.setMaximum(120);
        mintSlider.setMinimum(5);
        mintSlider.setValue(15);
        mintSlider.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                mintSliderStateChanged(evt);
            }
        });
        mintSlider.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseReleased(java.awt.event.MouseEvent evt)
            {
                mintSliderMouseReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 19;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        settingsPanel.add(mintSlider, gridBagConstraints);

        jLabel4.setText("Set minting treshold");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 18;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        settingsPanel.add(jLabel4, gridBagConstraints);

        mintGainedBox.setText("has resumed minting");
        mintGainedBox.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                mintGainedBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 17;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        settingsPanel.add(mintGainedBox, gridBagConstraints);

        mintLostBox.setText("has NOT been minting for more than 15 minutes");
        mintLostBox.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                mintLostBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        settingsPanel.add(mintLostBox, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
        settingsPanel.add(jSeparator8, gridBagConstraints);

        jLabel5.setFont(new java.awt.Font("Segoe UI", 1, 16)); // NOI18N
        jLabel5.setText("Notify me when my node...");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 0);
        settingsPanel.add(jLabel5, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 15, 0);
        settingsPanel.add(jSeparator9, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 20;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 15, 0);
        settingsPanel.add(jSeparator3, gridBagConstraints);

        emailLimitSlider.setMinimum(5);
        emailLimitSlider.setValue(10);
        emailLimitSlider.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                emailLimitSliderStateChanged(evt);
            }
        });
        emailLimitSlider.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseReleased(java.awt.event.MouseEvent evt)
            {
                emailLimitSliderMouseReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        settingsPanel.add(emailLimitSlider, gridBagConstraints);

        jLabel6.setText("Set e-mail limit");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        settingsPanel.add(jLabel6, gridBagConstraints);

        emailLimitBox.setText("Limit e-mail notifications to 10 per day");
        emailLimitBox.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                emailLimitBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        settingsPanel.add(emailLimitBox, gridBagConstraints);

        emailLimitHintLabel.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        emailLimitHintLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        emailLimitHintLabel.setText("<html><u>Should I set a limit?</u></html>");
        emailLimitHintLabel.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseReleased(java.awt.event.MouseEvent evt)
            {
                emailLimitHintLabelMouseReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 10, 0);
        settingsPanel.add(emailLimitHintLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.weighty = 0.2;
        mainPanel.add(settingsPanel, gridBagConstraints);

        loginPanel.setLayout(new java.awt.GridBagLayout());

        jLabel3.setText("Please log-in to use e-mail notifications");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 0);
        loginPanel.add(jLabel3, gridBagConstraints);

        loginPasswordField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        loginPasswordField.setMinimumSize(new java.awt.Dimension(175, 30));
        loginPasswordField.setPreferredSize(new java.awt.Dimension(175, 30));
        loginPasswordField.addKeyListener(new java.awt.event.KeyAdapter()
        {
            public void keyReleased(java.awt.event.KeyEvent evt)
            {
                loginPasswordFieldKeyReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 0);
        loginPanel.add(loginPasswordField, gridBagConstraints);

        loginButton.setText("Login");
        loginButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                loginButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 0);
        loginPanel.add(loginButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.weighty = 0.1;
        mainPanel.add(loginPanel, gridBagConstraints);

        setupServerPanel.addFocusListener(new java.awt.event.FocusAdapter()
        {
            public void focusLost(java.awt.event.FocusEvent evt)
            {
                setupServerPanelFocusLost(evt);
            }
        });
        setupServerPanel.setLayout(new java.awt.GridBagLayout());

        smtpServerInput.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        smtpServerInput.setText("smtp server");
        smtpServerInput.setMinimumSize(new java.awt.Dimension(250, 22));
        smtpServerInput.setPreferredSize(new java.awt.Dimension(175, 30));
        smtpServerInput.addFocusListener(new java.awt.event.FocusAdapter()
        {
            public void focusGained(java.awt.event.FocusEvent evt)
            {
                smtpServerInputFocusGained(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
        setupServerPanel.add(smtpServerInput, gridBagConstraints);

        portInput.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        portInput.setText("port number");
        portInput.setMinimumSize(new java.awt.Dimension(250, 22));
        portInput.setPreferredSize(new java.awt.Dimension(175, 30));
        portInput.addFocusListener(new java.awt.event.FocusAdapter()
        {
            public void focusGained(java.awt.event.FocusEvent evt)
            {
                portInputFocusGained(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
        setupServerPanel.add(portInput, gridBagConstraints);

        mailPasswordField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        mailPasswordField.setMinimumSize(new java.awt.Dimension(250, 22));
        mailPasswordField.setPreferredSize(new java.awt.Dimension(175, 30));
        mailPasswordField.addFocusListener(new java.awt.event.FocusAdapter()
        {
            public void focusGained(java.awt.event.FocusEvent evt)
            {
                mailPasswordFieldFocusGained(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
        setupServerPanel.add(mailPasswordField, gridBagConstraints);

        usernameInput.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        usernameInput.setText("username");
        usernameInput.setMinimumSize(new java.awt.Dimension(250, 22));
        usernameInput.setPreferredSize(new java.awt.Dimension(175, 30));
        usernameInput.addFocusListener(new java.awt.event.FocusAdapter()
        {
            public void focusGained(java.awt.event.FocusEvent evt)
            {
                usernameInputFocusGained(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
        setupServerPanel.add(usernameInput, gridBagConstraints);

        saveMailServerButton.setText("Save mail settings");
        saveMailServerButton.setEnabled(false);
        saveMailServerButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                saveMailServerButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 25;
        gridBagConstraints.ipadx = 37;
        gridBagConstraints.insets = new java.awt.Insets(15, 0, 15, 0);
        setupServerPanel.add(saveMailServerButton, gridBagConstraints);

        setupMailLabel.setFont(new java.awt.Font("Segoe UI", 1, 16)); // NOI18N
        setupMailLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        setupMailLabel.setText("<html><u>Setup mail server</u></html>");
        setupMailLabel.setToolTipText("");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 9, 0);
        setupServerPanel.add(setupMailLabel, gridBagConstraints);

        testMailServerButton.setText("Send test mail");
        testMailServerButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                testMailServerButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
        setupServerPanel.add(testMailServerButton, gridBagConstraints);

        recipientInput.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        recipientInput.setText("recipient e-mail address");
        recipientInput.setMinimumSize(new java.awt.Dimension(250, 22));
        recipientInput.setPreferredSize(new java.awt.Dimension(175, 30));
        recipientInput.addFocusListener(new java.awt.event.FocusAdapter()
        {
            public void focusGained(java.awt.event.FocusEvent evt)
            {
                recipientInputFocusGained(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
        setupServerPanel.add(recipientInput, gridBagConstraints);

        recipientLabel.setText("Recipient");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        setupServerPanel.add(recipientLabel, gridBagConstraints);

        smtpLabel.setText("SMTP server (gmail not supported)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        setupServerPanel.add(smtpLabel, gridBagConstraints);

        portLabel.setText("Port");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        setupServerPanel.add(portLabel, gridBagConstraints);

        userLabel.setText("Mail server username");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        setupServerPanel.add(userLabel, gridBagConstraints);

        passwordLabel.setText("Mail server password");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        setupServerPanel.add(passwordLabel, gridBagConstraints);

        receivedMailCheckbox.setText("I have received the test e-mail");
        receivedMailCheckbox.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                receivedMailCheckboxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 22;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 0);
        setupServerPanel.add(receivedMailCheckbox, gridBagConstraints);

        disclaimerCheckbox.setText("I have read and understood the disclaimer");
        disclaimerCheckbox.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                disclaimerCheckboxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 23;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 0);
        setupServerPanel.add(disclaimerCheckbox, gridBagConstraints);

        encryptBox.setSelected(true);
        encryptBox.setText("Encrypt mail settings");
        encryptBox.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                encryptBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 5, 0);
        setupServerPanel.add(encryptBox, gridBagConstraints);

        appPasswordLabel.setText("App password");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 17;
        gridBagConstraints.insets = new java.awt.Insets(15, 0, 0, 0);
        setupServerPanel.add(appPasswordLabel, gridBagConstraints);

        appPasswordField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        appPasswordField.setMinimumSize(new java.awt.Dimension(250, 22));
        appPasswordField.setPreferredSize(new java.awt.Dimension(175, 30));
        appPasswordField.addFocusListener(new java.awt.event.FocusAdapter()
        {
            public void focusGained(java.awt.event.FocusEvent evt)
            {
                appPasswordFieldFocusGained(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 18;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 10, 0);
        setupServerPanel.add(appPasswordField, gridBagConstraints);

        confirmPasswordLabel.setText("Confirm app password");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 19;
        setupServerPanel.add(confirmPasswordLabel, gridBagConstraints);

        confirmPasswordField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        confirmPasswordField.setMinimumSize(new java.awt.Dimension(250, 22));
        confirmPasswordField.setPreferredSize(new java.awt.Dimension(175, 30));
        confirmPasswordField.addFocusListener(new java.awt.event.FocusAdapter()
        {
            public void focusGained(java.awt.event.FocusEvent evt)
            {
                confirmPasswordFieldFocusGained(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 20;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 10, 0);
        setupServerPanel.add(confirmPasswordField, gridBagConstraints);

        passwordHintLabel.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        passwordHintLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        passwordHintLabel.setText("<html><u>Should I encrypt my mail settings?</u></html>");
        passwordHintLabel.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseReleased(java.awt.event.MouseEvent evt)
            {
                passwordHintLabelMouseReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 10, 0);
        setupServerPanel.add(passwordHintLabel, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        setupServerPanel.add(jSeparator5, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 26;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 35, 0);
        setupServerPanel.add(jSeparator6, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 21;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        setupServerPanel.add(jSeparator7, gridBagConstraints);

        settingsHintLabel.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        settingsHintLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        settingsHintLabel.setText("<html><u>What are SMTP setting?</u></html>");
        settingsHintLabel.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseReleased(java.awt.event.MouseEvent evt)
            {
                settingsHintLabelMouseReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 10, 0);
        setupServerPanel.add(settingsHintLabel, gridBagConstraints);

        disclaimerLabel.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        disclaimerLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        disclaimerLabel.setText("<html><u>DISCLAIMER</u></html>");
        disclaimerLabel.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseReleased(java.awt.event.MouseEvent evt)
            {
                disclaimerLabelMouseReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 24;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 10, 0);
        setupServerPanel.add(disclaimerLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.weighty = 0.1;
        mainPanel.add(setupServerPanel, gridBagConstraints);

        settingsScrollPane.setViewportView(mainPanel);

        tabbedPane.addTab("Settings", settingsScrollPane);

        notificationsMainPanel.setLayout(new java.awt.GridBagLayout());

        clearAllButton.setText("Clear all");
        clearAllButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                clearAllButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
        notificationsMainPanel.add(clearAllButton, gridBagConstraints);

        jSplitPane1.setDividerLocation(250);
        jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        notificationsTable.setFillsViewportHeight(true);
        jScrollPane1.setViewportView(notificationsTable);

        jSplitPane1.setLeftComponent(jScrollPane1);

        textArea.setEditable(false);
        textArea.setColumns(20);
        textArea.setLineWrap(true);
        textArea.setRows(5);
        textArea.setWrapStyleWord(true);
        textArea.setMargin(new java.awt.Insets(10, 20, 2, 20));
        jScrollPane2.setViewportView(textArea);

        jSplitPane1.setRightComponent(jScrollPane2);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.weighty = 0.1;
        notificationsMainPanel.add(jSplitPane1, gridBagConstraints);

        tabbedPane.addTab("Notifications", notificationsMainPanel);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(tabbedPane, javax.swing.GroupLayout.Alignment.TRAILING)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(tabbedPane)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void smtpServerInputFocusGained(java.awt.event.FocusEvent evt)//GEN-FIRST:event_smtpServerInputFocusGained
    {//GEN-HEADEREND:event_smtpServerInputFocusGained
        smtpServerInput.selectAll();
    }//GEN-LAST:event_smtpServerInputFocusGained

    private void portInputFocusGained(java.awt.event.FocusEvent evt)//GEN-FIRST:event_portInputFocusGained
    {//GEN-HEADEREND:event_portInputFocusGained
        portInput.selectAll();
    }//GEN-LAST:event_portInputFocusGained

    private void mailPasswordFieldFocusGained(java.awt.event.FocusEvent evt)//GEN-FIRST:event_mailPasswordFieldFocusGained
    {//GEN-HEADEREND:event_mailPasswordFieldFocusGained
        mailPasswordField.selectAll();
    }//GEN-LAST:event_mailPasswordFieldFocusGained

    private void usernameInputFocusGained(java.awt.event.FocusEvent evt)//GEN-FIRST:event_usernameInputFocusGained
    {//GEN-HEADEREND:event_usernameInputFocusGained
        usernameInput.selectAll();
    }//GEN-LAST:event_usernameInputFocusGained

    private void saveMailServerButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_saveMailServerButtonActionPerformed
    {//GEN-HEADEREND:event_saveMailServerButtonActionPerformed
       if(recipientInput.getText().isBlank() || smtpServerInput.getText().isBlank() || portInput.getText().isBlank() || 
               usernameInput.getText().isBlank() || String.valueOf(mailPasswordField.getPassword()).isBlank())
       {
           JOptionPane.showMessageDialog(this, "Please fill all input fields.");
           return;
       }
        
        if(encryptBox.isSelected() && !String.valueOf(appPasswordField.getPassword()).equals(String.valueOf(confirmPasswordField.getPassword())))
        {
            JOptionPane.showMessageDialog(this, "App passwords don't match. Please try again");
            appPasswordField.requestFocus();
            appPasswordField.selectAll();
            return;
        }
        
        char[] password = encryptBox.isSelected() ? appPasswordField.getPassword() : "".toCharArray();

        String[] keys = Utilities.EncryptPassword(mailPasswordField.getPassword(), "", "");
        if (keys != null)
        {
            notifier.saveCredentials(usernameInput.getText(),
                keys[0],
                smtpServerInput.getText(), portInput.getText(),
                recipientInput.getText(), 
                keys[1], 
                keys[2],
                password);
            
            JOptionPane.showMessageDialog(this, "Settings saved");
            loginPanel.setVisible(false);
            setupServerPanel.setVisible(false);
            settingsToggleButton.setSelected(false);
            settingsToggleButton.setText("Show mail server settings");
            emailEnabledLabel.setText("Email notifications are enabled");  
            appPassword = String.valueOf(password);
            notifier.setAppPw(appPassword);
        }
        else
            JOptionPane.showMessageDialog(this, "Error while encrypting password",
                "Encryption error", JOptionPane.ERROR_MESSAGE);
    }//GEN-LAST:event_saveMailServerButtonActionPerformed

    private void testMailServerButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_testMailServerButtonActionPerformed
    {//GEN-HEADEREND:event_testMailServerButtonActionPerformed
         if(recipientInput.getText().isBlank() || smtpServerInput.getText().isBlank() || portInput.getText().isBlank() || 
               usernameInput.getText().isBlank() || String.valueOf(mailPasswordField.getPassword()).isBlank())
       {
           JOptionPane.showMessageDialog(this, "Please fill all input fields.");
           return;
       }
        
        testMailServerButton.setEnabled(false);
        Timer buttonTimer = new Timer();
        TimerTask task = new TimerTask()
        {
            @Override
            public void run()
            {
                testMailServerButton.setEnabled(true);
            }
        };
        buttonTimer.schedule(task, 8000);

        if (Utilities.SendEmail(
            recipientInput.getText(),
            usernameInput.getText(),
            String.copyValueOf(mailPasswordField.getPassword()),
            smtpServerInput.getText(),
            portInput.getText(),
            "Node Monitor test mail",
            "Congratulations, your Node Monitor mail notification settings are working properly."))
    {
        JOptionPane.showMessageDialog(this,
            Utilities.AllignCenterHTML("E-mail sent succesfully. You can now save these settings."),
            "Success", JOptionPane.PLAIN_MESSAGE);
    }
    else
    {
        JOptionPane.showMessageDialog(this,
            Utilities.AllignCenterHTML("Failed to send test mail, are your settings correct?"),
            "Failed to send e-mail", JOptionPane.PLAIN_MESSAGE);
    }
    }//GEN-LAST:event_testMailServerButtonActionPerformed

    private void recipientInputFocusGained(java.awt.event.FocusEvent evt)//GEN-FIRST:event_recipientInputFocusGained
    {//GEN-HEADEREND:event_recipientInputFocusGained
        recipientInput.selectAll();
    }//GEN-LAST:event_recipientInputFocusGained

    private void receivedMailCheckboxActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_receivedMailCheckboxActionPerformed
    {//GEN-HEADEREND:event_receivedMailCheckboxActionPerformed
        saveMailServerButton.setEnabled(receivedMailCheckbox.isSelected() && disclaimerCheckbox.isSelected());
    }//GEN-LAST:event_receivedMailCheckboxActionPerformed

    private void setupServerPanelFocusLost(java.awt.event.FocusEvent evt)//GEN-FIRST:event_setupServerPanelFocusLost
    {//GEN-HEADEREND:event_setupServerPanelFocusLost
        mailPasswordField.setText("");
    }//GEN-LAST:event_setupServerPanelFocusLost

    private void disclaimerCheckboxActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_disclaimerCheckboxActionPerformed
    {//GEN-HEADEREND:event_disclaimerCheckboxActionPerformed
        saveMailServerButton.setEnabled(receivedMailCheckbox.isSelected() && disclaimerCheckbox.isSelected());
    }//GEN-LAST:event_disclaimerCheckboxActionPerformed

    private void confirmPasswordFieldFocusGained(java.awt.event.FocusEvent evt)//GEN-FIRST:event_confirmPasswordFieldFocusGained
    {//GEN-HEADEREND:event_confirmPasswordFieldFocusGained
        confirmPasswordField.selectAll();
    }//GEN-LAST:event_confirmPasswordFieldFocusGained

    private void appPasswordFieldFocusGained(java.awt.event.FocusEvent evt)//GEN-FIRST:event_appPasswordFieldFocusGained
    {//GEN-HEADEREND:event_appPasswordFieldFocusGained
        appPasswordField.selectAll();
    }//GEN-LAST:event_appPasswordFieldFocusGained

    private void emailBoxActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_emailBoxActionPerformed
    {//GEN-HEADEREND:event_emailBoxActionPerformed
        setEmailPanel();
    }//GEN-LAST:event_emailBoxActionPerformed

    private void syncLostBoxActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_syncLostBoxActionPerformed
    {//GEN-HEADEREND:event_syncLostBoxActionPerformed
        notifier.syncLostEnabled = syncLostBox.isSelected();
        Utilities.updateSetting("syncLostEnabled", String.valueOf(syncLostBox.isSelected()),"notifications.json");        
        checkToggle(syncLostBox, syncGainBox, "syncGainedEnabled");
    }//GEN-LAST:event_syncLostBoxActionPerformed

    private void syncGainBoxActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_syncGainBoxActionPerformed
    {//GEN-HEADEREND:event_syncGainBoxActionPerformed
        notifier.syncGainedEnabled = syncGainBox.isSelected();
        Utilities.updateSetting("syncGainedEnabled", String.valueOf(syncGainBox.isSelected()),"notifications.json");
    }//GEN-LAST:event_syncGainBoxActionPerformed

    private void connectionsLostBoxActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_connectionsLostBoxActionPerformed
    {//GEN-HEADEREND:event_connectionsLostBoxActionPerformed
        notifier.connectLostEnabled = connectionsLostBox.isSelected();
        Utilities.updateSetting("connectLostEnabled", String.valueOf(connectionsLostBox.isSelected()),"notifications.json");        
        checkToggle(connectionsLostBox, connectionsGainedBox, "connectGainedEnabled");
    }//GEN-LAST:event_connectionsLostBoxActionPerformed

    private void connectionsGainedBoxActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_connectionsGainedBoxActionPerformed
    {//GEN-HEADEREND:event_connectionsGainedBoxActionPerformed
        notifier.connectGainedEnabled = connectionsGainedBox.isSelected();
        Utilities.updateSetting("connectGainedEnabled", String.valueOf(connectionsGainedBox.isSelected()),"notifications.json");
    }//GEN-LAST:event_connectionsGainedBoxActionPerformed

    private void onlineBoxActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_onlineBoxActionPerformed
    {//GEN-HEADEREND:event_onlineBoxActionPerformed
        notifier.onlineEnabled = onlineBox.isSelected();
        Utilities.updateSetting("onlineEnabled", String.valueOf(onlineBox.isSelected()),"notifications.json");
    }//GEN-LAST:event_onlineBoxActionPerformed

    private void offlineBoxActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_offlineBoxActionPerformed
    {//GEN-HEADEREND:event_offlineBoxActionPerformed
        notifier.offlineEnabled = offlineBox.isSelected();
        Utilities.updateSetting("offlineEnabled", String.valueOf(offlineBox.isSelected()),"notifications.json");
        checkToggle(offlineBox, onlineBox, "onlineEnabled");
    }//GEN-LAST:event_offlineBoxActionPerformed

    private void syncSliderStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_syncSliderStateChanged
    {//GEN-HEADEREND:event_syncSliderStateChanged
        syncLostBox.setText("is " + syncSlider.getValue() + " blocks or more behind the chain height");
        syncGainBox.setText("has returned to less than " + syncSlider.getValue() + " blocks behind the chain height");
    }//GEN-LAST:event_syncSliderStateChanged

    private void syncSliderMouseReleased(java.awt.event.MouseEvent evt)//GEN-FIRST:event_syncSliderMouseReleased
    {//GEN-HEADEREND:event_syncSliderMouseReleased
        notifier.syncThreshold = syncSlider.getValue();
        Utilities.updateSetting("syncThreshold", String.valueOf(syncSlider.getValue()),"notifications.json");
    }//GEN-LAST:event_syncSliderMouseReleased

    private void connectionsSliderStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_connectionsSliderStateChanged
    {//GEN-HEADEREND:event_connectionsSliderStateChanged
        connectionsLostBox.setText("has less than " + connectionsSlider.getValue() + " connections");     
        connectionsGainedBox.setText("has regained " + connectionsSlider.getValue() + " or more connections");
    }//GEN-LAST:event_connectionsSliderStateChanged

    private void connectionsSliderMouseReleased(java.awt.event.MouseEvent evt)//GEN-FIRST:event_connectionsSliderMouseReleased
    {//GEN-HEADEREND:event_connectionsSliderMouseReleased
        notifier.connectThreshold = connectionsSlider.getValue();
        Utilities.updateSetting("connectThreshold", String.valueOf(connectionsSlider.getValue()),"notifications.json");     
    }//GEN-LAST:event_connectionsSliderMouseReleased

    private void inAppBoxActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_inAppBoxActionPerformed
    {//GEN-HEADEREND:event_inAppBoxActionPerformed
        notifier.inAppEnabled = inAppBox.isSelected();
        Utilities.updateSetting("inAppEnabled", String.valueOf(inAppBox.isSelected()),"notifications.json");
    }//GEN-LAST:event_inAppBoxActionPerformed

    private void encryptBoxActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_encryptBoxActionPerformed
    {//GEN-HEADEREND:event_encryptBoxActionPerformed
        appPasswordField.setEnabled(encryptBox.isSelected());
        confirmPasswordField.setEnabled(encryptBox.isSelected());
    }//GEN-LAST:event_encryptBoxActionPerformed

    private void loginButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_loginButtonActionPerformed
    {//GEN-HEADEREND:event_loginButtonActionPerformed
        if(ConnectionDB.CanConnect("mail_settings", "node_monitor", loginPasswordField.getPassword(), Folders.DB.get()))
        {
            appPassword = String.valueOf(loginPasswordField.getPassword());
            loginPanel.setVisible(false);
            emailEnabledLabel.setText("Email notifications are enabled");  
            
            if(settingsToggleButton.isSelected())
                fillSettingsFields();            
        }
        else
        {
            JOptionPane.showMessageDialog(this, "Wrong password");
        }
    }//GEN-LAST:event_loginButtonActionPerformed

    private void loginPasswordFieldKeyReleased(java.awt.event.KeyEvent evt)//GEN-FIRST:event_loginPasswordFieldKeyReleased
    {//GEN-HEADEREND:event_loginPasswordFieldKeyReleased
        if(evt.getKeyCode() == KeyEvent.VK_ENTER)
            loginButtonActionPerformed(null);
    }//GEN-LAST:event_loginPasswordFieldKeyReleased

    private void settingsToggleButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_settingsToggleButtonActionPerformed
    {//GEN-HEADEREND:event_settingsToggleButtonActionPerformed
        if(settingsToggleButton.isSelected())
        {
            settingsToggleButton.setText("Hide mail server settings");
            fillSettingsFields();
            setupServerPanel.setVisible(true);
        }
        else
        {
            settingsToggleButton.setText("Show mail server settings");
            setupServerPanel.setVisible(false);            
        }
    }//GEN-LAST:event_settingsToggleButtonActionPerformed

    private void passwordHintLabelMouseReleased(java.awt.event.MouseEvent evt)//GEN-FIRST:event_passwordHintLabelMouseReleased
    {//GEN-HEADEREND:event_passwordHintLabelMouseReleased
        String message = 
           "To keep your mail server credentials safe it is advised to encrypt the mail server "
                + "settings file with a password. Someone that gains access to an un-encrypted settings "
                + "file could potentially extract your mail server password from it.<br/><br/>"
                + "Your mail server password will always be encrypted in the settings file, but the "
                + "file itself can only be encrypted if you provide an app password.<br/><br/>"
                + "If you encrypt your mail settings file, you'll have to provide your app password every time "
                + "that you start this app. This is not neccesary if you don't use an app password or "
                + "if you opt to not receive e-mail notifications.";
        
        gui.showHintDialog(evt, message);
    }//GEN-LAST:event_passwordHintLabelMouseReleased

    private void settingsHintLabelMouseReleased(java.awt.event.MouseEvent evt)//GEN-FIRST:event_settingsHintLabelMouseReleased
    {//GEN-HEADEREND:event_settingsHintLabelMouseReleased
        String message = 
            "These are the settings that your e-mail provider needs in order to send "
                + "an e-mail from your mail-account.<br/><br/>"
                + "The recipient is the e-mail address at which you'd like to receive the notifications.<br/>"
                + "The SMTP server is the server through which the service is provided.<br/>"
                + "The port number is the port at which the server listens for outgoing mail requests.<br/>"
                + "The mail username and mail password are the credentials that you "
                + "use to log in to your mail provider.<br/><br/>"
                + "You can find the settings for your provider by performing an internet search. "
                + "Such a search might be 'smtp settings [your provider here]'. This will usually yield the required search result.<br/><br/>"
                + "Please note that due to log-in restrictions by Google, Gmail is not supported unless those "
                + "restrictions are disabled in your Google account.";
        
        gui.showHintDialog(evt, message);
    }//GEN-LAST:event_settingsHintLabelMouseReleased

    private void disclaimerLabelMouseReleased(java.awt.event.MouseEvent evt)//GEN-FIRST:event_disclaimerLabelMouseReleased
    {//GEN-HEADEREND:event_disclaimerLabelMouseReleased
        String message = 
                "Great care has been taken to ensure that your mail server password is stored in a secure "
                + "manner, however for convenience's sake the option to use an un-encrypted settings "
                + "file has been left up to the user. Your settings file will be stored locally and can only be "
                + "accessed by someone with access to your computer.<br/><br/>"
                + "By continuing and saving your mail server settings you acknowledge that you are doing "
                + "so at your own responsibility and will not hold the developer of this app liable or accountable "
                + "for damages resulting from any eventual misappropriation of your mail server credentials.<br/><br/>"
                + "For those who prefer to mitigate this risk it is advisable to use a throwaway e-mail account "
                + "or refrain from using email notifications and only use in-app notifications.";
        
        gui.showHintDialog(evt, message);
    }//GEN-LAST:event_disclaimerLabelMouseReleased

    private void mintSliderStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_mintSliderStateChanged
    {//GEN-HEADEREND:event_mintSliderStateChanged
        mintLostBox.setText("has NOT been minting for more than " + mintSlider.getValue() + " minutes");
    }//GEN-LAST:event_mintSliderStateChanged

    private void mintSliderMouseReleased(java.awt.event.MouseEvent evt)//GEN-FIRST:event_mintSliderMouseReleased
    {//GEN-HEADEREND:event_mintSliderMouseReleased
        notifier.mintThreshold = mintSlider.getValue();
        Utilities.updateSetting("mintThreshold", String.valueOf(mintSlider.getValue()),"notifications.json");     
    }//GEN-LAST:event_mintSliderMouseReleased

    private void mintGainedBoxActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_mintGainedBoxActionPerformed
    {//GEN-HEADEREND:event_mintGainedBoxActionPerformed
        notifier.mintGainedEnabled = mintGainedBox.isSelected();
        Utilities.updateSetting("mintGainedEnabled", String.valueOf(mintGainedBox.isSelected()),"notifications.json");
    }//GEN-LAST:event_mintGainedBoxActionPerformed

    private void mintLostBoxActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_mintLostBoxActionPerformed
    {//GEN-HEADEREND:event_mintLostBoxActionPerformed
        notifier.mintLostEnabled = mintLostBox.isSelected();
        Utilities.updateSetting("mintLostEnabled", String.valueOf(mintLostBox.isSelected()),"notifications.json");                
        checkToggle(mintLostBox, mintGainedBox,"mintGainedEnabled");
    }//GEN-LAST:event_mintLostBoxActionPerformed

    private void clearAllButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_clearAllButtonActionPerformed
    {//GEN-HEADEREND:event_clearAllButtonActionPerformed
        try(Connection c = ConnectionDB.getConnection("notifications", Folders.DB.get()))
        {
            if(dbManager.TableExists("notifications", c))
            {
                dbManager.ExecuteUpdate("delete from notifications", c);
                textArea.setText("");
                refreshNotifications();
            }            
        }
        catch (Exception e)
        {
            BackgroundService.AppendLog(e);
        }
    }//GEN-LAST:event_clearAllButtonActionPerformed

    private void emailLimitSliderStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_emailLimitSliderStateChanged
    {//GEN-HEADEREND:event_emailLimitSliderStateChanged
        emailLimitBox.setText("Limit e-mail notifications to " + emailLimitSlider.getValue() + " per day");
    }//GEN-LAST:event_emailLimitSliderStateChanged

    private void emailLimitSliderMouseReleased(java.awt.event.MouseEvent evt)//GEN-FIRST:event_emailLimitSliderMouseReleased
    {//GEN-HEADEREND:event_emailLimitSliderMouseReleased
        notifier.emailLimit = emailLimitSlider.getValue();
        Utilities.updateSetting("emailLimit", String.valueOf(emailLimitSlider.getValue()),"notifications.json");
    }//GEN-LAST:event_emailLimitSliderMouseReleased

    private void emailLimitBoxActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_emailLimitBoxActionPerformed
    {//GEN-HEADEREND:event_emailLimitBoxActionPerformed
        notifier.emailLimitEnabled = emailLimitBox.isSelected();
        Utilities.updateSetting("emailLimitEnabled", String.valueOf(emailLimitBox.isSelected()),"notifications.json");
    }//GEN-LAST:event_emailLimitBoxActionPerformed

    private void emailLimitHintLabelMouseReleased(java.awt.event.MouseEvent evt)//GEN-FIRST:event_emailLimitHintLabelMouseReleased
    {//GEN-HEADEREND:event_emailLimitHintLabelMouseReleased
        String message = 
                "Dependent on your notifications settings, it is possible that Node Monitor will "
                + "send you a large number of e-mail notifications.<br/><br/>"
                + "To avoid your inbox getting overwhelmed with notifications you "
                + "can set a limit for the number of e-mails that Node Monitor will send within a 24 hour period.<br/><br/>"
                + "If you're receiving too many notifications you can try tweaking your notifications settings by enabling "
                + "or disabling certain notifications or by adjusting the sliders to increase or decrease the threshold "
                + "at which a notification will be sent.";
        
        gui.showHintDialog(evt, message);
    }//GEN-LAST:event_emailLimitHintLabelMouseReleased

    private void inBackgroundBoxActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_inBackgroundBoxActionPerformed
    {//GEN-HEADEREND:event_inBackgroundBoxActionPerformed
        Utilities.updateSetting("runInBackground", String.valueOf(inBackgroundBox.isSelected()),"notifications.json");
    }//GEN-LAST:event_inBackgroundBoxActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPasswordField appPasswordField;
    private javax.swing.JLabel appPasswordLabel;
    private javax.swing.JButton clearAllButton;
    private javax.swing.JPasswordField confirmPasswordField;
    private javax.swing.JLabel confirmPasswordLabel;
    protected javax.swing.JCheckBox connectionsGainedBox;
    protected javax.swing.JCheckBox connectionsLostBox;
    protected javax.swing.JSlider connectionsSlider;
    private javax.swing.JCheckBox disclaimerCheckbox;
    private javax.swing.JLabel disclaimerLabel;
    protected javax.swing.JCheckBox emailBox;
    private javax.swing.JLabel emailEnabledLabel;
    protected javax.swing.JCheckBox emailLimitBox;
    private javax.swing.JLabel emailLimitHintLabel;
    protected javax.swing.JSlider emailLimitSlider;
    private javax.swing.JCheckBox encryptBox;
    private javax.swing.JPanel headerPanel;
    protected javax.swing.JCheckBox inAppBox;
    protected javax.swing.JCheckBox inBackgroundBox;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JSeparator jSeparator6;
    private javax.swing.JSeparator jSeparator7;
    private javax.swing.JSeparator jSeparator8;
    private javax.swing.JSeparator jSeparator9;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JButton loginButton;
    private javax.swing.JPanel loginPanel;
    private javax.swing.JPasswordField loginPasswordField;
    private javax.swing.JPasswordField mailPasswordField;
    private javax.swing.JPanel mainPanel;
    protected javax.swing.JCheckBox mintGainedBox;
    protected javax.swing.JCheckBox mintLostBox;
    protected javax.swing.JSlider mintSlider;
    private javax.swing.JPanel notificationsMainPanel;
    private javax.swing.JTable notificationsTable;
    protected javax.swing.JCheckBox offlineBox;
    protected javax.swing.JCheckBox onlineBox;
    private javax.swing.JLabel passwordHintLabel;
    private javax.swing.JLabel passwordLabel;
    private javax.swing.JTextField portInput;
    private javax.swing.JLabel portLabel;
    private javax.swing.JCheckBox receivedMailCheckbox;
    private javax.swing.JTextField recipientInput;
    private javax.swing.JLabel recipientLabel;
    private javax.swing.JButton saveMailServerButton;
    private javax.swing.JLabel settingsHintLabel;
    private javax.swing.JPanel settingsPanel;
    private javax.swing.JScrollPane settingsScrollPane;
    private javax.swing.JToggleButton settingsToggleButton;
    private javax.swing.JLabel setupMailLabel;
    private javax.swing.JPanel setupServerPanel;
    private javax.swing.JLabel smtpLabel;
    private javax.swing.JTextField smtpServerInput;
    protected javax.swing.JCheckBox syncGainBox;
    protected javax.swing.JCheckBox syncLostBox;
    protected javax.swing.JSlider syncSlider;
    private javax.swing.JTabbedPane tabbedPane;
    private javax.swing.JButton testMailServerButton;
    private javax.swing.JTextArea textArea;
    private javax.swing.JLabel userLabel;
    private javax.swing.JTextField usernameInput;
    // End of variables declaration//GEN-END:variables
}
