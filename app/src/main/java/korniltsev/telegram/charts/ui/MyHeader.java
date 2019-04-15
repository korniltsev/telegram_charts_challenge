package korniltsev.telegram.charts.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.View;
import android.view.animation.BaseInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import korniltsev.telegram.charts.R;

public class MyHeader extends View {

    public final String str;
    public final Dimen dimen;
    private final int fontSize;
    private final StaticLayout nameLayout;
    private final int paddingTop;
    private final int paddingBotom;
    private final int blueColorDay;
    private final int blueColorNight;
    private final StaticLayout zoomLayout;
    private final Drawable icZoom;
    TextPaint text1Paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    TextPaint text2Paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    float zoomValue = 0f;
    private MyAnimation.Float zoomAnim;
    private int zlHeight;

    public MyHeader(Context context, String str, Dimen dimen, ColorSet colors) {
        super(context);
        this.str = str;
        this.dimen = dimen;
        paddingTop = dimen.dpi(16);
        paddingBotom = dimen.dpi(8);
        fontSize = dimen.dpi(16);
        blueColorDay = 0xff1C8BE3;
        blueColorNight = 0xff48ACF4;
        icZoom = getResources().getDrawable(R.drawable.magnify_minus_outline).mutate();
        icZoom.setBounds(0, 0, dimen.dpi(20), dimen.dpi(20));
        if (colors.day) {
            text1Paint.setColor(colors.legendTitle);
        } else {
            text1Paint.setColor(Color.WHITE);
        }


        if (colors.day) {
            text2Paint.setColor(blueColorDay);
            icZoom.setColorFilter(blueColorDay, PorterDuff.Mode.SRC_IN);
        } else {
            text2Paint.setColor(blueColorNight);
            icZoom.setColorFilter(blueColorNight, PorterDuff.Mode.SRC_IN);
        }
        text1Paint.setTextSize(fontSize);
        text2Paint.setTextSize(fontSize);
//        text1Paint.setTypeface(MyFonts.getRobotoMono(context));
        int tw1 = (int) text1Paint.measureText(str);
        String zo = "Zoom Out";
        int tw2 = (int) text2Paint.measureText(zo);
        nameLayout = new StaticLayout(str, 0, str.length(), text1Paint, tw1, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        zoomLayout = new StaticLayout(zo, 0, zo.length(), text2Paint, tw2, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        setWillNotDraw(false);
        zlHeight = zoomLayout.getHeight();
    }

    public void animate(ColorSet colors) {
        if (colors.day) {
            text1Paint.setColor(colors.legendTitle);
        } else {
            text1Paint.setColor(Color.WHITE);
        }
        if (colors.day) {
            text2Paint.setColor(blueColorDay);
            icZoom.setColorFilter(blueColorDay, PorterDuff.Mode.SRC_IN);
        } else {
            text2Paint.setColor(blueColorNight);
            icZoom.setColorFilter(blueColorNight, PorterDuff.Mode.SRC_IN);
        }
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(w, nameLayout.getHeight() + paddingTop + paddingBotom);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (zoomAnim != null) {
            zoomValue = zoomAnim.tick(SystemClock.uptimeMillis());
            if (zoomAnim.ended) {
                zoomAnim = null;
            } else {
                invalidate();
            }
        }
        float scale = 1f - zoomValue;
        if (scale != 0f) {
            canvas.save();
            text1Paint.setAlpha((int) (255 * (scale)));
            canvas.translate(paddingTop , paddingTop);
            canvas.scale(scale, scale);
            nameLayout.draw(canvas);
            canvas.restore();
        }

        if (zoomValue != 0f) {
            text2Paint.setAlpha((int) (255 * (zoomValue)));
            canvas.translate(paddingTop , paddingTop);

            canvas.translate(0, zlHeight);
            canvas.scale(zoomValue, zoomValue);
            canvas.translate(0, -zlHeight);
            icZoom.draw(canvas);
            canvas.translate(dimen.dpi(24), 0);
            zoomLayout.draw(canvas);
        }
    }

    public void animateZoom(boolean zoomedIn) {
        Interpolator i;
        if (zoomedIn) i = MyAnimation.DECELERATE;
        else i = MyAnimation.ACCELERATE;
        zoomAnim = new MyAnimation.Float(i, 208, zoomValue, zoomedIn ? 1f : 0f);
        invalidate();
    }
}
