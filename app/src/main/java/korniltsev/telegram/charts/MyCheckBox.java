package korniltsev.telegram.charts;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import korniltsev.telegram.charts.ui.Dimen;
import korniltsev.telegram.charts.ui.MyAnimation;

class MyCheckBox extends View {
    public static final OvershootInterpolator OVERSHOOT_INTERPOLATOR = new OvershootInterpolator(2.2f);
    public static final DecelerateInterpolator DECELERATE_INTERPOLATOR = new DecelerateInterpolator();
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint pText = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Dimen dimen;

    //        private final ImageView img;
//        private final TextView text;
    private final Drawable icchecked;
    private final int color;
    private int w;
    private int h;
    private final StaticLayout staticLayout;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Path path;
    private OnCheckedChangeListener listener;
    private MyAnimation.Float anim;
    private MyAnimation.Float scaleAnim;
    private MyAnimation.Color textColorAnim;


    public MyCheckBox(Context c, Dimen dimen, String str, int tint) {
        super(c);
        this.color = tint;
        this.dimen = dimen;
        pText.setTextSize(dimen.dpf(14));
        pText.setColor(Color.WHITE);
        int tw = (int) Math.ceil(pText.measureText(str));
        staticLayout = new StaticLayout(str, 0, str.length(), pText, tw, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        icchecked = getResources().getDrawable(R.drawable.check).mutate();
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setChecked(!checked);
            }
        });
        setWillNotDraw(false);

        icchecked.setBounds(0, 0, icchecked.getIntrinsicWidth(), icchecked.getIntrinsicHeight());
        icchecked.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
        int dip4 = dimen.dpi(4);
        int stroke = dimen.dpi(2);
        h = dimen.dpi(36) - stroke;
        int contentW = tw + icchecked.getIntrinsicWidth() + dimen.dpi(3);
        w = contentW + h - stroke - dimen.dpi(8);

        path = new Path();

        RectF r = new RectF();
        path.moveTo(h / 2f, 0);
        r.set(0, 0, h, h);
        path.addArc(r, -90f, -180f);
        path.lineTo(w - h / 2f, h);
        r.set(w - h, 0, w, h);
        path.addArc(r, 90f, -180f);
        path.lineTo(h / 2f, 0);


        p.setStrokeWidth(stroke);
        p.setColor(tint);
        p.setStyle(Paint.Style.STROKE);
        path.setFillType(Path.FillType.WINDING);


        w += stroke;
        h += stroke;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        setMeasuredDimension(
                w,
                h

        );
    }

    boolean checked = true;
    float fchecked = 1f;
    float fscale = 1f;

    //        @Override
    public void setChecked(boolean checked) {
        boolean stateChanged = listener.onCheckedChanged(checked);
        if (!stateChanged) {
            return;
        }
        this.checked = checked;
        int duration = 208;
        anim = new MyAnimation.Float(duration, this.fchecked, checked ? 1f : 0f);
        if (checked) {
            scaleAnim = new MyAnimation.Float(OVERSHOOT_INTERPOLATOR, duration, fchecked, 1f);
        } else {
            scaleAnim = new MyAnimation.Float(DECELERATE_INTERPOLATOR, duration, fchecked, 0f);
        }
        textColorAnim = new MyAnimation.Color(duration, pText.getColor(), checked ? Color.WHITE : color);

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        long t = SystemClock.uptimeMillis();
        if (anim != null) {
            fchecked = anim.tick(t);
            if (anim.ended) {
                anim = null;
            } else {
                invalidate();
            }
        }
        if (scaleAnim != null) {
            fscale = scaleAnim.tick(t);
            if (scaleAnim.ended) {
                scaleAnim = null;
            } else {
                invalidate();
            }
        }
        if (textColorAnim != null) {
            int c = textColorAnim.tick(t);
            pText.setColor(c);
            if (textColorAnim.ended) {
                textColorAnim = null;
            } else {
                invalidate();
            }
        }
        int dip1 = dimen.dpi(1);
        canvas.save();
        canvas.translate(dip1, dip1);

        p.setAlpha(255);
        p.setStyle(Paint.Style.STROKE);
        canvas.drawPath(path, p);
        if (fchecked != 0f) {
            p.setAlpha((int) (fchecked * 255));
            p.setStyle(Paint.Style.FILL);
            canvas.drawPath(path, p);
        }
        canvas.restore();

        boolean checked = this.checked;
        float icw = fchecked * (icchecked.getBounds().width() + dimen.dpi(4));
        float ty;
        float tx;
        ty = (getMeasuredHeight() - staticLayout.getHeight()) / 2f;
        tx = (getMeasuredWidth() - staticLayout.getWidth() - icw) / 2f + icw;

        canvas.save();
        canvas.translate(tx, ty);
        staticLayout.draw(canvas);
        canvas.restore();

        if (checked) {

            ty = (getMeasuredHeight() - icchecked.getBounds().height()) / 2f;
            tx = (getMeasuredWidth() - staticLayout.getWidth() - (icchecked.getBounds().width() + dimen.dpi(4))) / 2f;
            canvas.save();
            canvas.translate(tx, ty);
            canvas.translate(icchecked.getBounds().width()/2f, icchecked.getBounds().height()/2f);
            canvas.scale(fscale, fscale);
            canvas.translate(-icchecked.getBounds().width()/2f, -icchecked.getBounds().height()/2f);

            icchecked.draw(canvas);
            canvas.restore();
        }


    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener onCheckedChangeListener) {
        this.listener = onCheckedChangeListener;
    }

    public interface OnCheckedChangeListener {

        boolean onCheckedChanged(boolean isChecked);
    }
}
