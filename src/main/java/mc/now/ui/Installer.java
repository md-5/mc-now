package mc.now.ui;

import com.jidesoft.swing.CheckBoxTree;
import com.jidesoft.swing.CheckBoxTreeSelectionModel;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import mc.now.util.InstallScript;
import mc.now.util.InstallerConfig;
import net.miginfocom.layout.AC;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

/**
 * The main GUI used to install mods.
 */
@SuppressWarnings("serial")
public class Installer extends JFrame implements ActionListener, TreeSelectionListener, HyperlinkListener {

    private static final Logger LOGGER = Logger.getLogger(Installer.class);
    private static final String PRESET_NONE = "Minimum Install";
    private static final String PRESET_ALL = "Full Install";
    private static final String PRESET_CUSTOM = "Custom";
    private JButton nextButton;
    private JButton cancelButton;
    private JProgressBar progressBar;
    private int step = 0;
    private JPanel contentPane;
    private CheckBoxTree modTree;
    private JComboBox presetDropdown;
    private JPanel modDescrPane;
    private DefaultMutableTreeNode modTreeRoot;
    private JButton targetButton;
    private Map<String, List<String>> presetMap;
    private Map<String, DefaultMutableTreeNode> treeNodeMap;

    public Installer() {
        super(InstallerConfig.getFrameTitle());
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(800, 600));
        init();
        pack();
    }

    private void init() {
        JPanel p = new JPanel();
        p.setLayout(new MigLayout("fill", "[fill,grow|]", "[fill,grow||]")); //lc col row
        p.add(getMainPane(), new CC().spanX().grow());
        p.add(getProgressBar(), new CC().growX());
        p.add(getCancelButton(), new CC().alignX("right"));
        p.add(getNextButton(), new CC().alignX("right"));
        setContentPane(p);
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
            initialPanel(contentPane);
        }
        return contentPane;
    }

    protected void initialPanel(JPanel contentPane) {
        JLabel text = new JLabel();
        try {
            ImageIcon icon = new ImageIcon(ImageIO.read(new FileInputStream(InstallerConfig.getLogoFile())));
            text.setIcon(icon);
        } catch (IOException e) {
            LOGGER.error("IO error on logo.png", e);
        }
        StringBuilder textBuffer = new StringBuilder();
        try {
            BufferedReader r = new BufferedReader(new FileReader(InstallerConfig.getInitTextFile()));
            String line = null;
            while ((line = r.readLine()) != null) {
                textBuffer.append(line + "\n");
            }
        } catch (IOException ioe) {
            LOGGER.error("IO error on logo.png", ioe);
        }
        text.setText(textBuffer.toString());
        text.setVerticalTextPosition(JLabel.BOTTOM);
        text.setHorizontalTextPosition(JLabel.CENTER);
        contentPane.setLayout(new MigLayout(new LC().fill()));
        contentPane.add(text, new CC().alignX("center").wrap());
        contentPane.add(getTargetButton(), new CC().alignX("center").wrap());
    }

    private JButton getTargetButton() {
        if (targetButton == null) {
            targetButton = new JButton("Set Target Minecraft Folder...");
            targetButton.addActionListener(this);
        }
        return targetButton;
    }

    private void chooseTargetMinecraftFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setMultiSelectionEnabled(false);
        int opt = chooser.showOpenDialog(getMainPane());
        if (opt == JFileChooser.APPROVE_OPTION) {
            File dir = chooser.getSelectedFile();
            String oldDir = InstallerConfig.getMinecraftFolder();
            InstallerConfig.setMinecraftFolder(dir.getAbsolutePath());
            File mcjar = new File(InstallerConfig.getMinecraftJar());
            if (!mcjar.exists()) {
                JOptionPane.showMessageDialog(getMainPane(), "The installer couldn't find a minecraft installation in the specified folder.\n"
                        + "Restoring minecraft folder to " + oldDir,
                        "Error setting target Minecraft installation", JOptionPane.ERROR_MESSAGE);
                InstallerConfig.setMinecraftFolder(oldDir);
            }
        }

    }

    private void advanceStep() {
        step++;
        switch (step) {
            case 1: {
                String msg = InstallScript.preInstallCheck();
                if (msg != null) {
                    //TODO maybe a better way to check for collisions
                    // so that non-conflicting installs doesn't raise this warning
                    msg = msg + "\nThis modpack is meant to be installed on a fresh minecraft.jar and mods folder.\n"
                            + "Do you wish to continue, and attempt to install on top of the current minecraft?";
                    int opt = JOptionPane.showConfirmDialog(this, msg, "Installation Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (opt == JOptionPane.NO_OPTION) {
                        setVisible(false);
                        dispose();
                    }
                }
                File extraFolder = new File(InstallerConfig.getExtraModsFolder());
                //Check that the extras folder exists and that it has some mods in it. Otherwise, simply start installing
                if (extraFolder.exists()) {
                    File[] children = extraFolder.listFiles();
                    if (children != null) {
                        for (File child : children) {
                            if (child.isDirectory()) {
                                buildOptionsPane();
                                return;
                            }
                        }
                    }
                    //extras folder was empty, or no mod directories (only files)
                    advanceStep();
                } else {
                    //extras folder didn't exist.
                    advanceStep();
                }
                return;
            }
            case 2:
                buildInstallingPane();
                return;
            default:
                LOGGER.error("Advanced too far");
        }
    }

    private void buildInstallingPane() {

        getNextButton().setEnabled(false);
        getCancelButton().setEnabled(false);
        getMainPane().removeAll();
        getMainPane().setLayout(new MigLayout(new LC().fill()));
        final JTextArea text = new JTextArea();
        text.setEditable(false);
        getMainPane().add(new JScrollPane(text), new CC().grow().spanY().wrap());
        getMainPane().validate();
        getMainPane().repaint();
        SwingWorker<Object, String> worker = new SwingWorker<Object, String>() {

            @Override
            protected Object doInBackground() throws Exception {
                try {
                    List<String> mods = new ArrayList<String>();
                    CheckBoxTreeSelectionModel select = getModTree().getCheckBoxTreeSelectionModel();
                    TreePath[] paths = select.getSelectionPaths();
                    if (paths != null && paths.length > 0) {
                        for (TreePath path : paths) {
                            DefaultMutableTreeNode node = ((DefaultMutableTreeNode) path.getLastPathComponent());
                            String mod = (String) node.getUserObject();
                            if (mod == null) {
                                for (int i = 0; i < node.getChildCount(); i++) {
                                    DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
                                    mods.add((String) child.getUserObject());
                                }
                            } else {
                                mods.add(mod);
                            }
                        }
                    }
                    InstallScript.guiInstall(mods, text, getProgressBar());
                } catch (Exception e) {
                    LOGGER.error("Error while trying to install mods", e);
                    JOptionPane.showMessageDialog(Installer.this, "Unexpected error while installing mods:\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    setVisible(false);
                    dispose();
                }
                return null;
            }

            @Override
            public void done() {
                getNextButton().removeActionListener(Installer.this);
                getNextButton().setText("Done");
                getNextButton().addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        setVisible(false);
                        dispose();
                    }
                });
                getNextButton().setEnabled(true);
            }
        };

        worker.execute();
    }

    private CheckBoxTree getModTree() {
        if (modTree == null) {
            modTreeRoot = new DefaultMutableTreeNode();
            treeNodeMap = new HashMap<String, DefaultMutableTreeNode>();
            try {
                File opt = new File(InstallerConfig.getExtraModsFolder());
                for (File mod : opt.listFiles()) {
                    if (!mod.isDirectory()) {
                        continue;
                    }
                    String modName = mod.getName();
                    DefaultMutableTreeNode child = new DefaultMutableTreeNode(modName);
                    treeNodeMap.put(modName, child);
                    modTreeRoot.add(child);
                }
                modTree = new CheckBoxTree(modTreeRoot);
                modTree.setRootVisible(false);
                modTree.getSelectionModel().addTreeSelectionListener(this);
                modTree.getCheckBoxTreeSelectionModel().addTreeSelectionListener(this);
            } catch (Exception e) {
                LOGGER.error("Error scanning and building optional mod tree.", e);
                JOptionPane.showMessageDialog(this, "Error scanning mods folder. It may not exists.", "Error", JOptionPane.ERROR_MESSAGE);
                setVisible(false);
                dispose();
                return null;
            }
        }
        return modTree;
    }

    private void buildOptionsPane() {

        JPanel p = getMainPane();
        p.removeAll();

        JPanel leftPane = new JPanel(new MigLayout(new LC().fill(),
                new AC().index(1).fill().grow(),
                new AC().index(0).fill().grow().index(1).fill().grow()));
        leftPane.add(new JLabel("Please select which optional mods you would like to install."), new CC().spanX().wrap());
        leftPane.add(new JLabel("Presets: "), new CC());
        leftPane.add(getPresetDropdown(), new CC().grow().wrap());
        leftPane.add(new JScrollPane(getModTree()), new CC().spanX().spanY().grow().wrap());

        p.setLayout(new MigLayout(new LC().fill(),
                new AC().index(0).fill().grow().index(1).fill().grow(),
                new AC().fill()));
        p.add(leftPane, new CC().grow().width(":300:"));
        p.add(getModDescriptionPane(), new CC().grow().width(":300:300").wrap());

        p.validate();
        p.repaint();
    }

    private JComboBox getPresetDropdown() {
        if (presetDropdown == null) {
            presetDropdown = new JComboBox();
            presetDropdown.addItem(PRESET_NONE);
            presetDropdown.addItem(PRESET_ALL);
            presetDropdown.addItem(PRESET_CUSTOM);
            presetDropdown.addActionListener(this);
            File presetDir = new File(FilenameUtils.concat(InstallerConfig.getInstallerDir(), "config/presets"));
            if (!presetDir.exists() || !presetDir.isDirectory()) {
                LOGGER.warn("presets does not exist or is not a directory");
            } else {
                presetMap = new HashMap<String, List<String>>();
                File[] children = presetDir.listFiles();
                for (File child : children) {
                    if (!child.isFile()) {
                        continue;
                    }
                    String name = FilenameUtils.getBaseName(child.getName());
                    try {
                        BufferedReader r = new BufferedReader(new FileReader(child));
                        String l = null;
                        List<String> mods = new ArrayList<String>();
                        while ((l = r.readLine()) != null) {
                            mods.add(l);
                        }
                        presetMap.put(name, mods);
                        presetDropdown.addItem(name);
                    } catch (IOException e) {
                        LOGGER.warn("Error reading " + child.getName(), e);
                    }
                }
            }
        }
        return presetDropdown;
    }

    private JPanel getModDescriptionPane() {
        if (modDescrPane == null) {
            modDescrPane = new JPanel();
            modDescrPane.add(new JLabel("<html>This is the mod description panel.<br>"
                    + "Select an optional mod to the left<br>"
                    + "to view a description of it.</html>"));
        }
        return modDescrPane;
    }

    private void loadModDescription(String modName) {
        JPanel p = getModDescriptionPane();
        p.removeAll();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        final String extras = InstallerConfig.getExtraModsFolder();
        final String modFolderName = FilenameUtils.concat(extras, modName);
        File modFolder = new File(modFolderName);
        if (!modFolder.exists()) {
            LOGGER.error("Mod folder for " + modName + " does not exist.");
        }
        File descrFile = new File(FilenameUtils.concat(modFolderName, "description.txt"));
        File imgFile = new File(FilenameUtils.concat(modFolderName, "image.png"));
        if (!descrFile.exists() && !imgFile.exists()) {
            p.add(new JLabel("<html>No description for:<br>" + modName + "</html>"));
        } else {
            if (imgFile.exists()) {
                try {
                    JLabel label = new JLabel();
                    BufferedImage img = ImageIO.read(imgFile);
                    label.setIcon(new ImageIcon(img));
                    p.add(label);
                } catch (IOException e) {
                    LOGGER.error("Error reading image file: " + imgFile.getPath(), e);
                }
            }
            if (descrFile.exists()) {
                StringBuilder buffer = new StringBuilder();
                try {
                    BufferedReader r = new BufferedReader(new FileReader(descrFile));
                    String l = null;
                    while ((l = r.readLine()) != null) {
                        buffer.append(l + "\n");
                    }
                    r.close();
                    JEditorPane area = new JEditorPane();
                    area.setContentType("text/html");
                    area.setText(buffer.toString());
                    area.setEditable(false);
                    area.addHyperlinkListener(this);
                    area.setCaretPosition(0);
                    p.add(new JScrollPane(area));
                } catch (IOException e) {
                    LOGGER.error("Error reading description file: " + descrFile.getPath(), e);
                }
            }
        }

        p.validate();
        p.repaint();
    }

    private void loadPreset(String presetName) {
        if (presetName.equals(PRESET_NONE)) {
            getModTree().getCheckBoxTreeSelectionModel().clearSelection();
        } else if (presetName.equals(PRESET_ALL)) {
            CheckBoxTreeSelectionModel selectModel = getModTree().getCheckBoxTreeSelectionModel();
            selectModel.setSelectionPath(new TreePath(modTreeRoot));
        } else {
            loadPresetFromFile(presetName);
        }
    }

    private void loadPresetFromFile(String presetName) {
        List<String> mods = presetMap.get(presetName);
        CheckBoxTreeSelectionModel select = getModTree().getCheckBoxTreeSelectionModel();
        select.clearSelection();
        TreePath[] paths = new TreePath[mods.size()];
        for (int i = 0; i < mods.size(); i++) {
            String mod = mods.get(i);
            DefaultMutableTreeNode modNode = treeNodeMap.get(mod);
            paths[i] = new TreePath(new Object[]{modTreeRoot, modNode});
        }
        select.setSelectionPaths(paths);
        presetDropdown.setSelectedItem(presetName);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == getNextButton()) {
            advanceStep();
        } else if (e.getSource() == getCancelButton()) {
            setVisible(false);
            dispose();
        } else if (e.getSource() == getPresetDropdown()) {
            loadPreset(getPresetDropdown().getSelectedItem().toString());
        } else if (e.getSource() == getTargetButton()) {
            chooseTargetMinecraftFolder();
        }
    }

    @Override
    public void valueChanged(TreeSelectionEvent e) {
        if (!e.isAddedPath()) {
            return;
        }
        TreePath path = e.getPath();
        CheckBoxTree tree = getModTree();
        if (e.getSource() == tree.getSelectionModel()) {
            DefaultMutableTreeNode last = (DefaultMutableTreeNode) path.getLastPathComponent();
            loadModDescription(last.getUserObject().toString());
        } else if (e.getSource() == tree.getCheckBoxTreeSelectionModel()) {
            getPresetDropdown().setSelectedItem(PRESET_CUSTOM);
        }
    }

    @Override
    public void hyperlinkUpdate(final HyperlinkEvent e) {
        if (e.getEventType() != HyperlinkEvent.EventType.ACTIVATED) {
            return;
        }
        SwingWorker<Object, Object> worker = new SwingWorker<Object, Object>() {

            @Override
            protected Object doInBackground() throws Exception {
                try {
                    String url = e.getURL().toExternalForm();
                    Desktop.getDesktop().browse(URI.create(url));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
        worker.execute();
    }

    public static boolean sanityCheck() {
        //Check for some required stuff
        File mcFolder = new File(InstallerConfig.getMinecraftFolder());
        String errmsg = "";
        if (InstallerConfig.getInstallerDir().toLowerCase().endsWith(".zip")) {
            //TODO is this the right check?
            errmsg += "You must extract the contents of the zip file to a folder before running the installer.";
            return false;
        }
//    if (!mcFolder.exists()) {
//      errmsg += InstallerConfig.getMinecraftFolder() + " doesn't exist.\n";
//    }
        File modsFolder = new File(InstallerConfig.getInstallerModsFolder());
        if (!modsFolder.exists()) {
            errmsg += InstallerConfig.getInstallerModsFolder() + " doesn't exist.\n";
        }
        File logofile = new File(InstallerConfig.getInitTextFile());
        if (!logofile.exists()) {
            errmsg += InstallerConfig.getInitTextFile() + " doesn't exist.\n";
        }
        File textfile = new File(InstallerConfig.getLogoFile());
        if (!textfile.exists()) {
            errmsg += InstallerConfig.getLogoFile() + " doesn't exist.\n";
        }

        errmsg = errmsg.trim();
        if (!errmsg.isEmpty()) {
            LOGGER.error(errmsg);
            JOptionPane.showMessageDialog(null, errmsg, "Installer Error", JOptionPane.ERROR_MESSAGE);
        }
        return errmsg.isEmpty();
    }

    public static void main(String[] args) throws IOException {

        LOGGER.debug("Starting Modpack Installer");
        LOGGER.debug("Current OS: " + InstallerConfig.currentOS);

        if (!sanityCheck()) {
            return;
        }

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            LOGGER.warn("Couldn't set the look and feel", e);
        }

        Installer installer = new Installer();
        installer.setVisible(true);
    }
}
