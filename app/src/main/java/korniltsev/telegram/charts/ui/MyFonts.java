package korniltsev.telegram.charts.ui;

import android.content.Context;
import android.graphics.Typeface;

public class MyFonts {
    static Typeface ROBOTO_MONO;
    public static Typeface getRobotoMono(Context ctx) {
        if (ROBOTO_MONO == null) {
            ROBOTO_MONO = Typeface.createFromAsset(ctx.getAssets(), "Roboto-Medium.ttf");
        }
        return ROBOTO_MONO;
    }
}
