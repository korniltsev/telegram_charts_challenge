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
import android.widget.TextView;

import korniltsev.telegram.charts.ui.Dimen;
import korniltsev.telegram.charts.ui.MyAnimation;

class MyCheckBox extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint pText = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Dimen dimen;

    //        private final ImageView img;
//        private final TextView text;
    private final Drawable icchecked;
    private int w;
    private int h;
    private final StaticLayout staticLayout;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Path path;
    private OnCheckedChangeListener listener;

    public MyCheckBox(Context c, Dimen dimen, String str, int tint) {
        super(c);
        this.dimen = dimen;
        pText.setTextSize(dimen.dpf(16));
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
        int dip4 = dimen.dpi(4);
        int stroke = dimen.dpi(2);
        h = dimen.dpi(36) - stroke;
        int contentW = tw + icchecked.getIntrinsicWidth();
        w =  contentW + h - stroke;

        path = new Path();

        RectF r = new RectF();
        path.moveTo(h/2f, 0);
        r.set(0, 0, h, h);
        path.addArc(r, -90f, -180f);
        path.lineTo(h/2f + contentW, h);
        r.set(w-h, 0, w, h);
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

    //        @Override
    public void setChecked(boolean checked) {
        boolean stateChanged = listener.onCheckedChanged(checked);
        if (!stateChanged) {
            return;
        }
        this.checked = checked;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int dip1 = dimen.dpi(1);
        canvas.translate(dip1, dip1);
        if (checked) {
            p.setStyle(Paint.Style.FILL_AND_STROKE);
        } else {
            p.setStyle(Paint.Style.STROKE);
        }
        canvas.drawPath(path, p);

        boolean checked = this.checked;
        float icw = (checked ? 1f : 0f) * (icchecked.getBounds().width());
        float ty;
        float tx;
        ty = (getMeasuredHeight() - staticLayout.getHeight()) / 2f;
        tx= (getMeasuredWidth() - staticLayout.getWidth()-icw) / 2f
                + (checked ? 1f : 0f) * icchecked.getBounds().width();

        canvas.save();
        canvas.translate(tx, ty);
        staticLayout.draw(canvas);
        canvas.restore();

        if (checked) {

            ty = (getMeasuredHeight() - icchecked.getBounds().height()) / 2f;
            canvas.save();
            canvas.translate(h/2f, ty);
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
