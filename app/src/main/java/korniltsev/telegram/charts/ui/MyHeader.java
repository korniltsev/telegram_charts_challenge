package korniltsev.telegram.charts.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.View;

public class MyHeader extends View {
    public final String str;
    public final Dimen dimen;
    private final int fontSize;
    private final StaticLayout staticLayout;
    private final int paddingTop;
    private final int paddingBotom;
    TextPaint text1Paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

    public MyHeader(Context context, String str, Dimen dimen, ColorSet colors) {
        super(context);
        this.str = str;
        this.dimen = dimen;
        paddingTop = dimen.dpi(16);
        paddingBotom = dimen.dpi(8);
        fontSize = dimen.dpi(16);
        if (colors.day) {
            text1Paint.setColor(colors.legendTitle);
        } else {
            text1Paint.setColor(Color.WHITE);
        }

        text1Paint.setTextSize(fontSize);
//        text1Paint.setTypeface(MyFonts.getRobotoMono(context));
        int tw = (int) text1Paint.measureText(str);

        staticLayout = new StaticLayout(str, 0, str.length(), text1Paint, tw, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        setWillNotDraw(false);
    }

    public void animate(ColorSet colors) {
        if (colors.day) {
            text1Paint.setColor(colors.legendTitle);
        } else {
            text1Paint.setColor(Color.WHITE);
        }
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(w, staticLayout.getHeight() + paddingTop + paddingBotom);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.translate(paddingTop, paddingTop);
        staticLayout.draw(canvas);
    }

    public void animateZoom(boolean b) {

    }
}
