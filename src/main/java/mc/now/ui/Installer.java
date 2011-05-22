package mc.now.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import com.jidesoft.swing.CheckBoxTree;
import com.jidesoft.swing.CheckBoxTreeSelectionModel;

import mc.now.util.InstallScript;
import net.miginfocom.layout.CC;
import net.miginfocom.swing.MigLayout;

@SuppressWarnings( "serial" )
public class Installer extends JFrame implements ActionListener {

  private JButton nextButton;
  private JButton cancelButton;
  private JProgressBar progressBar;
  
  private int step = 0;
  private JPanel contentPane;
  private CheckBoxTree modTree;

  public Installer() throws IOException {
    super("Minecraft NOW Modpack Installer");
    setResizable( false );
    setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
    setSize( 500, 500 );
    init();
  }
  
  private void init() throws IOException {
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
      JLabel text = new JLabel();
      try {
        ImageIcon icon = new ImageIcon( ImageIO.read(ClassLoader.getSystemResourceAsStream( "NOW_Logo.png" )) );
        text.setIcon( icon );
      } catch ( IOException e ) {
        e.printStackTrace();
      }
      text.setText( "<html><center><h3>Minecraft NOW! Modpack v5 Installer</h3><br>" +
          "This will install the the NOW! modpack v5 for Minecraft 1.5_01<br>" +
          "We are not responsible when you lose all your giant obsidian dongs.<br>" +
          "This will only work with a fresh minecraft.jar and mod folder." +
          "</center></html>" );
      text.setVerticalTextPosition(JLabel.BOTTOM);
      text.setHorizontalTextPosition(JLabel.CENTER);
      contentPane.add( text );
    }
    return contentPane;
  }
  
  public static void main( String[] args ) throws IOException {
    
    try {
      UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    Installer installer = new Installer();
    
    installer.setVisible( true );
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
      case 1: buildStepOnePane(); return;
      case 2: buildStepTwoPane(); return;
      default: System.err.println("bad step"); return;
    }
  }

  private void buildStepTwoPane() {
    
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
        List<String> mods = new ArrayList<String>();
        CheckBoxTreeSelectionModel select = getModTree().getCheckBoxTreeSelectionModel();
        for (TreePath path : select.getSelectionPaths()) {
          String mod = (String)((DefaultMutableTreeNode)path.getLastPathComponent()).getUserObject();
          mods.add(mod);
        }
        InstallScript.guiInstall(mods,text);
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
      File opt = new File("./mods/extra/");//TODO
      for (File mod : opt.listFiles()) {
        if (!mod.isDirectory()) {
          continue;
        }
        root.add( new DefaultMutableTreeNode( mod.getName() ) );
      }
      modTree = new CheckBoxTree(root);
      modTree.setRootVisible( false );
    }
    return modTree;
  }
  
  private void buildStepOnePane() {
    JPanel p = getMainPane();
    p.removeAll();
    p.setLayout( new BoxLayout( p, BoxLayout.Y_AXIS ) );
    
    p.add( new JLabel("Select the mods you want to install:") );
    p.add(new JScrollPane(getModTree()));
    p.validate();
    p.repaint();
  }
}
