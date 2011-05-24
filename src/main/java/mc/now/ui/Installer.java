package mc.now.ui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.apache.log4j.Logger;

import mc.now.util.InstallScript;
import mc.now.util.InstallerProperties;
import net.miginfocom.layout.CC;
import net.miginfocom.swing.MigLayout;

import com.jidesoft.swing.CheckBoxTree;
import com.jidesoft.swing.CheckBoxTreeSelectionModel;

@SuppressWarnings( "serial" )
public class Installer extends JFrame implements ActionListener {
  
  private static final Logger LOGGER = Logger.getLogger( Installer.class );
  private JButton nextButton;
  private JButton cancelButton;
  private JProgressBar progressBar;
  
  private int step = 0;
  private JPanel contentPane;
  private CheckBoxTree modTree;

  public Installer() {
    super(InstallerProperties.getFrameTitle());
    setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
    setMinimumSize(new Dimension(500,500));
    init();
    pack();
  }
  
  private void init() {
    JPanel p = new JPanel();
    p.setLayout( new MigLayout( "fill", "[fill,grow|]", "[fill,grow||]") ); //lc col row
    p.add(getMainPane(), new CC().spanX().grow());
    p.add(getProgressBar(), new CC().growX());
    p.add(getCancelButton(), new CC().alignX( "right" ));
    p.add(getNextButton(), new CC().alignX( "right" ));
    setContentPane( p );
  }
  
  private JButton getNextButton() {
    if (nextButton == null) {
      nextButton = new JButton("Next");
      nextButton.addActionListener(this);
    }
    return nextButton;
  }
  
  private JButton getCancelButton() {
    if (cancelButton == null) {
      cancelButton = new JButton("Cancel");
      cancelButton.addActionListener(this);
    }
    return cancelButton;
  }
  
  private JProgressBar getProgressBar() {
    if (progressBar == null) {
      progressBar = new JProgressBar();
    }
    return progressBar;
  }
  
  private JPanel getMainPane() {
    if (contentPane == null) {
      contentPane = new JPanel();
      initialPanel( contentPane );
    }
    return contentPane;
  }
  
  protected void initialPanel(JPanel contentPane) {
    JLabel text = new JLabel();
    try {
      ImageIcon icon = new ImageIcon( ImageIO.read(new FileInputStream(InstallerProperties.getLogoFile())) );
      text.setIcon( icon );
    } catch ( IOException e ) {
      LOGGER.error( "IO error on logo.png", e );
    }
    StringBuffer textBuffer = new StringBuffer();
    try {
      BufferedReader r = new BufferedReader( new FileReader(InstallerProperties.getInitTextFile()) );
      String line = null;
      while ((line = r.readLine()) != null) {
        textBuffer.append( line + "\n" );
      } 
    } catch (IOException ioe) {
      LOGGER.error( "IO error on logo.png", ioe );
    }
    text.setText(textBuffer.toString());
    text.setVerticalTextPosition(JLabel.BOTTOM);
    text.setHorizontalTextPosition(JLabel.CENTER);
    contentPane.add( text );
  }

  @Override
  public void actionPerformed( ActionEvent e ) {
    if (e.getSource() == getNextButton()) {
      advanceStep();
    } else if (e.getSource() == getCancelButton()) {
      setVisible( false );
      dispose();
    }
  }

  private void advanceStep() {
    step++;
    switch(step) {
      case 1: buildOptionsPane(); return;
      case 2: buildInstallingPane(); return;
      default: LOGGER.error( "Advanced too far" ); return;
    }
  }

  private void buildInstallingPane() {
    
    getNextButton().setEnabled( false );
    getCancelButton().setEnabled( false );
    getMainPane().removeAll();
    final JTextArea text = new JTextArea();
    text.setEditable( false );
    getMainPane().add(new JScrollPane(text));
    getMainPane().validate();
    getMainPane().repaint();
    SwingWorker<Object, String> worker = new SwingWorker<Object, String>(){

      @Override
      protected Object doInBackground() throws Exception {
        try {
          List<String> mods = new ArrayList<String>();
          CheckBoxTreeSelectionModel select = getModTree().getCheckBoxTreeSelectionModel();
          TreePath[] paths = select.getSelectionPaths();
          if (paths != null && paths.length > 0) {
            for (TreePath path : paths) {
              DefaultMutableTreeNode node = ((DefaultMutableTreeNode)path.getLastPathComponent());
              String mod = (String)node.getUserObject();
              if (mod == null) {
                for (int i = 0; i < node.getChildCount(); i++) {
                  DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt( i );
                  mods.add( (String) child.getUserObject());
                }
              } else {
                mods.add(mod);
              }
            }
          }
          InstallScript.guiInstall(mods,text,getProgressBar());
        } catch (Exception e) {
          LOGGER.error("Error while trying to install mods",e);
          JOptionPane.showMessageDialog( Installer.this, "Unexpected error while installing mods:\n"+e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE );
          setVisible( false );
          dispose();
        }
        return null;
      }
      
      @Override
      public void done() {
        getNextButton().removeActionListener( Installer.this );
        getNextButton().setText( "Done" );
        getNextButton().addActionListener( new ActionListener() {
          
          @Override
          public void actionPerformed( ActionEvent e ) {
            setVisible( false );
            dispose();
          }
        });
        getNextButton().setEnabled( true );
      }
      
    };
    
    worker.execute();
  }

  
  private CheckBoxTree getModTree() {
    if (modTree == null) {
      DefaultMutableTreeNode root = new DefaultMutableTreeNode();
      try {
        File opt = new File(InstallScript.EXTRA_MODS_FOLDER);
        for (File mod : opt.listFiles()) {
          if (!mod.isDirectory()) {
            continue;
          }
          DefaultMutableTreeNode child = new DefaultMutableTreeNode( mod.getName() );
          root.add(child);
        }
        modTree = new CheckBoxTree(root);
        modTree.setRootVisible( false );
      } catch (Exception e) {
        LOGGER.error( "Error scanning and building optional mod tree.",e );
        JOptionPane.showMessageDialog( this, "Error scanning mods folder. It may not exists.", "Error", JOptionPane.ERROR_MESSAGE );
        setVisible( false );
        dispose();
        return null;
      }
    }
    return modTree;
  }
  
  private void buildOptionsPane() {
    String msg = InstallScript.preInstallCheck();
    if (msg != null) {
      //TODO maybe a better way to check for collisions
      // so that non-conflicting installs doesn't raise this warning
      msg = msg + "\nThis modpack is meant to be installed on a fresh minecraft.jar and mods folder.\n" +
      		"Do you wish to continue, and attempt to install on top of the current minecraft?";
      int opt = JOptionPane.showConfirmDialog(this,msg,"Installation Warning",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE);
      if (opt == JOptionPane.NO_OPTION) {
        setVisible( false );
        dispose();
      }
    }
    JPanel p = getMainPane();
    p.removeAll();
    p.setLayout( new BoxLayout( p, BoxLayout.Y_AXIS ) );
    p.add( new JLabel("Select the mods you want to install:") );
    p.add(new JScrollPane(getModTree()));
    p.validate();
    p.repaint();
  }
  
  public static void main( String[] args ) throws IOException {
    LOGGER.debug( "Starting Modpack Installer" );
    
    try {
      UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
    } catch (Exception e) {
      LOGGER.warn( "Couldn't set the look and feel", e );
    }
    
    Installer installer = new Installer();
    installer.setVisible( true );
  }
}
