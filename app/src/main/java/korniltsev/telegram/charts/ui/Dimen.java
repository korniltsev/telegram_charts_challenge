package korniltsev.telegram.charts.ui;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.TypedValue;

public final class Dimen {
    public final float density;

    public Dimen(Context ctx) {
        DisplayMetrics metrics = ctx.getResources().getDisplayMetrics();
        density = metrics.density;
    }

    public final float dpf(float dip) {
        return dip * density;
    }

    public final int dpi(int dip) {
        return (int) (dip * density);
    }
}
