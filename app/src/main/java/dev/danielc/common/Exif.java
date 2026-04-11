package dev.danielc.common;

public class Exif {
    public native static byte[] getExifThumbnail(String filepath);
}
