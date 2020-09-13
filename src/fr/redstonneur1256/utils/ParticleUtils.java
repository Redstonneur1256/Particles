package fr.redstonneur1256.utils;

import fr.redstonneur1256.redutilities.io.JDownload;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import java.util.regex.Pattern;

public class ParticleUtils {

    private static final Pattern urlPattern;
    static {
        urlPattern = Pattern.compile("^(?i)https?://[a-z0-9]*.[a-z]*.*$");
    }

    public static BufferedImage getDesktopImage() throws IOException {
        Process process = Runtime.getRuntime().exec("reg query \"HKEY_CURRENT_USER\\Control Panel\\Desktop\" /v Wallpaper");
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
        String line;
        String path = null;
        while((line = reader.readLine()) != null) {
            if(line.startsWith("    Wallpaper    REG_SZ    ")) {
                path = line.substring("    Wallpaper    REG_SZ    ".length());
                break;
            }

        }
        if(path == null) {
            throw new IllegalStateException("Cannot get current user wallpaper");
        }
        if(path.indexOf('�') >= 0) { // FIXME
            throw new IllegalStateException("Your wallpaper path contains invalid characters");
        }
        reader.close();
        reader.close();
        process.destroy();
        return ImageIO.read(new File(path));
    }

    public static File getJarFile() {
        try {
            String path = ParticleUtils.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            String decodedPath = URLDecoder.decode(path, "UTF-8");
            if(!decodedPath.endsWith(".jar")) {
                return new File(".");
            }
            return new File(decodedPath);
        }catch(Exception exception) {
            return new File("Particles.jar");
        }
    }

    public static BufferedImage readImage(String path, Function<JDownload, JDownload.Listener> listenerCreator) throws Exception {
        BufferedImage image;
        if(isURL(path)) {
            System.out.println("Downloading image from " + path + "...");

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            JDownload download = new JDownload(path, output);
            download.setProperty("User-Agent", "JParticles");
            if(listenerCreator != null) {
                download.addListener(listenerCreator.apply(download));
            }
            download.connect(true);
            download.download();

            ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
            image = ImageIO.read(input);
            input.close();

        }else if(path.equalsIgnoreCase("desktop")) {
            image = getDesktopImage();
        }else if(path.equalsIgnoreCase("null")) {
            image = null;
        }else {
            File file = new File(path);
            if(!file.exists() || !file.canRead())
                throw new IllegalStateException("Cannot read the file or it doesn't exists.");
            image = ImageIO.read(file);
        }
        return image;
    }

    public static boolean isURL(String path) {
        return urlPattern.matcher(path).matches();
    }

    public static void async(Runnable run) {
        new Thread(run).start();
    }

    public static String errorMessage(Throwable throwable) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(output);
        throwable.printStackTrace(writer);
        writer.close();
        return new String(output.toByteArray());
    }

}
