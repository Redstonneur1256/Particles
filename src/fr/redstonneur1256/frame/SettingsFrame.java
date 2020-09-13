package fr.redstonneur1256.frame;

import fr.redstonneur1256.Main;
import fr.redstonneur1256.particles.ParticlesPanel;
import fr.redstonneur1256.redutilities.io.JDownload;
import fr.redstonneur1256.utils.ImageFilter;
import fr.redstonneur1256.utils.ParticleUtils;
import fr.redstonneur1256.utils.swing.TexturedButton;
import fr.redstonneur1256.utils.swing.WindowMover;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;

public class SettingsFrame extends JFrame {

    private ParticlesPanel panel;
    private File lastPath;

    private JLabel backGroundLabel;
    private JTextField backgroundPath;
    private JButton setImageButton;
    private JProgressBar downloadBar;
    private ParticleFrame frame;

    public SettingsFrame(ParticlesPanel panel) throws Exception {
        this.panel = panel;
        this.lastPath = new File(".");

        WindowMover mover = new WindowMover(this);
        mover.apply();

        setContentPane(new SettingsPanel());
        setUndecorated(true);
        setResizable(false);
        setSize(400, 400);
        setLayout(null);
        setLocationRelativeTo(null);

        BufferedImage minifyTexture = ImageIO.read(Main.class.getResourceAsStream("resources/minimize.png"));

        TexturedButton minify = new TexturedButton(minifyTexture);
        minify.addActionListener(() -> setVisible(false));
        minify.setBounds(380, 0, 20, 20);
        add(minify);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        JTabbedPane colorSelectionPane = new JTabbedPane();
        colorSelectionPane.setBounds(0, 20, 400, 260);
        colorSelectionPane.add("Background", createColorSelector(panel.getBackgroundColor(), panel::setBackgroundColor));
        colorSelectionPane.add("Lines", createColorSelector(panel.getLineColor(), panel::setLineColor));
        colorSelectionPane.add("Particles", createColorSelector(panel.getParticleColor(), panel::setParticleColor));
        add(colorSelectionPane);

        createLabel("Particle count:", 0, 280, 100);
        JSpinner particleCountSpinner = createSpinner(panel.getParticleCount(), panel::setParticleCount);
        particleCountSpinner.setBounds(100, 280, 75, 20);
        add(particleCountSpinner);

        createLabel("Line distance (pixels):", 175, 280, 150);
        JSpinner distanceSpinner = createSpinner(panel.getLineDistance(), panel::setLineDistance);
        distanceSpinner.setBounds(325, 280, 75, 20);
        add(distanceSpinner);

        createLabel("Line type:", 0, 300, 100);
        JComboBox<ParticlesPanel.LineType> typesList = new JComboBox<>();
        typesList.setBounds(100, 300, 300, 20);
        Arrays.asList(ParticlesPanel.LineType.values()).forEach(typesList::addItem);
        typesList.setSelectedItem(panel.getLineType());
        typesList.addItemListener(e -> panel.setLineType((ParticlesPanel.LineType) typesList.getSelectedItem()));
        add(typesList);

        backGroundLabel = createLabel("Background:", 0, 320, 100);

        backgroundPath = new JTextField("desktop | URL | path");
        backgroundPath.setBounds(100, 320, 300, 20);
        add(backgroundPath);

        JCheckBox useImageCheckBox = createCheckBox("Use background image", panel.isUseBackgroundImage(), panel::setUseBackgroundImage);
        useImageCheckBox.setBounds(0, 340, 200, 20);
        add(useImageCheckBox);

        JButton browseImage = new JButton("Browse");
        browseImage.setBounds(200, 340, 100, 20);
        browseImage.addActionListener(event -> browseImage(backgroundPath::setText));
        add(browseImage);

        setImageButton = new JButton("Set");
        setImageButton.setBounds(300, 340, 100, 20);
        setImageButton.addActionListener(event -> ParticleUtils.async(this::changeImage));
        add(setImageButton);

        downloadBar = new JProgressBar();
        downloadBar.setBounds(0, 320, 400, 20);
        downloadBar.setVisible(false);
        add(downloadBar);

        JCheckBox debugCheckBox = createCheckBox("Debug", panel.isDebugEnabled(), panel::setDebugEnabled);
        debugCheckBox.setBounds(0, 360, 100, 20);
        add(debugCheckBox);

        createLabel("FrameRate:", 100, 360, 100);
        JSpinner frameRateSpinner = createSpinner(panel.getFramesPerSecond(), panel::setFramesPerSecond);
        frameRateSpinner.setBounds(200, 360, 100, 20);
        add(frameRateSpinner);

        JButton exportButton = new JButton("Export");
        exportButton.setBounds(100, 380, 200, 20);
        exportButton.addActionListener(event -> export());
        add(exportButton);
    }

