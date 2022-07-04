package nodemonitor;

import enums.Extensions;
import enums.Folders;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.sql.Connection;
import java.util.Timer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

public class AppearancePanel extends javax.swing.JPanel
{    
    protected DatabaseManager dbManager;
    protected GUI gui;
    protected CustomColorChooser colorChooser;
    protected boolean initComplete;
    protected int guiBackgroundColor = Color.lightGray.getRGB();
    protected int guiComponentColor = Color.lightGray.getRGB();
    protected int guiFontColor = Color.black.getRGB();
    protected int componentFontColor = Color.black.getRGB();
    private Timer updateTimer;
    protected int currentTick;
    protected final static int UPDATE_INTERVAL = 15;
    protected long lastUpdatedTime;
    protected boolean updateInProgress;

    public AppearancePanel()
    {
        initComponents();
    }

    protected void initialise(DatabaseManager dbManager, GUI gui)
    {
        this.dbManager = dbManager;
        this.gui = gui;
        colorChooser = new CustomColorChooser(this);
        initListeners();            
        for (LookAndFeelInfo LFI : UIManager.getInstalledLookAndFeels())
        {
            ((DefaultListModel)lnfList.getModel()).addElement(LFI.getName());
        }
        initComplete = true;
        
    }//end initialise()
    
    
    protected void setGuiValues(String styleName,String folder, Extensions extension)
    {
        try(Connection connection = ConnectionDB.getConnection(styleName, folder, extension))
        {    
            Object fontObject = dbManager.GetFirstItem("gui", "font", connection);
            if(fontObject != null)
            {
                for(int i = 0; i < fontsBox.getItemCount(); i++)
                {
                    if(fontsBox.getItemAt(i).equals(fontObject.toString()))
                    {
                        fontsBox.setSelectedIndex(i);
                        break;
                    }
                }
            }
            
            Object bgcObject = dbManager.GetFirstItem("gui", "background_color", connection);
            if(bgcObject != null)
            {
                guiBackgroundColor = (int)bgcObject;
                guiBackgroundLabel.setBackground(new Color(guiBackgroundColor));
            }
            
            Object cmpObject = dbManager.GetFirstItem("gui", "component_color", connection);
            if(cmpObject != null)
            {
                guiComponentColor = (int)cmpObject;
                guiComponentsLabel.setBackground(new Color(guiComponentColor));
            }
            
            Object fontColorObject = dbManager.GetFirstItem("gui", "gui_font_color", connection);
            if(fontColorObject != null)
            {
                guiFontColor = (int)fontColorObject;
                guiFontColorLabel.setBackground(new Color(guiFontColor));
            }
            
            Object cmpFontObject = dbManager.GetFirstItem("gui", "component_font_color", connection);
            if(cmpFontObject != null)
            {
                componentFontColor = (int)cmpFontObject;
                componentFontLabel.setBackground(new Color(componentFontColor));
            }
            
        }
        catch(Exception e)
        {
            BackgroundService.AppendLog(e);
        }
    }
        
    protected void updateStylesList()
    {
        SwingUtilities.invokeLater(()->
        {          
            File folder = new File(System.getProperty("user.dir") + "/" + Folders.STYLES.get());
            if(!folder.isDirectory())
                folder.mkdir();

           File[] listOfFiles = folder.listFiles();

           var model = (DefaultListModel)stylesList.getModel();
           model.clear();

           for (File file : listOfFiles)
           {
               if(file.isDirectory())
                   continue;
               
               if(!file.getName().endsWith(".style"))
                   continue;
               
               String dbName = file.getName().substring(0,file.getName().length() - 6);
               model.addElement(dbName);             
           }  
           if(stylesList.getSelectedIndex() == -1)
           {
               loadStyleButton.setEnabled(false);
               deleteStyleButton.setEnabled(false);
           }           
        });        
    }
    
