package korniltsev.telegram.charts.ui;

import android.graphics.Color;

public class MyColor {
    public static void set(float[] parts, int color) {
        parts[0] = Color.red(color)/ 255f;
        parts[1] = Color.green(color) / 255f;
        parts[2] = Color.blue(color) / 255f;
        parts[3] = Color.alpha(color) / 255f;

    }
}
