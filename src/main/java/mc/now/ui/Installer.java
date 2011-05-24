package mc.now.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.apache.log4j.Logger;

import mc.now.util.InstallScript;
import net.miginfocom.layout.CC;
import net.miginfocom.swing.MigLayout;

import com.jidesoft.swing.CheckBoxTree;
import com.jidesoft.swing.CheckBoxTreeSelectionModel;

@SuppressWarnings( "serial" )
public abstract class Installer extends JFrame implements ActionListener {

  private static final Logger LOGGER = Logger.getLogger( Installer.class );
  private JButton nextButton;
  private JButton cancelButton;
  private JProgressBar progressBar;
  
  private int step = 0;
  private JPanel contentPane;
  private CheckBoxTree modTree;

  public Installer() {
    super("Modpack Installer");
    setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
    setSize( 500, 500 );
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
  
  protected abstract void initialPanel(JPanel contentPane);

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
      default: System.err.println("bad step"); return;
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
          for (TreePath path : select.getSelectionPaths()) {
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
          InstallScript.guiInstall(mods,text);
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
      
      @Override
      protected void process(List<String> msgs) {
        for (String msg : msgs) {
          text.append( msg + "\n" );
          text.setCaretPosition( text.getDocument().getLength() );
        }
      }
    };
    
    worker.execute();
  }

  
  private CheckBoxTree getModTree() {
    if (modTree == null) {
      DefaultMutableTreeNode root = new DefaultMutableTreeNode();
      try {
        File opt = new File("./mods/extra/");
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
    JPanel p = getMainPane();
    p.removeAll();
    p.setLayout( new BoxLayout( p, BoxLayout.Y_AXIS ) );
    
    p.add( new JLabel("Select the mods you want to install:") );
    p.add(new JScrollPane(getModTree()));
    p.validate();
    p.repaint();
  }
}
