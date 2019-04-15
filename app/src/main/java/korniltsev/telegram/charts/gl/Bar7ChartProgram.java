package korniltsev.telegram.charts.gl;

import android.opengl.GLES20;
import android.opengl.Matrix;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import korniltsev.telegram.charts.MainActivity;
import korniltsev.telegram.charts.R;
import korniltsev.telegram.charts.data.ColumnData;
import korniltsev.telegram.charts.ui.Dimen;
import korniltsev.telegram.charts.ui.MyAnimation;
import korniltsev.telegram.charts.ui.MyColor;


public class Bar7ChartProgram {
    public final List<ColumnData> column;// excluding x
    private final int w;
    private final int h;
    private final Dimen dimen;
    private final ChartViewGL root;
    private final boolean scrollbar;
    //    private final float[] vertices;
    private final int[] vbos;
//    private final int vbo;

    private final MyShader shader;
    private final int n;
    private int vxCount;
    public float zoom;
    public float left;
    float max;
    private MyAnimation.Float maxAnim;
    float[]visibility = new float[7];
    private MyAnimation.Float[] visibilityANim = new MyAnimation.Float[7];

    private int tooltipIndex = -1;
    private float aniamteOutPivot;
    public MyAnimation.Float animateOutValueAnim;
    public float animateOutValue = -1f;
    public boolean zoomedIn;
    public float animateInValue = -1f;
    public MyAnimation.Float animateInValueAnim;
    private int leftxi;
    private int rightxi;
    private float leftx;
    private float rightx;


    public Bar7ChartProgram.Vx set(int i, Vx v) {
        List<ColumnData> cs = column;
        v.v0 = cs.get(0).values[i];
        v.v1 = cs.get(1).values[i];
        v.v2 = cs.get(2).values[i];
        v.v3 = cs.get(3).values[i];
        v.v4 = cs.get(4).values[i];
        v.v5 = cs.get(5).values[i];
        v.v6 = cs.get(6).values[i];
        return v;
    }

