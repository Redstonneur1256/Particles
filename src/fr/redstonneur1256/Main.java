package fr.redstonneur1256;

import com.formdev.flatlaf.FlatDarculaLaf;
import fr.redstonneur1256.frame.ParticleFrame;
import fr.redstonneur1256.frame.SettingsFrame;
import fr.redstonneur1256.particles.ParticlesPanel;
import fr.redstonneur1256.redutilities.io.JDownload;
import fr.redstonneur1256.utils.ParticleUtils;
import joptsimple.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        try {
            FlatDarculaLaf.install();

            OptionParser parser = new OptionParser();
            parser.allowsUnrecognizedOptions();
            parser.accepts("?");
            OptionSpec<Integer> width = parser.accepts("width").withRequiredArg().ofType(Integer.class).defaultsTo(720);
            OptionSpec<Integer> height = parser.accepts("height").withRequiredArg().ofType(Integer.class).defaultsTo(480);
            OptionSpec<Integer> frameRate = parser.accepts("frameRate").withRequiredArg().ofType(Integer.class).defaultsTo(30);
            OptionSpec<Integer> particleAmount = parser.accepts("amount").withRequiredArg().ofType(Integer.class).defaultsTo(100);
            OptionSpec<Integer> distance = parser.accepts("distance").withRequiredArg().ofType(Integer.class).defaultsTo(75);
            OptionSpec<Integer> particleWidth = parser.accepts("size").withRequiredArg().ofType(Integer.class).defaultsTo(10);
            OptionSpec<Integer> chromaSteps = parser.accepts("chromaSteps").withRequiredArg().ofType(Integer.class).defaultsTo(50);
            OptionSpec<ParticlesPanel.LineType> type = parser.accepts("line").withRequiredArg().ofType(ParticlesPanel.LineType.class).defaultsTo(ParticlesPanel.LineType.color);
            OptionSpec<String> backgroundColor = colorParser(parser, "backColor", Color.BLACK);
            OptionSpec<String> lineColor = colorParser(parser, "lineColor", Color.GRAY);
            OptionSpec<String> ballColor = colorParser(parser, "ballColor", Color.WHITE);
            parser.accepts("fullscreen");
            parser.accepts("useback");
            parser.accepts("debug");
            OptionSpec<String> backgroundPath = parser.accepts("backimg").withRequiredArg().ofType(String.class);
            OptionSpec<String> ignored = parser.nonOptions();
            OptionSet options = parser.parse(args);

            List<String> strings = options.valuesOf(ignored);
            System.out.println("Ignored arguments " + strings);


            if(options.has("?")) {
                parser.printHelpOn(System.out);
            }


            BufferedImage background = options.has(backgroundPath) ? ParticleUtils.readImage(backgroundPath.value(options),
                    download -> new JDownload.ListenerAdapter() {
                        @Override
                        public void speedChanged(long speed) {
                            System.out.print(download.createBar(50, "-", " ") + "\r");
                        }

                        @Override
                        public void downloadComplete() {
                            System.out.println(download.createBar(50, "-", " "));
                        }
                    }) : null;

            BufferedImage icon = ImageIO.read(Main.class.getResourceAsStream("resources/settings.png"));

            ParticlesPanel panel = new ParticlesPanel();


            panel.setStepsPerColor(chromaSteps.value(options));

            panel.setLineType(type.value(options));

            panel.setParticleSize(particleWidth.value(options));

            panel.setBackgroundColor(parseColor(backgroundColor.values(options)));
            panel.setParticleColor(parseColor(ballColor.values(options)));
            panel.setLineColor(parseColor(lineColor.values(options)));

            panel.setUseBackgroundImage(options.has("debug"));
            panel.setBackgroundImage(background);

            panel.setLineDistance(distance.value(options));
            panel.setFramesPerSecond(frameRate.value(options));
            panel.setDebugEnabled(options.has("debug"));

            SettingsFrame settingsFrame = new SettingsFrame(panel);
            settingsFrame.setIconImage(icon);
            settingsFrame.getBackgroundPath().setText(backgroundPath.value(options));

            panel.setSize(width.value(options), height.value(options));

            ParticleFrame frame = new ParticleFrame(panel, settingsFrame);
            frame.setSize(width.value(options), height.value(options));
            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setIconImage(ImageIO.read(Main.class.getResourceAsStream("resources/icon.png")));
            if(options.has("fullscreen")) {
                frame.setFullScreen();
            }
            frame.setVisible(true);

            settingsFrame.setFrame(frame);

            panel.setParticleCount(particleAmount.value(options));

            panel.start();

        }catch(Exception exception) {
            exception.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error while running application.\n" +
                    ParticleUtils.errorMessage(exception), "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private static OptionSpec<String> colorParser(OptionParser parser, String name, Color color) {
        return parser.accepts(name).withRequiredArg().withValuesSeparatedBy(',')
                .defaultsTo(String.valueOf(color.getRed()), String.valueOf(color.getGreen()), String.valueOf(color.getBlue()));
    }

    private static Color parseColor(List<String> input) {
        try {
            int red = Integer.parseInt(input.get(0));
            int green = Integer.parseInt(input.get(1));
            int blue = Integer.parseInt(input.get(2));
            return new Color(red, green, blue);
        }catch(Exception exception) {
            throw new IllegalStateException("Failed to parse color for input \"" + input + "\"");
        }
    }

}