    private void export() {
        File jarFile = ParticleUtils.getJarFile();

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Exporting");
        chooser.setCurrentDirectory(jarFile.getParentFile());
        int code = chooser.showOpenDialog(this);
        if(code == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if(file.exists()) {
                int option = JOptionPane.showConfirmDialog(this, "The target file already exist, overwrite it ?");
                if(option == JOptionPane.NO_OPTION) {
                    export();
                    return;
                }else if(option == JOptionPane.CANCEL_OPTION) {
                    return;
                }
            }
            if(!file.getName().contains(".")) {
                file = new File(file.getAbsolutePath() + ".cmd");
            }
            try {
                PrintWriter writer = new PrintWriter(new FileOutputStream(file), true);
                writer.print("java -jar ");
                writer.print(jarFile.getPath());
                writer.print(" ");
                writer.println(generateCommand());
                writer.close();
                JOptionPane.showMessageDialog(this, "Save done.", "Complete",
                        JOptionPane.INFORMATION_MESSAGE);
            }catch(FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private String generateCommand() {
        StringBuilder builder = new StringBuilder();

        builder.append("-width ").append(panel.getWidth())
                .append(" -height ").append(panel.getHeight())
                .append(" -amount ").append(panel.getParticleCount())
                .append(" -dist ").append(panel.getLineDistance())
                .append(" -line ").append(panel.getLineType().name())
                .append(" -size ").append(panel.getParticleSize())
                .append(" -backimg ").append(backgroundPath.getText())
                .append(" -fps ").append(panel.getFramesPerSecond());
        appendColor(builder, "-backColor", panel.getBackgroundColor());
        appendColor(builder, "-lineColor", panel.getLineColor());
        appendColor(builder, "-ballColor", panel.getParticleColor());
        if(panel.isUseBackgroundImage()) {
            builder.append(" -useback");
        }
        if(panel.isDebugEnabled()) {
            builder.append(" -debug");
        }
        if(frame.isFullScreen()) {
            builder.append(" -fullscreen");
        }
        return builder.toString();
    }

    private void appendColor(StringBuilder builder, String name, Color color) {
        builder.append(" ").append(name).append(" ")
                .append(color.getRed()).append(" ")
                .append(color.getGreen()).append(" ")
                .append(color.getBlue());
    }

    private void browseImage(Consumer<String> onChoice) {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(lastPath);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setFileFilter(new ImageFilter());
        int code = chooser.showOpenDialog(this);
        if(code == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            lastPath = file;
            onChoice.accept(file.getAbsolutePath());
        }
    }

    private void changeImage() {
        try {
            String path = backgroundPath.getText();

            Function<JDownload, JDownload.Listener> listener = null;
            if(ParticleUtils.isURL(path)) {
                downloadBar.setValue(0);
                backGroundLabel.setVisible(false);
                backgroundPath.setVisible(false);
                setImageButton.setEnabled(false);
                downloadBar.setVisible(true);
                downloadBar.setStringPainted(true);
                downloadBar.setString("Downloading...");


                listener = download -> new JDownload.ListenerAdapter() {

                    @Override
                    public void downloadComplete() {
                        backGroundLabel.setVisible(true);
                        backgroundPath.setVisible(true);
                        setImageButton.setEnabled(true);
                        downloadBar.setVisible(false);
                    }

                    @Override
                    public void speedChanged(long speed) {
                        downloadBar.setValue((int) (download.getProgress() * 100));
                    }
                };
            }
            BufferedImage image = ParticleUtils.readImage(path, listener);
            panel.setBackgroundImage(image);
        }catch(Exception exception) {
            JOptionPane.showMessageDialog(this, "Failed to set image:\n" +
                    exception.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JLabel createLabel(String name, int x, int y, int width) {
        JLabel label = new JLabel(name);
        label.setBounds(x, y, width, 20);
        add(label);
        return label;
    }

    private JColorChooser createColorSelector(Color color, Consumer<Color> onEdit) {
        JColorChooser colorChooser = new JColorChooser(color);
        colorChooser.setPreviewPanel(new JPanel());
        AbstractColorChooserPanel[] chooserPanels = colorChooser.getChooserPanels().clone();
        for(AbstractColorChooserPanel colorChooserPanel : chooserPanels) {
            if(colorChooserPanel.getDisplayName().equalsIgnoreCase("TSV")) {
                try {
                    Field panelField = colorChooserPanel.getClass().getDeclaredField("panel");
                    panelField.setAccessible(true);
                    Object panel = panelField.get(colorChooserPanel);
                    Class<?> panelType = panel.getClass();

                    Field spinnersField = panelType.getDeclaredField("spinners");
                    spinnersField.setAccessible(true);
                    Object[] spinners = (Object[]) spinnersField.get(panel);

                    for(Object spinner : spinners) {
                        Field sliderField = spinner.getClass().getDeclaredField("slider");
                        sliderField.setAccessible(true);
                        JSlider slider = (JSlider) sliderField.get(spinner);
                        slider.setVisible(false);

                        Field spinnerField = spinner.getClass().getDeclaredField("spinner");
                        spinnerField.setAccessible(true);
                        JSpinner jspinner = (JSpinner) spinnerField.get(spinner);
                        jspinner.setVisible(false);

                        Field labelField = spinner.getClass().getDeclaredField("label");
                        labelField.setAccessible(true);
                        Component label = (Component) labelField.get(spinner);
                        label.setVisible(false);
                    }
                }catch(Exception exception) {
                    exception.printStackTrace();
                }
            }else {
                colorChooser.removeChooserPanel(colorChooserPanel);
            }
        }
        colorChooser.getSelectionModel().addChangeListener(e -> onEdit.accept(colorChooser.getColor()));
        return colorChooser;
    }

    private JSpinner createSpinner(int value, Consumer<Integer> onEdit) {
        JSpinner spinner = new JSpinner();
        spinner.setValue(value);
        spinner.addChangeListener(event -> onEdit.accept((int) spinner.getValue()));
        return spinner;
    }

    private JCheckBox createCheckBox(String name, boolean value, Consumer<Boolean> onEdit) {
        JCheckBox checkBox = new JCheckBox();
        checkBox.setText(name);
        checkBox.setSelected(value);
        checkBox.addChangeListener(event -> onEdit.accept(checkBox.isSelected()));
        return checkBox;
    }

    public void setFrame(ParticleFrame frame) {
        this.frame = frame;
    }

    public JTextField getBackgroundPath() {
        return backgroundPath;
    }

    private static class SettingsPanel extends JPanel {

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D graphics = (Graphics2D) g;

            graphics.setColor(new Color(0x282828));
            graphics.fillRect(0, 0, 400, 20);

            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(Color.WHITE);
            graphics.drawString("Settings", 0, 15);
        }
    }

}
