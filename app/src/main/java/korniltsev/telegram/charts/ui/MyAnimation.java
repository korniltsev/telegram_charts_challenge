package korniltsev.telegram.charts.ui;

import android.os.SystemClock;
import android.view.animation.AccelerateDecelerateInterpolator;

public class MyAnimation {
    public static final AccelerateDecelerateInterpolator INTERPOLATOR = new AccelerateDecelerateInterpolator();
    public static final class Float {

        final long startTime = SystemClock.uptimeMillis();
        final float from;
        final float to;
        private final long duration;

        long endTime;
        public boolean ended = false;

        public Float(long duration, float from, float to) {
            this.duration = duration;
            endTime = startTime + duration;
            this.from = from;
            this.to = to;
        }

        public final float tick(long t) {
            if (t > endTime) {
                ended = true;
                return to;
            } else {
                float v = (float) (t - startTime) / duration;
                float interpolated = INTERPOLATOR.getInterpolation(v);
                return from + (to - from) * interpolated;
            }
        }
    }

}
