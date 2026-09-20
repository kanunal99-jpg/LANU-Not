package com.lanu.notdefteri.awt;

public class Color {
    private final int value;
    public Color(int r, int g, int b) {
        this.value = ((255 & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | ((b & 0xFF) << 0);
    }
    public int getRGB() { return value; }
}
