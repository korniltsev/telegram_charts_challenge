package korniltsev.telegram.charts.ui;

import android.os.SystemClock;
import android.util.Log;
import android.view.animation.AccelerateDecelerateInterpolator;

public class MyAnimation {
    public static final int ANIM_DRATION = 1600;


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

    public static final class Color {

        final long startTime = SystemClock.uptimeMillis();
        final int from;
        final int to;
//        private final long duration;

        long endTime;
        public boolean ended = false;

        public Color(int from, int to) {
            endTime = startTime + ANIM_DRATION;
            this.from = from;
            this.to = to;
        }

        public Color(long duration, int from, int to) {
//            this.duration = duration;
            this(from, to);
        }

        public final int tick(long t) {
            if (t > endTime) {
                ended = true;
                return to;
            } else {
                float v = (float) (t - startTime) / ANIM_DRATION;
//                float interpolated = INTERPOLATOR.getInterpolation(v);
                float interpolated = v;
                int res = ArgbEvaluator.sInstance.evaluate(v, from, to);
//                Log.d("MyColorAnim", "v " + v  + " " + Integer.toHexString(res));
                return res;
            }
        }
    }

}
