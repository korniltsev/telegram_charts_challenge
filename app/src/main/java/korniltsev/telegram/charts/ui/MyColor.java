package korniltsev.telegram.charts.ui;

public class MyColor {
    public static void set(float[] parts, int color) {
        parts[0] = ((color >> 16) & 0xFF) / 255f;
        parts[1] = ((color >> 8) & 0xFF) / 255f;
        parts[2] = (color & 0xFF) / 255f;
        parts[3] = (color >>> 24) / 255f;

    }


    public static int alpha(int color) {
        return color >>> 24;
    }


    public static int red(int color) {
        return (color >> 16) & 0xFF;
    }


    public static int green(int color) {
        return (color >> 8) & 0xFF;
    }


    public static int blue(int color) {
        return color & 0xFF;
    }
}
