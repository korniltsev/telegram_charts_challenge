package korniltsev.telegram.charts.ui;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.util.Log;
import android.view.animation.LinearInterpolator;

import static android.graphics.PixelFormat.OPAQUE;

public class MyColorDrawable extends Drawable {
    public int color;
    private ValueAnimator anim;
    private MyAnimation.Color bgAnim;

    final boolean marker;

    public MyColorDrawable(int color, boolean marker) {
        this.color = color;
        this.marker = marker;
    }

    public void animate(final int to) {
        bgAnim = new MyAnimation.Color(160, color, to);
//        if (anim != null) {
//            anim.cancel();
//        }
//        anim = ValueAnimator.ofInt(color, to);
//        anim.setEvaluator(new ArgbEvaluator());
//        anim.setInterpolator(new LinearInterpolator());
//        anim.setDuration(1600);
//        final int from = color;
//
//        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//            @Override
//            public void onAnimationUpdate(ValueAnimator animation) {
//                float f = animation.getAnimatedFraction();
//
////                int v = (int) animation.getAnimatedValue();//todo unneded allocations
//                int v = korniltsev.telegram.charts.ui.ArgbEvaluator.sInstance.evaluate(f, from, to);
//                if (marker) {
//                    Log.d("MyColorDrawable", "v " + Integer.toHexString(v));
//                }
//                color = v;
//                invalidateSelf();
//            }
//        });
//        anim.start();
        invalidateSelf();

    }

    @Override
    public void draw(Canvas canvas) {
        if (bgAnim != null) {//todo try to synchronize time with gl renderer
            color = bgAnim.tick(SystemClock.uptimeMillis());
            if (bgAnim.ended) {
                bgAnim = null;
            } else {
                invalidateSelf();
            }
        }
        canvas.drawColor(color);
    }

    @Override
    public void setAlpha(int alpha) {

    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {

    }

    @Override
    public int getOpacity() {
        return OPAQUE;
    }
}
