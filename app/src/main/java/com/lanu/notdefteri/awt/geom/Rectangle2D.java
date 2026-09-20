package com.lanu.notdefteri.awt.geom;

public abstract class Rectangle2D extends RectangularShape {
    protected Rectangle2D() {}

    public static class Double extends Rectangle2D {
        public double x;
        public double y;
        public double width;
        public double height;
        public Double(double x, double y, double w, double h) {
            this.x = x; this.y = y; this.width = w; this.height = h;
        }
        public double getX() { return x; }
        public double getY() { return y; }
        public double getWidth() { return width; }
        public double getHeight() { return height; }
    }
}
