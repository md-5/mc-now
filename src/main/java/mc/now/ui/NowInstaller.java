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

import org.apache.log4j.Logger;

@SuppressWarnings( "serial" )
public class NowInstaller extends Installer {

  private static final Logger LOGGER = Logger.getLogger( NowInstaller.class );
  @Override
  protected void initialPanel(JPanel contentPane) {
    JLabel text = new JLabel();
    try {
      ImageIcon icon = new ImageIcon( ImageIO.read(new FileInputStream( "./logo.png" )) );
      
      text.setIcon( icon );
    } catch ( IOException e ) {
      LOGGER.error( "IO error on logo.png", e );
    }
    StringBuffer textBuffer = new StringBuffer();
    try {
      BufferedReader r = new BufferedReader( new FileReader( "./init_text.txt" ) );
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
  
  public static void main( String[] args ) throws IOException {
    LOGGER.debug( "debugging here" );
    try {
      UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
    } catch (Exception e) {
      LOGGER.warn( "Couldn't set the look and feel", e );
    }
    
    NowInstaller installer = new NowInstaller();
    
    installer.setVisible( true );
  }
  
}
