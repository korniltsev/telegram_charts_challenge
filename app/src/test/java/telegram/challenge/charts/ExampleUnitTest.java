package telegram.challenge.charts;

import android.graphics.Color;

import org.junit.Test;

import korniltsev.telegram.charts.ui.ArgbEvaluator;
import korniltsev.telegram.charts.ui.ColorSet;

import static org.junit.Assert.*;


public class ExampleUnitTest {
    @Test
    public void calc() {

        System.out.println("overlay");
        int scroll_overlay = 0xFFF5F8F9;

        alpha(scroll_overlay, 0.9f);
        alpha(scroll_overlay, 0.8f);
        alpha(scroll_overlay, 0.7f);
        alpha(scroll_overlay, 0.75f); /// <<<  fff1f5f7
        alpha(scroll_overlay, 0.6f);

        System.out.println("border");
        int scroll_border = 0xFFDBE7F0;

        alpha(scroll_border, 0.9f);
        alpha(scroll_border, 0.8f);
        alpha(scroll_border, 0.7f);
        alpha(scroll_border, 0.75f);
        alpha(scroll_border, 0.6f);
        alpha(scroll_border, 0.5f);
        alpha(scroll_border, 0.4f);
        alpha(scroll_border, 0.3f);
        alpha(scroll_border, 0.25f);
        alpha(scroll_border, 0.20f);
    }

    int alpha(int color, float a1) {
        int r2 = 0xff;
        int g2 = 0xff;
        int b2 = 0xff;

        int r3 = red(color);
        int g3 = green(color);
        int b3 = blue(color);

        int r1 = (int) ((r3 - r2 + r2 * a1) / a1);
        int g1 = (int) ((g3 - g2 + g2 * a1) / a1);
        int b1 = (int) ((b3 - b2 + b2 * a1) / a1);
        int res = rgb(r1, g1, b1);
        System.out.printf("%f %x\n", a1, res & 0xFFFFFFFFL);
        return res;


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

    public static int rgb(
            int red,
            int green,
            int blue) {
        return 0xff000000 | (red << 16) | (green << 8) | blue;
    }

    @Test
    public void name() {
        for (float i = 0; i <= 1.01; i += 0.1) {
            if (i > 1) {
                i = 1.0f;
            }

            int res = ArgbEvaluator.sInstance.evaluate(i,
                    ColorSet.DAY.lightBackground,
                    ColorSet.NIGHT.lightBackground);
            System.out.println(i + " " + Integer.toHexString(res).substring(2)
            );
        }
    }

    @Test
    public void reverseblending() {

    }
}