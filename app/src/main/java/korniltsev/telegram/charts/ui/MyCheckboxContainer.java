package korniltsev.telegram.charts.ui;

import android.content.Context;
import android.view.ViewGroup;

import java.util.List;

public class MyCheckboxContainer extends ViewGroup {
    final List<MyCheckBox> children;
    final Dimen dimen;
    public MyCheckboxContainer(Context context, List<MyCheckBox> children, Dimen dimen) {
        super(context);
        this.children = children;
        this.dimen = dimen;
        for (int i = 0; i < children.size(); i++) {
            MyCheckBox child = children.get(i);
            addView(child);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int dip16 = dimen.dpi(16);
        int x = dip16;
        int y = 0;
        int hz = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        for (int i = 0; i < children.size(); i++) {
            MyCheckBox child = children.get(i);
            child.measure(hz, hz);
            if (x + child.getMeasuredWidth() > w - dip16) {
                x = dip16;
                y += child.getMeasuredHeight();
                y += dimen.dpi(8);
            }
            child.p_l = x;
            child.p_t = y;
            child.p_r = x + child.getMeasuredWidth();
            child.p_b = y + child.getMeasuredHeight();
            x += child.getMeasuredWidth();
            x += dimen.dpi(6);
        }
        setMeasuredDimension(w, children.get(children.size()-1).p_b);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        for (int i = 0; i < children.size(); i++) {
            MyCheckBox child = children.get(i);
            child.layout(child.p_l, child.p_t, child.p_r, child.p_b);

        }
    }
}
