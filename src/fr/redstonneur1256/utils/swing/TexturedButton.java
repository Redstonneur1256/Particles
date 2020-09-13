package fr.redstonneur1256.utils.swing;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class TexturedButton extends JComponent implements MouseListener {

    private Image image;
    private Image hoverImage;
    private boolean hover;
    private List<Runnable> listeners;

    public TexturedButton(BufferedImage image) {
        this(image, createHover(image));
    }

    public TexturedButton(Image image, Image hover) {
        this.image = image;
        this.hoverImage = hover;
        this.hover = false;
        this.listeners = new ArrayList<>();
        this.addMouseListener(this);
    }

    private static Image createHover(BufferedImage image) {
        BufferedImage hover = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        Graphics graphics = hover.getGraphics();
        graphics.drawImage(image, 0, 0, null);
        graphics.setColor(new Color(255, 255, 255, 50));
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.dispose();
        return hover;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Image image = isHover() && this.hoverImage != null ? this.hoverImage : this.image;
        g.drawImage(image, 0, 0, getWidth(), getHeight(), this);
    }

    public void addActionListener(Runnable listener) {
        this.listeners.add(listener);
    }

    public boolean isHover() {
        return hover;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if(isEnabled() && isHover()) {
            listeners.forEach(Runnable::run);
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        hover = true;
        repaint();
    }

    @Override
    public void mouseExited(MouseEvent e) {
        hover = false;
        repaint();
    }

}