    private void initListeners()
    {              
       // <editor-fold defaultstate="collapsed" desc="Lists listeners"> 
        stylesList.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent evt)
            {
                if(stylesList.getSelectedIndex() >= 0)
                {
                    loadStyleButton.setEnabled(true);
                    saveStyleButton.setEnabled(true);
                    deleteStyleButton.setEnabled(true);
                }
                else
                {
                    loadStyleButton.setEnabled(false);
                    saveStyleButton.setEnabled(false);
                    deleteStyleButton.setEnabled(false);
                }
                
                if (evt.getClickCount() == 2)
                {
                    loadStyleButtonActionPerformed(null);
                }
            }
        });  
        
        lnfList.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent evt)
            {
                if(lnfList.getSelectedIndex() >= 0)
                    loadLnfButton.setEnabled(true);
                else
                    loadLnfButton.setEnabled(false);
                
                if (evt.getClickCount() == 2)
                {
                    loadLnfButtonActionPerformed(null);
                }
            }
        });  
        //</editor-fold>        
    }
    
    private void saveStyleFile(String styleName)
    {
        try(Connection connection = ConnectionDB.getConnection(styleName, Folders.STYLES.get(), Extensions.STYLE))
        {
            //when overwriting existing style file
            if(dbManager.TableExists("gui", connection))
                dbManager.ExecuteUpdate("drop table gui", connection);
            
            dbManager.CreateTable(new String[]{"gui",
                "background_color","int",
                "component_color","int",
                "gui_font_color","int",
                "component_font_color","int",
                "font","varchar(30)",
                "look_and_feel","varchar(30)"}, connection);            
            
            dbManager.InsertIntoDB(new String[]{"gui",
                "background_color",String.valueOf(guiBackgroundColor),
                "component_color",String.valueOf(guiComponentColor),
                "gui_font_color",String.valueOf(guiFontColor),
                "component_font_color",String.valueOf(componentFontColor),
                "font",String.valueOf(Utilities.SingleQuotedString(fontsBox.getSelectedItem().toString())),
                "look_and_feel",String.valueOf(Utilities.SingleQuotedString(UIManager.getLookAndFeel().getName()))}, connection);               
        }
        catch (Exception e)
        {
            BackgroundService.AppendLog(e);
        }
    }
     
     private void showColorChooser(JLabel colorBoxLabel)
     {
         int x,y;
         int width,height;
         
        //only set location and size if chooser was closed more than 2.5 seconds ago
        if(System.currentTimeMillis() - colorChooser.lastClosedTime > 2500)
        {
            x = getLocationOnScreen().x;
            y = getLocationOnScreen().y;
            width = gui.getWidth() / 2;
            height = gui.getHeight() / 2;
            x += (gui.getWidth() / 2 - width / 2);
            y += (gui.getHeight() / 2 - height / 2);
            colorChooser.setLocation(x,y);
            colorChooser.setPreferredSize(new Dimension(width,height));
            colorChooser.pack();
        }   
        
        colorChooser.setColorBoxLabel(colorBoxLabel);
        colorChooser.setTitle(colorBoxLabel.getToolTipText());
        colorChooser.setVisible(true);
     }
     
     public void changeColor(JLabel label,Color color)
     {
             //cannot use button.getBackground() -> might not 
             //be set yet at the moment of db entry
             guiBackgroundColor = guiBackgroundLabel.getBackground().getRGB();
             guiComponentColor = guiComponentsLabel.getBackground().getRGB();
             guiFontColor = guiFontColorLabel.getBackground().getRGB();
             componentFontColor = componentFontLabel.getBackground().getRGB(); 
             updateGuiItems();
         
     }
     
     protected void updateGuiItems()
     {  
         //this function gets called by listeners (fontsBox) before GUI is done initialising
         if(!initComplete)
             return;
         
         Color bgColor = new Color(guiBackgroundColor);
         Color cmpColor = new Color(guiComponentColor);
         Color fontColor = new Color(guiFontColor);
         Color cmpFontColor = new Color(componentFontColor);         
         
         // <editor-fold defaultstate="collapsed" desc="Update local components">  
            
         
        var fontBoxRenderer = (ComboRenderer) fontsBox.getRenderer();
        fontsBox.setBackground(cmpColor);
        fontsBox.setForeground(cmpFontColor);
        fontBoxRenderer.bgColor = cmpColor;
        fontBoxRenderer.fgColor = cmpFontColor;
         
         Font listFont = new Font(fontsBox.getSelectedItem().toString(), Font.PLAIN, stylesList.getFont().getSize());
         
         //These lists will not show up as components of menu's 
         //they are children of scrollPanes
         stylesList.setBackground(cmpColor);
         stylesList.setFont(listFont);
         stylesList.setForeground(cmpFontColor);      
         
         lnfList.setBackground(cmpColor);
         lnfList.setFont(listFont);
         lnfList.setForeground(cmpFontColor);            
         
         updateCollapsableMenu(styleMenu, bgColor, cmpColor, cmpFontColor, fontColor);       
                  
         //</editor-fold>
         
         gui.updateStyle(bgColor, cmpColor, fontColor, cmpFontColor, fontsBox.getSelectedItem().toString());
         
         updateGuiStyleLabels();    
         updatePopOutMenusDimension();
     }
     
     private void updateGuiStyleLabels()
     {
        guiBackgroundLabel.setBackground(new Color(guiBackgroundColor));
        guiComponentsLabel.setBackground(new Color(guiComponentColor));
        guiFontColorLabel.setBackground(new Color(guiFontColor));
        componentFontLabel.setBackground(new Color(componentFontColor));
     }
     
     private void updateCollapsableMenu(JPanel menu, Color bgColor, Color cmpColor, Color cmpFontColor,Color fontColor)
     {
         menu.setBackground(bgColor);
         for(Component c : menu.getComponents())
         {
             if(c instanceof JButton)
             {
                 c.setBackground(cmpColor);
                 c.setFont(new Font(fontsBox.getSelectedItem().toString(), c.getFont().getStyle(), c.getFont().getSize()));
                 c.setForeground(cmpFontColor);
             }
             if(c instanceof JScrollPane)
                 c.setBackground(bgColor);
             if(c instanceof JLabel)
             {                 
                 c.setFont(new Font(fontsBox.getSelectedItem().toString(), c.getFont().getStyle(), c.getFont().getSize()));
                 c.setForeground(fontColor);
             }
             if(c instanceof JCheckBox)
             {
                 c.setBackground(bgColor);
                 c.setFont(new Font(fontsBox.getSelectedItem().toString(), c.getFont().getStyle(), c.getFont().getSize()));
                 c.setForeground(fontColor);
             }
             if(c instanceof JSlider)
                 c.setBackground(bgColor);
         }
     }    
     
     private void updatePopOutMenusDimension()
     {
         SwingUtilities.invokeLater(()->
         {
            //When font is changed, we need to re-calculate the width and height of the layout and style menu
            //Get the height of all the components in the style/layout menu's by the grid bag manager and
            //set the height of the panel by their cumulative height + 20  (insets)
            int totalHeight = 0;
            GridBagLayout gbl = (GridBagLayout) styleMenu.getLayout();
            for (int height : gbl.getLayoutDimensions()[1])
            {
                totalHeight += height;
            }
            styleMenu.setMinimumSize(new Dimension(styleMenu.getWidth(), totalHeight + 20));
            styleMenu.setPreferredSize(new Dimension(styleMenu.getWidth(), totalHeight + 20));
            styleMenu.revalidate();

            //make sure the width of the style menu scroll pane is big enough to fit the style menu
            styleMenuScrollpane.setPreferredSize(new Dimension(
                    styleMenu.getWidth() + styleMenuScrollpane.getVerticalScrollBar().getWidth(), styleMenu.getHeight()));
         });
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

        styleMenuScrollpane = new javax.swing.JScrollPane();
        styleMenuScrollpane.getHorizontalScrollBar().setUnitIncrement(10);
        styleMenuScrollpane.getVerticalScrollBar().setUnitIncrement(10);
        styleMenu = new javax.swing.JPanel();
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] fontFamilyNames = ge.getAvailableFontFamilyNames();
        fontsBox = new JComboBox(fontFamilyNames);
        fontsBox.setSelectedItem(0);
        fontsBox.setRenderer(new ComboRenderer(fontsBox));
        fontsBox.addItemListener(new ItemListener()
        {

            @Override
            public void itemStateChanged(ItemEvent e)
            {
                if (e.getStateChange() == ItemEvent.SELECTED)
                {
                    final String fontName = fontsBox.getSelectedItem().toString();
                    fontsBox.setFont(new Font(fontName, Font.PLAIN, 12));
                }
            }
        });
        fontsBox.setSelectedItem(0);
        fontsBox.getEditor().selectAll();
        saveStyleButton = new javax.swing.JButton();
        loadStyleButton = new javax.swing.JButton();
        sylesListScrollpane = new javax.swing.JScrollPane();
        stylesList = new javax.swing.JList(new DefaultListModel());
        deleteStyleButton = new javax.swing.JButton();
        guiBackgroundLabel = new javax.swing.JLabel();
        guiBackgroundLabel.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)), javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 255, 255))));
        jLabel12 = new javax.swing.JLabel();
        jSeparator4 = new javax.swing.JSeparator();
        jLabel13 = new javax.swing.JLabel();
        guiComponentsLabel = new javax.swing.JLabel();
        guiComponentsLabel.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)), javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 255, 255))));
        jLabel14 = new javax.swing.JLabel();
        guiFontColorLabel = new javax.swing.JLabel();
        guiFontColorLabel.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)), javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 255, 255))));
        jLabel15 = new javax.swing.JLabel();
        componentFontLabel = new javax.swing.JLabel();
        componentFontLabel.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)), javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 255, 255))));
        fontStyleLabel = new javax.swing.JLabel();
        jSeparator5 = new javax.swing.JSeparator();
        uiLabel1 = new javax.swing.JLabel();
        fontStyleLabel1 = new javax.swing.JLabel();
        stylesLinkLabel = new javax.swing.JLabel();
        lnfLabel = new javax.swing.JLabel();
        loadLnfButton = new javax.swing.JButton();
        sylesListScrollpane1 = new javax.swing.JScrollPane();
        lnfList = new javax.swing.JList(new DefaultListModel());
        jSeparator6 = new javax.swing.JSeparator();
        lnfLinkLabel = new javax.swing.JLabel();

        setBackground(new java.awt.Color(102, 255, 102));
        setOpaque(false);

        styleMenu.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        styleMenu.setDoubleBuffered(false);
        styleMenu.setFocusTraversalPolicyProvider(true);
        styleMenu.setVerifyInputWhenFocusTarget(false);
        styleMenu.setLayout(new java.awt.GridBagLayout());

        fontsBox.setFont(new java.awt.Font("Segoe UI", 0, 10)); // NOI18N
        fontsBox.setMaximumSize(new java.awt.Dimension(150, 30));
        fontsBox.setPreferredSize(new java.awt.Dimension(150, 30));
        fontsBox.addItemListener(new java.awt.event.ItemListener()
        {
            public void itemStateChanged(java.awt.event.ItemEvent evt)
            {
                fontsBoxItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 0);
        styleMenu.add(fontsBox, gridBagConstraints);

        saveStyleButton.setText("Save style");
        saveStyleButton.setMinimumSize(new java.awt.Dimension(150, 25));
        saveStyleButton.setPreferredSize(new java.awt.Dimension(150, 25));
        saveStyleButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                saveStyleButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(3, 0, 3, 0);
        styleMenu.add(saveStyleButton, gridBagConstraints);

        loadStyleButton.setText("Load style");
        loadStyleButton.setMinimumSize(new java.awt.Dimension(150, 25));
        loadStyleButton.setPreferredSize(new java.awt.Dimension(150, 25));
        loadStyleButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                loadStyleButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(3, 0, 3, 0);
        styleMenu.add(loadStyleButton, gridBagConstraints);

        sylesListScrollpane.setMaximumSize(new java.awt.Dimension(125, 250));
        sylesListScrollpane.setMinimumSize(new java.awt.Dimension(125, 150));
        sylesListScrollpane.setPreferredSize(new java.awt.Dimension(125, 150));
        sylesListScrollpane.setRequestFocusEnabled(false);

        stylesList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        sylesListScrollpane.setViewportView(stylesList);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipady = 50;
        gridBagConstraints.insets = new java.awt.Insets(3, 0, 5, 0);
        styleMenu.add(sylesListScrollpane, gridBagConstraints);

        deleteStyleButton.setText("Delete style");
        deleteStyleButton.setMinimumSize(new java.awt.Dimension(150, 25));
        deleteStyleButton.setPreferredSize(new java.awt.Dimension(150, 25));
        deleteStyleButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                deleteStyleButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 3, 0);
        styleMenu.add(deleteStyleButton, gridBagConstraints);

        guiBackgroundLabel.setText("pick color");
        guiBackgroundLabel.setToolTipText("Pick a background color for the UI");
        guiBackgroundLabel.setOpaque(true);
        guiBackgroundLabel.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseReleased(java.awt.event.MouseEvent evt)
            {
                colorLabelMouseReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(18, 3, 2, 0);
        styleMenu.add(guiBackgroundLabel, gridBagConstraints);
        guiBackgroundLabel.setText("     ");

        jLabel12.setText("UI background");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(18, 25, 2, 0);
        styleMenu.add(jLabel12, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
        styleMenu.add(jSeparator4, gridBagConstraints);

        jLabel13.setText("UI elements");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 25, 2, 0);
        styleMenu.add(jLabel13, gridBagConstraints);

        guiComponentsLabel.setText("pick color");
        guiComponentsLabel.setToolTipText("Pick a color for the UI elements (buttons, lists, etc)");
        guiComponentsLabel.setOpaque(true);
        guiComponentsLabel.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseReleased(java.awt.event.MouseEvent evt)
            {
                colorLabelMouseReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 3, 2, 0);
        styleMenu.add(guiComponentsLabel, gridBagConstraints);
        guiComponentsLabel.setText("     ");

        jLabel14.setText("UI font");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 25, 2, 0);
        styleMenu.add(jLabel14, gridBagConstraints);

        guiFontColorLabel.setText("pick color");
        guiFontColorLabel.setToolTipText("Pick a font color for the UI");
        guiFontColorLabel.setOpaque(true);
        guiFontColorLabel.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseReleased(java.awt.event.MouseEvent evt)
            {
                colorLabelMouseReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 3, 2, 0);
        styleMenu.add(guiFontColorLabel, gridBagConstraints);
        guiFontColorLabel.setText("     ");

        jLabel15.setText("Element font");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 25, 2, 0);
        styleMenu.add(jLabel15, gridBagConstraints);

        componentFontLabel.setText("pick color");
        componentFontLabel.setToolTipText("Pick a font color for the UI elements (buttons, lists, etc)");
        componentFontLabel.setOpaque(true);
        componentFontLabel.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseReleased(java.awt.event.MouseEvent evt)
            {
                colorLabelMouseReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 3, 2, 0);
        styleMenu.add(componentFontLabel, gridBagConstraints);
        componentFontLabel.setText("     ");

        fontStyleLabel.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        fontStyleLabel.setText("Styles");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        styleMenu.add(fontStyleLabel, gridBagConstraints);

        jSeparator5.setOrientation(javax.swing.SwingConstants.VERTICAL);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 12;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 15, 5, 15);
        styleMenu.add(jSeparator5, gridBagConstraints);

        uiLabel1.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        uiLabel1.setText("UI Colors");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        styleMenu.add(uiLabel1, gridBagConstraints);

        fontStyleLabel1.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        fontStyleLabel1.setText("Font");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        styleMenu.add(fontStyleLabel1, gridBagConstraints);

        stylesLinkLabel.setFont(new java.awt.Font("Segoe UI", 1, 10)); // NOI18N
        stylesLinkLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        stylesLinkLabel.setText("<html><u>what are styles?</u></html>");
        stylesLinkLabel.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseClicked(java.awt.event.MouseEvent evt)
            {
                stylesLinkLabelMouseClicked(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        styleMenu.add(stylesLinkLabel, gridBagConstraints);

        lnfLabel.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lnfLabel.setText("Installed L&F's");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        styleMenu.add(lnfLabel, gridBagConstraints);

        loadLnfButton.setText("Load L&F");
        loadLnfButton.setMinimumSize(new java.awt.Dimension(150, 25));
        loadLnfButton.setPreferredSize(new java.awt.Dimension(150, 25));
        loadLnfButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                loadLnfButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(3, 0, 3, 0);
        styleMenu.add(loadLnfButton, gridBagConstraints);

        sylesListScrollpane1.setMaximumSize(new java.awt.Dimension(125, 250));
        sylesListScrollpane1.setMinimumSize(new java.awt.Dimension(125, 150));
        sylesListScrollpane1.setPreferredSize(new java.awt.Dimension(125, 150));
        sylesListScrollpane1.setRequestFocusEnabled(false);

        lnfList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        sylesListScrollpane1.setViewportView(lnfList);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipady = 50;
        gridBagConstraints.insets = new java.awt.Insets(3, 0, 5, 0);
        styleMenu.add(sylesListScrollpane1, gridBagConstraints);

        jSeparator6.setOrientation(javax.swing.SwingConstants.VERTICAL);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 12;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 15, 5, 15);
        styleMenu.add(jSeparator6, gridBagConstraints);

        lnfLinkLabel.setFont(new java.awt.Font("Segoe UI", 1, 10)); // NOI18N
        lnfLinkLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lnfLinkLabel.setText("<html><u>what are L&F's?</u></html>");
        lnfLinkLabel.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseClicked(java.awt.event.MouseEvent evt)
            {
                lnfLinkLabelMouseClicked(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        styleMenu.add(lnfLinkLabel, gridBagConstraints);

        styleMenuScrollpane.setViewportView(styleMenu);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(styleMenuScrollpane, javax.swing.GroupLayout.DEFAULT_SIZE, 637, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(styleMenuScrollpane, javax.swing.GroupLayout.DEFAULT_SIZE, 474, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void stylesLinkLabelMouseClicked(java.awt.event.MouseEvent evt)//GEN-FIRST:event_stylesLinkLabelMouseClicked
    {//GEN-HEADEREND:event_stylesLinkLabelMouseClicked
        String message = 
            "Styles are visual themes that you can apply to your user interface environment.<br/><br/>"
            + "You can customize the way your UI looks by setting different colors and fonts for "
            + "background panels, buttons, textfields and other such components.<br/><br/>"
            + "Changes to the current style will not be saved automatically, you can save your new "
            + "style by saving it to a new style file or by overwriting the current one.<br/><br/>"
            + "Tip: You can double click on a style to load it.";
        
        gui.showHintDialog(evt, message);
    }//GEN-LAST:event_stylesLinkLabelMouseClicked

    private void colorLabelMouseReleased(java.awt.event.MouseEvent evt)//GEN-FIRST:event_colorLabelMouseReleased
    {//GEN-HEADEREND:event_colorLabelMouseReleased
        showColorChooser((JLabel) evt.getSource());
    }//GEN-LAST:event_colorLabelMouseReleased

    private void deleteStyleButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_deleteStyleButtonActionPerformed
    {//GEN-HEADEREND:event_deleteStyleButtonActionPerformed
        if(stylesList.getSelectedIndex() == -1)
            return;

        if(JOptionPane.showConfirmDialog(deleteStyleButton, "Delete " + stylesList.getSelectedValue() + "?",
            "Please confirm", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
            return;

        File file = new File(System.getProperty("user.dir") + "/" +
        Folders.STYLES.get() + "/" + stylesList.getSelectedValue() + ".style");
        file.delete();

        //using index to remove causes weird behaviour, use model
        var model = (DefaultListModel)stylesList.getModel();
        model.removeElement(stylesList.getSelectedValue());
        updateStylesList();
    }//GEN-LAST:event_deleteStyleButtonActionPerformed

    private void loadStyleButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_loadStyleButtonActionPerformed
    {//GEN-HEADEREND:event_loadStyleButtonActionPerformed
        if(stylesList.getSelectedIndex() == -1)
            return;
        
        setGuiValues(stylesList.getSelectedValue(), Folders.STYLES.get(), Extensions.STYLE);
        updateGuiItems();    
        Utilities.updateSetting("currentStyle", stylesList.getSelectedValue(),"settings.json");
    }//GEN-LAST:event_loadStyleButtonActionPerformed

    private void saveStyleButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_saveStyleButtonActionPerformed
    {//GEN-HEADEREND:event_saveStyleButtonActionPerformed
        var model = (DefaultListModel) stylesList.getModel();

        String dbFileName = null;

        if(stylesList.getSelectedIndex() >= 0)
        {
            do
            {
                int choice = JOptionPane.showConfirmDialog(saveStyleButton,
                    Utilities.AllignCenterHTML("Overwrite '" + stylesList.getSelectedValue() + "'?<br/>Choose 'No' to create a new file"),
                    "Overwrite?",
                    JOptionPane.YES_NO_OPTION);

                if(choice == JOptionPane.CANCEL_OPTION)
                    return;
                
                if(choice == JOptionPane.YES_OPTION)
                {
                    dbFileName = stylesList.getSelectedValue();
                    break;
                }
                if(choice == JOptionPane.NO_OPTION)
                    dbFileName = JOptionPane.showInputDialog(saveStyleButton, "Creating a new style. Please give it a name");
            }
            while(dbFileName != null && Utilities.containsIgnoreCase(model.toArray(), dbFileName));

        }
        else
        {
            dbFileName = JOptionPane.showInputDialog(saveStyleButton, "Creating a new style. Please give it a name");
        }

        if(dbFileName == null)
            return;
        
        saveStyleFile(dbFileName);
        updateStylesList();
    }//GEN-LAST:event_saveStyleButtonActionPerformed

    private void fontsBoxItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_fontsBoxItemStateChanged
    {//GEN-HEADEREND:event_fontsBoxItemStateChanged
        if(initComplete)
            updateGuiItems();
    }//GEN-LAST:event_fontsBoxItemStateChanged

    private void loadLnfButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_loadLnfButtonActionPerformed
    {//GEN-HEADEREND:event_loadLnfButtonActionPerformed
        gui.setLookAndFeel(lnfList.getSelectedValue());
        Utilities.updateSetting("currentLnf", lnfList.getSelectedValue(),"settings.json");
    }//GEN-LAST:event_loadLnfButtonActionPerformed

    private void lnfLinkLabelMouseClicked(java.awt.event.MouseEvent evt)//GEN-FIRST:event_lnfLinkLabelMouseClicked
    {//GEN-HEADEREND:event_lnfLinkLabelMouseClicked
        String message = 
            "Look and Feels (L&F's) are stylistic UI designs that come pre-installed with "
            + "your Java software. The L&F determines the design of components such as buttons, "
            + "sliders, scrollbars etcetera.<br/><br/>"
            + "All L&F's installed on your system will be listed below, it is also possible to install "
            + "additional L&F's. A quick internet search on how to install Java look and feels "
            + "will inform you about doing so.<br/><br/>"
            + "Tip: You can double click on a look and feel to load it.";
        
        gui.showHintDialog(evt, message);
    }//GEN-LAST:event_lnfLinkLabelMouseClicked


    // Variables declaration - do not modify//GEN-BEGIN:variables
    protected javax.swing.JLabel componentFontLabel;
    private javax.swing.JButton deleteStyleButton;
    private javax.swing.JLabel fontStyleLabel;
    private javax.swing.JLabel fontStyleLabel1;
    protected javax.swing.JComboBox<String> fontsBox;
    protected javax.swing.JLabel guiBackgroundLabel;
    protected javax.swing.JLabel guiComponentsLabel;
    protected javax.swing.JLabel guiFontColorLabel;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JSeparator jSeparator6;
    private javax.swing.JLabel lnfLabel;
    private javax.swing.JLabel lnfLinkLabel;
    private javax.swing.JList<String> lnfList;
    private javax.swing.JButton loadLnfButton;
    private javax.swing.JButton loadStyleButton;
    private javax.swing.JButton saveStyleButton;
    private javax.swing.JPanel styleMenu;
    private javax.swing.JScrollPane styleMenuScrollpane;
    private javax.swing.JLabel stylesLinkLabel;
    private javax.swing.JList<String> stylesList;
    private javax.swing.JScrollPane sylesListScrollpane;
    private javax.swing.JScrollPane sylesListScrollpane1;
    private javax.swing.JLabel uiLabel1;
    // End of variables declaration//GEN-END:variables
}
