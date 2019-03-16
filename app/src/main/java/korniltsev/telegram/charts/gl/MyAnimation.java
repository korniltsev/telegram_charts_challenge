package korniltsev.telegram.charts.gl;

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
        boolean ended = false;

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

    public static final class Long {

        final long startTime = SystemClock.uptimeMillis();
        final long from;
        final long to;
        private final long duration;

        long endTime;
        boolean ended = false;

        public Long(long duration, long from, long to) {
            this.duration = duration;
            endTime = startTime + duration;
            this.from = from;
            this.to = to;
        }

        public final long tick(long t) {
            if (t > endTime) {
                ended = true;
                return to;
            } else {
                float v = (float) (t - startTime) / duration;
                float interpolated = INTERPOLATOR.getInterpolation(v);
                return (long) (from + (to - from) * interpolated);
            }
        }
    }
}
