package fr.redstonneur1256.utils;

import javax.swing.filechooser.FileFilter;
import java.io.File;

public class ImageFilter extends FileFilter {

    private String[] extensions = new String[] {".png", ".jpg", ".jpeg"};

    @Override
    public boolean accept(File file) {
        if(file.isDirectory())
            return true;
        String name = file.getName().toLowerCase();
        for(String extension : extensions) {
            if(name.endsWith(extension))
                return true;
        }
        return false;
    }

    @Override
    public String getDescription() {
        return "Images (.PNG, .JPG, .JPEG)";
    }
}
