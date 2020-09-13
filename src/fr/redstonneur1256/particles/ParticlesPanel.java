package fr.redstonneur1256.particles;

import fr.redstonneur1256.redutilities.Utils;
import fr.redstonneur1256.utils.Timing;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class ParticlesPanel extends JComponent {

    private static final Color[] RAINBOW = new Color[] {
            new Color(0xEE82EE),
            new Color(0x4B0082),
            new Color(0x0000FF),
            new Color(0x039BE5),
            new Color(0x008000),
            new Color(0x8BC34A),
            new Color(0xFFFF00),
            new Color(0xFFA500),
            new Color(0xFF0000)
    };

    private boolean running;
    private int framesPerSecond;
    private long updateTime;
    private long lastUpdateTime;

    private int lastFPS;
    private int toGenerate;
    private boolean clearOnGenerate;

    private BufferedImage buffer;
    private List<Particle> particles;
    private Map<Particle, Map<Particle, FadeColor>> colors;
    private Color particleColor;
    private int particleSize;

    private Color lineColor;
    private LineType lineType;
    private double lineDistanceSquared;
    private Color[] chromaColors;
    private int stepsPerColor;
    private FadeColor chromaColor;

    private Color backgroundColor;
    private BufferedImage backgroundImage;
    private boolean useBackgroundImage;

    private boolean debugEnabled;
    private boolean showFPS;
    private Timing timings;

    public ParticlesPanel() {
        this.running = false;
        this.framesPerSecond = 30;
        this.updateTime = 1_000_000_000L / framesPerSecond;
        this.lastUpdateTime = 0;
        this.lastFPS = 0;
        this.toGenerate = 100;
        this.clearOnGenerate = true;
        this.buffer = new BufferedImage(Math.max(1, getWidth()), Math.max(1, getHeight()), BufferedImage.TYPE_INT_RGB);
        this.particles = new ArrayList<>();
        this.colors = new HashMap<>();
        this.particleColor = Color.WHITE;
        this.particleSize = 8;
        this.lineColor = Color.GRAY;
        this.lineType = LineType.color;
        this.lineDistanceSquared = Math.pow(75, 2);
        this.chromaColors = RAINBOW;
        this.stepsPerColor = 100;
        this.chromaColor = new FadeColor(stepsPerColor, chromaColors);
        this.backgroundColor = Color.BLACK;
        this.backgroundImage = null;
        this.useBackgroundImage = false;
        this.debugEnabled = false;
        this.showFPS = false;
        this.timings = new Timing();

        timings.registerNew("sleep", "Sleep", new Color(0x4CAF50));
        timings.registerNew("tick", "Tick", new Color(0x29B6F6));
        timings.registerNew("clear", "Clear Buffer", new Color(0xd32f2f));
        timings.registerNew("line", "Draw lines", new Color(0xFFC107));
        timings.registerNew("particles", "Draw particles", new Color(0xFF9800));
        timings.registerNew("ui", "Render UI", new Color(0x673AB7));
        timings.registerNew("update", "Display Update", new Color(0x9CCC65));
    }


    public void start() {
        running = true;
        updateChromaColors();
        run();
    }

    public void stop() {
        running = false;
    }

    private void run() {
        lastUpdateTime = System.nanoTime();
        long timer = System.currentTimeMillis();
        int updates = 0;
        while(running) {
            long now = System.nanoTime();
            if(lastUpdateTime <= now + updateTime) {
                update();
                render();
                updates++;
                lastUpdateTime += updateTime;
            }else {
                timings.start("sleep");
                Utils.sleep(1);
                timings.stop("sleep");
            }

            long nowMillis = System.currentTimeMillis();
            if(timer + 1000 <= nowMillis) {
                timer += 1000;
                lastFPS = updates;
                System.out.println(updates + " FPS");
                updates = 0;
                timings.reset();
            }
        }
    }

    private void update() {

        int currentWidth = buffer.getWidth();
        int currentHeight = buffer.getHeight();
        if(getWidth() != currentWidth || getHeight() != currentHeight) {
            Dimension newSize = getSize();
            int newWidth = Math.max(newSize.width, 1);
            int newHeight = Math.max(newSize.height, 1);

            double xRatio = newSize.getWidth() / currentWidth;
            double yRatio = newSize.getHeight() / currentHeight;

            buffer = new BufferedImage(newWidth, newHeight, buffer.getType());
            for(Particle particle : particles) {
                particle.x *= xRatio;
                particle.y *= yRatio;
            }

            currentWidth = newWidth;
            currentHeight = newHeight;
        }

        timings.start("tick");
        for(Particle particle : particles) {
            particle.update(currentWidth, currentHeight);
        }

        if(toGenerate != 0) {
            if(clearOnGenerate)
                particles.clear();
            long start = System.currentTimeMillis();
            Random random = ThreadLocalRandom.current();
            colors.clear(); // New particles
            for(int i = 0; i < toGenerate; i++) {
                try {
                    Particle particle = new Particle();
                    particle.x = Math.max(particleSize, Math.min(currentWidth - particleSize, random.nextFloat() * currentWidth));
                    particle.y = Math.max(particleSize, Math.min(currentHeight - particleSize, random.nextFloat() * currentHeight));
                    particle.dX = random.nextFloat() * (random.nextBoolean() ? -1 : 1);
                    particle.dY = random.nextFloat() * (random.nextBoolean() ? -1 : 1);
                    particles.add(particle);
                }catch(Exception e) {
                    e.printStackTrace();
                    break;
                }
            }
            long end = System.currentTimeMillis();

            System.out.println("Generated " + toGenerate + " particles in " + (end - start) + "ms");
            toGenerate = 0;
        }

        timings.stop("tick");
    }

    private void updateChromaColors() {
        chromaColor.setColors(chromaColors);
        colors.clear();
    }


    private void render() {
        if(buffer == null)
            resizeBuffer(getWidth(), getHeight());
        Graphics2D graphics = (Graphics2D) buffer.getGraphics();

        int width = buffer.getWidth();
        int height = buffer.getHeight();

        timings.start("clear");
        if(useBackgroundImage) {
            graphics.drawImage(backgroundImage, 0, 0, width, height, null);
        }else {
            graphics.setColor(backgroundColor);
            graphics.fillRect(0, 0, width, height);
        }
        timings.stop("clear");

        timings.start("line");
        graphics.setStroke(new BasicStroke(2));

        Color color = lineColor;
        if(lineType == LineType.monoChroma || lineType == LineType.distanceMonoChroma) {
            graphics.setColor(color = chromaColor.fade());
        }

        for(int i = 0; i < particles.size(); i++) {
            Particle particle = particles.get(i);
            for(int j = i + 1; j < particles.size(); j++) {
                Particle other = particles.get(j);

                double distSquared = particle.distanceSquared(other);
                int alpha;
                if(distSquared <= lineDistanceSquared) {
                    switch(lineType) {
                        case color:
                            graphics.setColor(lineColor);
                            break;
                        case distance:
                            alpha = 255 - ((int) (distSquared / lineDistanceSquared * 255));
                            graphics.setColor(new Color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), alpha));
                            break;
                        case chroma:
                            graphics.setColor(getColor(particle, other).fade());
                            break;
                        case distanceChroma:
                            alpha = 255 - ((int) (distSquared / lineDistanceSquared * 255));
                            graphics.setColor(getColor(particle, other).fade(alpha));
                            break;
                        case distanceMonoChroma:
                            alpha = 255 - ((int) (distSquared / lineDistanceSquared * 255));
                            graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
                            break;

                    }
                    graphics.drawLine((int) particle.x, (int) particle.y, (int) other.x, (int) other.y);
                }
            }
        }

        timings.stop("line");

        timings.start("particles");
        for(Particle particle : particles) {
            graphics.setColor(particleColor);
            graphics.fillOval((int) particle.x - particleSize / 2, (int) particle.y - particleSize / 2, particleSize, particleSize);
        }
        timings.stop("particles");

        timings.start("ui");
        if(debugEnabled) {
            int size = (int) Math.max(150, getWidth() * 0.1);
            graphics.drawImage(Timing.graphic(size, size, timings), 0, 0, null);
        }
        if(showFPS) {
            graphics.drawString(lastFPS + " FPS", 0, 0);
        }
        timings.stop("ui");

        timings.start("update");
        getGraphics().drawImage(buffer, 0, 0, getWidth(), getHeight(), null);
        timings.stop("update");

        graphics.dispose();
    }

    private void resizeBuffer(int width, int height) {
        buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    }

    private FadeColor getColor(Particle a, Particle b) {
        if(colors.get(a) == null && colors.get(b) == null) {
            colors.put(a, new HashMap<>());
        }else {
            if(colors.get(b) != null) {
                if(colors.get(b).get(a) == null)
                    colors.get(b).put(a, new FadeColor(stepsPerColor, chromaColors));
                return colors.get(b).get(a);
            }else if(colors.get(a) != null) {
                if(colors.get(a).get(b) == null)
                    colors.get(a).put(b, new FadeColor(stepsPerColor, chromaColors));
                return colors.get(a).get(b);
            }
        }
        return getColor(a, b);
    }

    public boolean isRunning() {
        return running;
    }

    public int getFramesPerSecond() {
        return framesPerSecond;
    }

    public void setFramesPerSecond(int framesPerSecond) {
        this.framesPerSecond = framesPerSecond;
        this.updateTime = 1_000_000_000L / framesPerSecond;
        this.lastUpdateTime = System.nanoTime();
    }

    public int getLastFPS() {
        return lastFPS;
    }

    public int getParticleCount() {
        return particles.size() != toGenerate && toGenerate != 0 ? toGenerate : particles.size();
    }

    public void setParticleCount(int amount) {
        this.toGenerate = amount;
        this.clearOnGenerate = true;
        if(!isRunning())
            update();
    }

    public Color getParticleColor() {
        return particleColor;
    }

    public void setParticleColor(Color particleColor) {
        this.particleColor = particleColor;
    }

    public int getParticleSize() {
        return particleSize;
    }

    public void setParticleSize(int particleSize) {
        this.particleSize = particleSize;
    }

    public Color getLineColor() {
        return lineColor;
    }

    public void setLineColor(Color lineColor) {
        this.lineColor = lineColor;
    }

    public LineType getLineType() {
        return lineType;
    }

    public void setLineType(LineType lineType) {
        this.lineType = lineType;
    }

    public int getLineDistance() {
        return (int) Math.sqrt(lineDistanceSquared);
    }

    public void setLineDistance(double distance) {
        this.lineDistanceSquared = Math.pow(distance, 2);
    }

    public double getLineDistanceSquared() {
        return lineDistanceSquared;
    }

    public void setLineDistanceSquared(double lineDistanceSquared) {
        this.lineDistanceSquared = lineDistanceSquared;
    }

    public Color[] getChromaColors() {
        return chromaColors;
    }

    public void setChromaColors(int... colors) {
        setChromaColors(Arrays.stream(colors).mapToObj(Color::new).toArray(Color[]::new));
    }

    public void setChromaColors(Color[] chromaColors) {
        this.chromaColors = chromaColors;
        this.updateChromaColors();
    }

    public int getStepsPerColor() {
        return stepsPerColor;
    }

    public void setStepsPerColor(int stepsPerColor) {
        this.stepsPerColor = stepsPerColor;
        this.updateChromaColors();
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public BufferedImage getBackgroundImage() {
        return backgroundImage;
    }

    public void setBackgroundImage(BufferedImage backgroundImage) {
        this.backgroundImage = backgroundImage;
    }

    public boolean isUseBackgroundImage() {
        return useBackgroundImage;
    }

    public void setUseBackgroundImage(boolean useBackgroundImage) {
        this.useBackgroundImage = useBackgroundImage;
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public void setDebugEnabled(boolean debugEnabled) {
        this.debugEnabled = debugEnabled;
    }

    public boolean isShowFPS() {
        return showFPS;
    }

    public void setShowFPS(boolean showFPS) {
        this.showFPS = showFPS;
    }

    public enum LineType {
        color,
        distance,
        chroma,
        distanceChroma,
        monoChroma,
        distanceMonoChroma
    }

    private static class FadeColor {
        private Color[] loop;
        private int i = 0, index;
        private Color oldColor, nextColor;
        private int steps;

        FadeColor(int chromaSteps, Color[] colors) {
            this.steps = chromaSteps;
            this.loop = colors;
            index = new Random().nextInt(loop.length - 1) + 1;
            oldColor = loop[index - 1];
            nextColor = loop[index];
        }

        Color fade() {
            return fade(255);
        }

        public Color fade(int alpha) {
            if(oldColor == null)
                oldColor = Color.WHITE;
            int dRed = nextColor.getRed() - oldColor.getRed();
            int dGreen = nextColor.getGreen() - oldColor.getGreen();
            int dBlue = nextColor.getBlue() - oldColor.getBlue();
            if(dRed != 0 || dGreen != 0 || dBlue != 0) {
                Color color = new Color(
                        oldColor.getRed() + ((dRed * i) / steps),
                        oldColor.getGreen() + ((dGreen * i) / steps),
                        oldColor.getBlue() + ((dBlue * i++) / steps),
                        alpha);
                if(i > steps) {
                    i = 0;
                    index++;
                    if(index >= loop.length)
                        index = 0;
                    oldColor = nextColor;
                    nextColor = loop[index];
                }else {
                    return color;
                }
            }
            return alpha == 255 ? oldColor : copyColor(oldColor, alpha);
        }

        private Color copyColor(Color color, int alpha) {
            //return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
            return new Color(((alpha & 0xFF) << 24) | color.getRGB());
        }

        public void setColors(Color[] colors) {
            this.loop = colors;
        }
    }

    private class Particle {
        private double x, y, dX, dY;

        void update(int width, int height) {
            x += dX;
            y += dY;
            int size = particleSize / 2;
            if(x < size || x + size > width) {
                dX *= -1;
                x = Math.max(size, Math.min(width - size, x));
            }
            if(y < size || y + size > height) {
                dY *= -1;
                y = Math.max(size, Math.min(height - size, y));
            }
        }

        double distance(Particle other) {
            return Math.sqrt(distanceSquared(other));
        }

        double distanceSquared(Particle other) {
            return Math.pow(x - other.x, 2) + Math.pow(y - other.y, 2);
        }
    }

}