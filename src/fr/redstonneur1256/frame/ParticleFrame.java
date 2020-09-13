package fr.redstonneur1256.frame;

import fr.redstonneur1256.Main;
import fr.redstonneur1256.particles.ParticlesPanel;
import fr.redstonneur1256.utils.swing.ComponentResizer;
import fr.redstonneur1256.utils.swing.TexturedButton;
import fr.redstonneur1256.utils.swing.WindowMover;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class ParticleFrame extends JFrame {

    private SettingsFrame settingsFrame;
    private boolean fullscreen;
    private Rectangle before;

    public ParticleFrame(ParticlesPanel particlesPanel, SettingsFrame settingsPanel) throws Exception {
        this.settingsFrame = settingsPanel;

        Panel panel = new Panel(particlesPanel, settingsPanel);

        super.setUndecorated(true);
        super.setContentPane(panel);

        ComponentResizer resizer = new ComponentResizer(this);
        resizer.registerComponent(this);

        WindowMover mover = new WindowMover(this);
        mover.apply();

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int code = e.getKeyCode();
                if(code == KeyEvent.VK_F11) {
                    setFullScreen();
                }else if(code == KeyEvent.VK_ESCAPE) {
                    exitFullscreen();
                }
            }
        });
    }

    public void setFullScreen() {
        if(fullscreen)
            return;

        before = getBounds();
        setResizable(false);
        setExtendedState(Frame.MAXIMIZED_BOTH);

        if(settingsFrame.isVisible())
            settingsFrame.setVisible(true);

        fullscreen = true;
    }

    public void exitFullscreen() {
        if(!fullscreen)
            return;

        setResizable(true);
        setBounds(before);
        fullscreen = false;
    }

    public boolean isFullScreen() {
        return fullscreen;
    }


    private class Panel extends JPanel {

        private ParticlesPanel particlesPanel;
        private List<TexturedButton> buttons;

        public Panel(ParticlesPanel particlesPanel, SettingsFrame settingsPanel) throws Exception {
            setLayout(null);

            buttons = new ArrayList<>();

            BufferedImage settingsImage = ImageIO.read(Main.class.getResourceAsStream("resources/settings.png"));
            BufferedImage informationImage = ImageIO.read(Main.class.getResourceAsStream("resources/information.png"));
            BufferedImage reduceImage = ImageIO.read(Main.class.getResourceAsStream("resources/minimize.png"));
            BufferedImage fullScreenImage = ImageIO.read(Main.class.getResourceAsStream("resources/fullscreen.png"));
            BufferedImage closeImage = ImageIO.read(Main.class.getResourceAsStream("resources/close.png"));

            this.particlesPanel = particlesPanel;

            TexturedButton settingsButton = new TexturedButton(settingsImage);
            settingsButton.addActionListener(() -> settingsPanel.setVisible(!settingsPanel.isVisible()));
            add(settingsButton);
            buttons.add(settingsButton);

            TexturedButton informationButton = new TexturedButton(informationImage);
            informationButton.addActionListener(this::showInformations);
            add(informationButton);
            buttons.add(informationButton);

            TexturedButton reduceButton = new TexturedButton(reduceImage);
            reduceButton.addActionListener(() -> setState(ICONIFIED));
            add(reduceButton);
            buttons.add(reduceButton);

            TexturedButton fullScreenButton = new TexturedButton(fullScreenImage);
            fullScreenButton.addActionListener(ParticleFrame.this::setFullScreen);
            add(fullScreenButton);
            buttons.add(fullScreenButton);

            TexturedButton closeButton = new TexturedButton(closeImage);
            closeButton.addActionListener(() -> System.exit(0));
            add(closeButton);
            buttons.add(closeButton);

            add(particlesPanel);

            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    adjustButton();
                }
            });
        }

        private void showInformations() {
            final String url = "https://github.com/Redstonneur1256/Particles";
            try {
                Desktop.getDesktop().browse(URI.create(url));
            }catch(IOException exception) {
                JOptionPane.showMessageDialog(this, "Failed to open the page\nVisit " +
                        url + " manually instead", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void adjustButton() {
            int pos = fullscreen ? 20 : 0;

            for(int i = 1, max = buttons.size(); i <= max; i++) {
                TexturedButton button = buttons.get(max - i);

                button.setBounds(getWidth() - i * 20, -pos, 20, 20);
            }

            particlesPanel.setBounds(0, 20 - pos, getWidth(), getHeight() - 20 + pos);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);


        }
    }
}