    public Bar7ChartProgram(List<ColumnData> columns, int w, int h, Dimen dimen, ChartViewGL root, boolean scrollbar, MyShader shader) {
        this.column = columns;
        for (int i = 0; i < visibility.length; i++) {
            visibility[i] = 1f;
        }
        if (columns.size() != 7) {
            throw new AssertionError();
        }
        this.w = w;
        this.h = h;
        this.dimen = dimen;
        this.root = root;
        this.scrollbar = scrollbar;
        this.shader = shader;


        vbos = new int[7];
        GLES20.glGenBuffers(7, vbos, 0);
        MyGL.checkGlError2();

//        long[] values = column.values;

        n = columns.get(0).values.length;
//        int n = values.length/20;
//        vertices = new float[n * 18];

        for (int j = 0; j < 7; j++) {

            List<Vx> vs = new ArrayList<>();
            int columnNo = j;
            for (int i = 0; i < n; i++) {
//            long value = values[i];
                vs.add(set(i, new Vx(i, 0, i)));

                vs.add(set(i, new Vx(i, 1, i)));

                vs.add(set(i, new Vx(i + 1, 0, i)));

                vs.add(set(i, new Vx(i + 1, 0, i)));

                vs.add(set(i, new Vx(i, 1, i)));

                vs.add(set(i, new Vx(i + 1, 1, i)));
            }
            ByteBuffer buf1 = ByteBuffer.allocateDirect(vs.size() * Vx.SIZE)
                    .order(ByteOrder.nativeOrder());
            for (Vx v : vs) {
                buf1.putFloat(v.v0);
                buf1.putFloat(v.v1);
                buf1.putFloat(v.v2);
                buf1.putFloat(v.v3);
                buf1.putFloat(v.v4);
                buf1.putFloat(v.v5);
                buf1.putFloat(v.v6);
                buf1.putFloat(v.x);
                buf1.putFloat(v.zeroOrValue);
                buf1.putFloat(v.xNo);
            }
            vxCount = vs.size();
            buf1.position(0);


            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbos[j]);
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, buf1.capacity(), buf1, GLES20.GL_STATIC_DRAW);
            MyGL.checkGlError2();
        }
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

    float[] colors = new float[4];
    float[] MVP = new float[16];
    float[] V = new float[16];

    public boolean animate(long t) {
        boolean invalidate = false;
        if (maxAnim != null) {
            max = maxAnim.tick(t);
            if (maxAnim.ended) {
                maxAnim = null;
            } else {
                invalidate = true;
            }
        }
        for (int i = 0; i < 7; i++) {
            MyAnimation.Float it = visibilityANim[i];
            if (it != null) {
                visibility[i] = it.tick(t);
                if (it.ended) {
                    visibilityANim[i] = null;
                } else {
                    invalidate = true;
                }
            }
        }
        if (animateOutValueAnim != null) {
            animateOutValue = animateOutValueAnim.tick(t);
            if (animateOutValueAnim.ended) {
                animateOutValueAnim = null;
                if (!zoomedIn) {
                    animateOutValue = -1f;
                }
            } else {
                invalidate = true;
            }
        }
        if (animateInValueAnim != null) {
            animateInValue = animateInValueAnim.tick(t);
            if (animateInValueAnim.ended) {
                animateInValueAnim = null;
                if (!zoomedIn) {
                    animateInValue = -1f;
                }
            } else {
                invalidate = true;
            }
        }
        return invalidate;
    }

    public void prepare(float[] PROJ) {
        float hpadding = dimen.dpf(16);
        float maxx = n ;


        Matrix.setIdentityM(V, 0);
        if (scrollbar) {
            final float max = this.max;
//            hpadding += dimen.dpf(1);
//            final float dip2 = dimen.dpf(2);

            final float w = this.w - 2 * hpadding;
            final float h = root.dimen_scrollbar_height;

            final float yscale = h / (max - 0);
            final float dy = -yscale * 0;
            if (animateOutValue != -1f && animateOutValue != 0f) {
                Matrix.translateM(V, 0, aniamteOutPivot, 0, 0);
                Matrix.scaleM(V, 0, 8 * animateOutValue + 1f, 1f, 1f);
                Matrix.translateM(V, 0, -aniamteOutPivot, 0, 0);
            } else if (animateInValue != -1f) {
                Matrix.translateM(V, 0, aniamteOutPivot, 0, 0);
                Matrix.scaleM(V, 0, animateInValue, 1f, 1f);
                Matrix.translateM(V, 0, -aniamteOutPivot, 0, 0);
            }
            Matrix.translateM(V, 0, hpadding, root.dimen_v_padding8 + root.checkboxesHeight, 0);
            Matrix.translateM(V, 0, 0, dy, 0);
            Matrix.scaleM(V, 0, w / ((maxx)), yscale, 1.0f);
            Matrix.multiplyMM(MVP, 0, PROJ, 0, V, 0);
        } else {
            final float max = this.max;


            final int ypx = dimen.dpi(80) + root.checkboxesHeight;

            final float w = this.w - 2 * hpadding;
            final float h = root.dimen_chart_usefull_height;
            final float xdiff = maxx;
            final float ws = w / xdiff / zoom;
            final float hs = h / (max - 0);
            final float dy = -hs * 0;
            if (animateOutValue != -1f && animateOutValue != 0f) {
                Matrix.translateM(V, 0, aniamteOutPivot, 0, 0);
                Matrix.scaleM(V, 0, 8 * animateOutValue + 1f, 1f, 1f);
                Matrix.translateM(V, 0, -aniamteOutPivot, 0, 0);
            } else if (animateInValue != -1f) {
                Matrix.translateM(V, 0, aniamteOutPivot, 0, 0);
                Matrix.scaleM(V, 0, animateInValue, 1f, 1f);
                Matrix.translateM(V, 0, -aniamteOutPivot, 0, 0);
            }
            Matrix.translateM(V, 0, hpadding, ypx, 0);
            Matrix.translateM(V, 0, 0, dy, 0);
            Matrix.scaleM(V, 0, ws, hs, 1.0f);
            Matrix.translateM(V, 0, -left * xdiff, 0f, 0f);

            Matrix.multiplyMM(MVP, 0, PROJ, 0, V, 0);
        }

    }
    float[] tmpvec = new float[4];
    float[] tmpvec2 = new float[4];

    public void draw(long t, float[] PROJ) {

        shader.use();


        for (int i = 0; i < 7; i++) {
            if (visibility[i] == 0f) {
                continue;
            }

            MyGL.checkGlError2();
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbos[i]);
            GLES20.glEnableVertexAttribArray(shader.a_v0);
            GLES20.glEnableVertexAttribArray(shader.a_v1);
            GLES20.glEnableVertexAttribArray(shader.a_v2);
            GLES20.glEnableVertexAttribArray(shader.a_v3);
            GLES20.glEnableVertexAttribArray(shader.a_v4);
            GLES20.glEnableVertexAttribArray(shader.a_v5);
            GLES20.glEnableVertexAttribArray(shader.a_v6);
            GLES20.glEnableVertexAttribArray(shader.a_x);
            GLES20.glEnableVertexAttribArray(shader.a_zeroOrValue);
            GLES20.glEnableVertexAttribArray(shader.a_xNo);
            MyGL.checkGlError2();
            GLES20.glVertexAttribPointer(shader.a_v0, 1, GLES20.GL_FLOAT, false, Vx.SIZE, 4 * 0);
            GLES20.glVertexAttribPointer(shader.a_v1, 1, GLES20.GL_FLOAT, false, Vx.SIZE, 4 * 1);
            GLES20.glVertexAttribPointer(shader.a_v2, 1, GLES20.GL_FLOAT, false, Vx.SIZE, 4 * 2);
            GLES20.glVertexAttribPointer(shader.a_v3, 1, GLES20.GL_FLOAT, false, Vx.SIZE, 4 * 3);
            GLES20.glVertexAttribPointer(shader.a_v4, 1, GLES20.GL_FLOAT, false, Vx.SIZE, 4 * 4);
            GLES20.glVertexAttribPointer(shader.a_v5, 1, GLES20.GL_FLOAT, false, Vx.SIZE, 4 * 5);
            GLES20.glVertexAttribPointer(shader.a_v6, 1, GLES20.GL_FLOAT, false, Vx.SIZE, 4 * 6);
            GLES20.glVertexAttribPointer(shader.a_x, 1, GLES20.GL_FLOAT, false, Vx.SIZE, 4 * 7);
            GLES20.glVertexAttribPointer(shader.a_zeroOrValue, 1, GLES20.GL_FLOAT, false, Vx.SIZE, 4 * 8);
            GLES20.glVertexAttribPointer(shader.a_xNo, 1, GLES20.GL_FLOAT, false, Vx.SIZE, 4 * 9);
            MyGL.checkGlError2();
            if (animateOutValue != -1 && animateOutValue != 0f) {
                float colorAlpha = (column.get(i).color >>> 24) / 255f;
                float animOutAlpha = 1f - animateOutValue;
                colors[0] = ((column.get(i).color >> 16) & 0xFF) / 255f;
                colors[1] = ((column.get(i).color >> 8) & 0xFF) / 255f;
                colors[2] = (column.get(i).color & 0xFF) / 255f;
                colors[3] = colorAlpha * animOutAlpha;

            } else if (animateInValue != -1f) {
                float colorAlpha = (column.get(i).color >>> 24) / 255f;
                colors[0] = ((column.get(i).color >> 16) & 0xFF) / 255f;
                colors[1] = ((column.get(i).color >> 8) & 0xFF) / 255f;
                colors[2] = (column.get(i).color & 0xFF) / 255f;
                colors[3] = colorAlpha * animateInValue;

            } else {
                MyColor.set(colors, column.get(i).color);
            }
            GLES20.glUniform4fv(shader.colorHandle, 1, colors, 0);
            GLES20.glUniform1f(shader.u_selected_index, tooltipIndex);
            GLES20.glUniform1f(shader.u_columnNo, i);
            GLES20.glUniformMatrix4fv(shader.u_P, 1, false, PROJ, 0);
            GLES20.glUniformMatrix4fv(shader.u_V, 1, false, V, 0);
            GLES20.glUniform1f(shader.u_v0, visibility[0]);
            GLES20.glUniform1f(shader.u_v1, visibility[1]);
            GLES20.glUniform1f(shader.u_v2, visibility[2]);
            GLES20.glUniform1f(shader.u_v3, visibility[3]);
            GLES20.glUniform1f(shader.u_v4, visibility[4]);
            GLES20.glUniform1f(shader.u_v5, visibility[5]);
            GLES20.glUniform1f(shader.u_v6, visibility[6]);
            MyGL.checkGlError2();
            if (animateOutValue == 1f) {

            } else if (animateInValue == -1f || animateInValue == 1f) {
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vxCount);
            } else  {
                int vperline = 6;
                int first = vperline * leftxi;
                int count = (rightxi - leftxi ) * vperline;
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, first, count);
            }
            MyGL.checkGlError2();
        }
    }

    public void animateMinMax(long viewportMax, boolean animate, int druation) {
        if (animate) {
            maxAnim = new MyAnimation.Float(druation, max, viewportMax);
        } else {
            max = viewportMax;
            maxAnim = null;
        }

    }

    public void setTooltipIndex(int finali) {
        tooltipIndex = finali;
    }

    public int getTooltipIndex() {
        return tooltipIndex;
    }

    public void animateFade(int foundIndex, boolean isChecked, long duration) {
        visibilityANim[foundIndex] = new MyAnimation.Float(duration, visibility[foundIndex], isChecked ? 1f : 0f);
    }


    public void animateOut(int duration, boolean zoomedIn, float leftX) {
        this.zoomedIn = zoomedIn;
        if (zoomedIn) {
            this.aniamteOutPivot = leftX;
        }
        if (animateOutValue == -1f) {
            animateOutValue = 0f;
        }
        animateOutValueAnim = new MyAnimation.Float(duration, animateOutValue, zoomedIn ? 1f : 0f);
    }

    public float getTooltipX(float[] PROJ) {
        prepare(PROJ);
        tmpvec[0] = tooltipIndex;
        tmpvec[1] = 1f;
        tmpvec[2] = 0f;
        tmpvec[3] = 1;
        Matrix.multiplyMV(tmpvec2, 0, V, 0, tmpvec, 0);
        return tmpvec2[0];
    }

    public void calcAnimOffsets(float[] PROJ) {
        prepare(PROJ);
        leftxi = -1;
        rightxi = -1;
        for (int i = 0; i < column.get(0).values.length; i++) {
            tmpvec[0] = i;
            tmpvec[1] = 0f;
            tmpvec[2] = 0f;
            tmpvec[3] = 1f;
            Matrix.multiplyMV(tmpvec2, 0, V, 0, tmpvec, 0);
            float x = tmpvec2[0];
            if (leftxi == -1 || x <= 0) {
                leftx = x;
                leftxi = i;
            } else if (x >= w) {
                rightx = x;
                rightxi = i;
                break;
            } else if (i == column.get(0).values.length - 1) {
                rightx = x;
                rightxi = i;
            }
        }
    }
    public void copyState(Bar7ChartProgram chartBar7) {
        for (int i = 0; i < visibility.length; i++) {
            visibility[i] = chartBar7.visibility[i];
        }
    }

    public void animateIn(int duration, boolean zoomedIn, float leftx) {
        this.zoomedIn = zoomedIn;
        if (zoomedIn) {
            aniamteOutPivot = leftx;

        }
        if (animateInValue == -1f) {
            animateInValue = 0f;
        }
        animateInValueAnim = new MyAnimation.Float(duration, animateInValue, zoomedIn ? 1f : 0f);
    }


    static class Vx {
        public static final int SIZE = 10 * 4;
        float v0;
        float v1;
        float v2;
        float v3;
        float v4;
        float v5;
        float v6;
        float x;
        float zeroOrValue;// zero or value
        float xNo;

        public Vx(float x, float zeroOrValue, float xNo) {
            this.x = x;
            this.zeroOrValue = zeroOrValue;
            this.xNo = xNo;
        }
    }

    public static final class MyShader {

        public final int program;
        public final int colorHandle;
        public final int u_selected_index;
        public final int u_V;
        public final int u_P;
        public final int u_columnNo;
        public final int a_v0;
        public final int a_v1;
        public final int a_v2;
        public final int a_v3;
        public final int a_v4;
        public final int a_v5;
        public final int a_v6;
        public final int a_x;
        public final int a_zeroOrValue;
        public final int a_xNo;

        public final int u_v0;
        public final int u_v1;
        public final int u_v2;
        public final int u_v3;
        public final int u_v4;
        public final int u_v5;
        public final int u_v6;
        private boolean released;

        public MyShader() {
            try {
                byte[] fragment = MainActivity.readAll(
                        MainActivity.ctx.getResources().openRawResource(R.raw.bar7_fragment));
                byte[] vertex = MainActivity.readAll(
                        MainActivity.ctx.getResources().openRawResource(R.raw.bar7_vertex));
                program = MyGL.createProgram(new String(vertex, "UTF-8"), new String(fragment, "UTF-8"));
            } catch (IOException e) {
                throw new AssertionError();
            }
            u_V = GLES20.glGetUniformLocation(program, "u_V");
            u_P = GLES20.glGetUniformLocation(program, "u_P");
            u_selected_index = GLES20.glGetUniformLocation(program, "u_selected_index");
            a_v0 = GLES20.glGetAttribLocation(program, "a_v0");
            a_v1 = GLES20.glGetAttribLocation(program, "a_v1");
            a_v2 = GLES20.glGetAttribLocation(program, "a_v2");
            a_v3 = GLES20.glGetAttribLocation(program, "a_v3");
            a_v4 = GLES20.glGetAttribLocation(program, "a_v4");
            a_v5 = GLES20.glGetAttribLocation(program, "a_v5");
            a_v6 = GLES20.glGetAttribLocation(program, "a_v6");
            a_x = GLES20.glGetAttribLocation(program, "a_x");
            a_zeroOrValue = GLES20.glGetAttribLocation(program, "a_zeroOrValue");
            a_xNo = GLES20.glGetAttribLocation(program, "a_xNo");
            colorHandle = GLES20.glGetUniformLocation(program, "u_color");
            u_columnNo = GLES20.glGetUniformLocation(program, "u_columnNo");
            u_v0 = GLES20.glGetUniformLocation(program, "u_v0");
            u_v1 = GLES20.glGetUniformLocation(program, "u_v1");
            u_v2 = GLES20.glGetUniformLocation(program, "u_v2");
            u_v3 = GLES20.glGetUniformLocation(program, "u_v3");
            u_v4 = GLES20.glGetUniformLocation(program, "u_v4");
            u_v5 = GLES20.glGetUniformLocation(program, "u_v5");
            u_v6 = GLES20.glGetUniformLocation(program, "u_v6");
        }

        public final void use() {
            GLES20.glUseProgram(program);
        }

        public void release() {
            if (released) {
                return;
            }
            released = true;
            GLES20.glDeleteProgram(program);
        }
    }

    public void release() {
        GLES20.glDeleteBuffers(7, vbos, 0);
        MyGL.checkGlError2();
    }
}
