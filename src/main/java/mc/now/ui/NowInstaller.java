package mc.now.ui;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

@SuppressWarnings( "serial" )
public class NowInstaller extends Installer {

  @Override
  protected void initialPanel(JPanel contentPane) {
    JLabel text = new JLabel();
    try {
      ImageIcon icon = new ImageIcon( ImageIO.read(new FileInputStream( "./logo.png" )) );
      
      text.setIcon( icon );
    } catch ( IOException e ) {
      e.printStackTrace();
    }
    StringBuffer textBuffer = new StringBuffer();
    try {
      BufferedReader r = new BufferedReader( new FileReader( "./init_text.txt" ) );
      String line = null;
      while ((line = r.readLine()) != null) {
        textBuffer.append( line + "\n" );
      } 
    } catch (IOException ioe) {
      ioe.printStackTrace();
      
    }
    text.setText(textBuffer.toString());
    text.setVerticalTextPosition(JLabel.BOTTOM);
    text.setHorizontalTextPosition(JLabel.CENTER);
    contentPane.add( text );
  }
  
  public static void main( String[] args ) throws IOException {
    
    try {
      UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    NowInstaller installer = new NowInstaller();
    
    installer.setVisible( true );
  }
  
}
