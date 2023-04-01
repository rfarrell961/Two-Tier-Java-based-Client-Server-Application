// Display the results of queries against the bikes table in the bikedb database.
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import java.util.Properties;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import com.mysql.cj.jdbc.MysqlDataSource;
import java.io.FileInputStream;
import java.io.IOException;


public class Project3 extends JFrame
{
    private static final int FIELD_WIDTH = 20;
    private String[] propFiles = {"root.properties", "client.properties"};

    private JPanel topLeftPanel;
    private JPanel topRightPanel;

    private JTextField userField;
    private JTextField passField;
    private JComboBox propCboBox;
    private JButton connectDb;
    private JLabel statusLabel;
    private JButton clearResultsBtn;
    private JTextArea queryArea;
    private JScrollPane bottomScrollPane;
    private JTable table;
    private DbConnectionModel dbCon;
    private DbConnectionModel dbConOperations;
    private String propFile;

    private boolean isConnected = false;

    public Project3()
    {   

        super("SQL Client APP - (MJL - CNT 4714 - Spring 2023 - Project 3)");
        setLayout(new GridBagLayout());

        topRightPanel = new JPanel(new GridLayout(3, 1));
        topLeftPanel = new JPanel(new GridLayout(5, 2));

        addTopRight();
        addTopLeft();

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        statusLabel = new JLabel("No Connection Now");
        statusLabel.setBackground(Color.BLACK);
        statusLabel.setOpaque(true);
        statusLabel.setForeground(Color.RED);
        add( statusLabel, c);

        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        add( new JLabel("SQL Execution Result Window"), c);

        addBottom();

        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        clearResultsBtn = new JButton("Clear Results Window");
        clearResultsBtn.setBackground(Color.YELLOW);
        clearResultsBtn.addActionListener(
            new ActionListener() {
                public void actionPerformed( ActionEvent event)
                {
                   dbCon.clearResults();
                }
            }
        );

        add( clearResultsBtn, c);
        setSize( 700, 800 ); // set window size
        setVisible( true ); // display window  

        setDefaultCloseOperation( DISPOSE_ON_CLOSE );
    }

    private void addTopRight()
    {
        queryArea = new JTextArea( 7, 100 );
        queryArea.setWrapStyleWord( true );
        queryArea.setLineWrap( true );
        queryArea.setBackground(Color.LIGHT_GRAY);
         
        JScrollPane scrollPane = new JScrollPane( queryArea,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, 
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        JButton submitButton = new JButton( "Execute SQL Command" );
        submitButton.setForeground(Color.BLACK);
        submitButton.setOpaque(true);
        submitButton.addActionListener(
            new ActionListener() {
                public void actionPerformed( ActionEvent event )
                {
                    if (!isConnected)
                        return;
                    
                    try 
                    {
                        dbConOperations = new DbConnectionModel("operations.properties");
                        dbCon = new DbConnectionModel(propFile); 
                        String command = queryArea.getText();
                        //System.out.println(command.split("")[0]);
                        if (((command.split(" ")[0]).toLowerCase()).equals("select"))
                        {
                            dbCon.setQuery( command );
                            dbConOperations.setUpdate("update operationscount set num_queries = num_queries + 1");
                        }
                        else
                        {
                            int res;
                            res = dbCon.setUpdate( command );
                            dbConOperations.setUpdate("update operationscount set num_updates = num_updates + 1");
                            if (res != 0)
                            {
                                JOptionPane.showMessageDialog((Component)event.getSource(),
                                "Successful Update... " + res + " rows updated",
                                "Successful Update",
                                JOptionPane.INFORMATION_MESSAGE);
                            }

                        }

                        table.setModel(dbCon);
                    }
                    catch ( ClassNotFoundException classException ) 
                    {
                        classException.printStackTrace();
                    } 
                    catch ( SQLException sqlException ) 
                    {
                        JOptionPane.showMessageDialog( null, 
                        sqlException.getMessage(), "Database error", 
                        JOptionPane.ERROR_MESSAGE );         
                    }
                }
            }
        );

        JButton clearButton = new JButton( "Clear SQL Command" );
        clearButton.setForeground(Color.RED);
        clearButton.setOpaque(true);
        clearButton.addActionListener(
            new ActionListener() {
                public void actionPerformed( ActionEvent event)
                {
                    queryArea.setText("");
                }
            }
        );

        Box commandBtnBox = Box.createHorizontalBox();
        commandBtnBox.add(clearButton);
        commandBtnBox.add(submitButton);

        topRightPanel.add(new JLabel("Enter An SQL Command"));
        topRightPanel.add(scrollPane);
        topRightPanel.add(commandBtnBox);

        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 1;
        c.gridy = 0;
        c.weighty = 1;
        c.weightx = 1;
        c.gridwidth = 1;
        c.insets = new Insets(0, 10, 0, 10);
        c.fill = GridBagConstraints.VERTICAL;
        c.anchor = GridBagConstraints.NORTHEAST;
        add(topRightPanel,c);
    }
   
    private void addBottom()
    {
        // Add components to frame
        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 3;
        c.weighty = 1;
        c.weightx = 2;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTHEAST;
        c.insets = new Insets(0, 10, 10, 10);
        table = new JTable();
        bottomScrollPane = new JScrollPane(table);
        add( bottomScrollPane, c);
    }

    private void addTopLeft()
    {
        connectDb = new JButton("Connect to Database");
        connectDb.setBackground(Color.BLUE);
        connectDb.addActionListener(
            new ActionListener() 
            {
               public void actionPerformed( ActionEvent event )
               {
                    Properties properties = new Properties();
                    FileInputStream filein = null;
                    isConnected = false;

                    try {

                        filein = new FileInputStream((String)propCboBox.getSelectedItem());
                        properties.load(filein);
                        if (!properties.getProperty("MYSQL_DB_USERNAME").equals(userField.getText()) || !properties.getProperty("MYSQL_DB_PASSWORD").equals(passField.getText()))
                        {
                            statusLabel.setText("NOT CONNECTED - User Crednetials Do Not Match Properties Field");
                            statusLabel.setForeground(Color.RED);
                            return;
                        }

                        isConnected = true;
                        propFile = (String)propCboBox.getSelectedItem();              
                        statusLabel.setText("Connected to " + properties.getProperty("MYSQL_DB_URL"));
                        statusLabel.setForeground(Color.YELLOW);
                    }             
                    catch (IOException e) {
                            e.printStackTrace();
                    }  
                }  
            }
        );

        propCboBox = new JComboBox();
        for (int i = 0; i < propFiles.length; i++)
        {
            propCboBox.addItem(propFiles[i]);
        }

        userField = new JTextField(FIELD_WIDTH);

        passField = new JTextField(FIELD_WIDTH);

        topLeftPanel.add(new JLabel("Connection Details", SwingConstants.CENTER));
        topLeftPanel.add(new JLabel());
        topLeftPanel.add(new JLabel("Properties File"));
        topLeftPanel.add(propCboBox);
        topLeftPanel.add(new JLabel("Username"));
        topLeftPanel.add(userField);
        topLeftPanel.add(new JLabel("Password"));
        topLeftPanel.add(passField);
        topLeftPanel.add(connectDb);

        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;
        c.weighty = 1;
        c.weightx = 1;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTHEAST;
        c.insets = new Insets(0, 0, 0, 0);
        add( topLeftPanel, c);
    }

   public static void main( String args[] ) 
   {
      new Project3();     
   }
}