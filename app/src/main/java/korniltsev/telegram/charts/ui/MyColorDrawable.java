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


    public MyColorDrawable(int color) {
        this.color = color;
    }

    public void animate(final int to, int colorAnimationDuration) {
        bgAnim = new MyAnimation.Color(colorAnimationDuration, color, to);
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
