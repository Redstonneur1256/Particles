package fr.redstonneur1256.utils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.List;
import java.util.*;

public class Timing {

    private static final DecimalFormat format = new DecimalFormat("#.#");

    private Map<String, SubTiming> timingMap = new HashMap<>();

    public static String format(Timing timings) {
        String[] output = new String[timings.timingMap.size()];
        int i = 0;
        for(SubTiming timing : timings.timingMap.values()) {
            output[i++] = timing.name + " " + timing.getLastSecond() + "/" + timing.getAverage();
        }
        return "Last/Avg " + String.join(" | ", output);
    }

    public static BufferedImage graphic(int w, int h, Timing timings) {
        BufferedImage image = new BufferedImage(w, h + timings.timingMap.size() * 21, BufferedImage.TYPE_INT_ARGB);
        Graphics g = image.getGraphics();
        g.setFont(g.getFont().deriveFont(Font.BOLD));
        double total = 0, value = 0, nameY = h + 20;
        for(SubTiming timing : timings.timingMap.values())
            total += timing.getLastSecond();
        for(SubTiming timing : timings.timingMap.values()) {
            int startAngle = (int) Math.round(value * 360.0 / total);
            int angle = (int) Math.round((timing.getLastSecond() * 360.0 / total) + 0.5);
            g.setColor(timing.color);
            g.fillArc(0, 0, w, h, startAngle, angle);
            g.drawString(timing.name + " " + timing.getLastSecond() + "ms", 0, (int) nameY);
            nameY += 20;
            value += timing.getLastSecond();
        }
        return image;
    }

    private static double check(double d) {
        return Double.isNaN(d) || Double.isInfinite(d) ? 0 : d;
    }

    public void start(String id) {
        timingMap.get(id).start();
    }

    public void stop(String id) {
        timingMap.get(id).stop();
    }

    public void registerNew(String id, String name, Color color) {
        timingMap.put(id, new SubTiming(name, color));
    }

    public void reset() {
        for(SubTiming timing : timingMap.values()) {
            timing.reset();
        }
    }

    private static class SubTiming {
        private String name;
        private Color color;
        private boolean running;
        private long start;
        private List<Long> values = new ArrayList<>();
        private double lastSecond, average;

        public SubTiming(String name, Color color) {
            this.name = name;
            this.color = color;
        }

        public SubTiming(String name) {
            this(name, new Color(new Random().nextInt()));
        }

        public void start() {
            //   System.out.println("started " + name);
            if(running)
                stop();
            start = System.currentTimeMillis();
            running = true;
        }

        public void stop() {
            //     System.out.println("Stopped " + name + " Time > " + (System.currentTimeMillis()-start));
            if(!running)
                return;
            running = false;
            values.add(System.currentTimeMillis() - start);
            start = 0;
        }

        public String getAverage() {
            return format.format(average);
        }

        public double getAverageValue() {
            return average;
        }

        public long getLastSecond() {
            return (long) lastSecond;
        }

        public void reset() {
            lastSecond = 0;
            for(Long l : values)
                lastSecond += l;
            lastSecond = check(lastSecond);
            average = lastSecond / values.size();
            values.clear();
            average = check(average);
        }
    }
}