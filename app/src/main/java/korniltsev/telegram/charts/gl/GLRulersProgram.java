package korniltsev.telegram.charts.gl;

import android.graphics.Color;
import android.graphics.Paint;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.text.TextPaint;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import korniltsev.telegram.charts.MainActivity;
import korniltsev.telegram.charts.data.ColumnData;
import korniltsev.telegram.charts.ui.ColorSet;
import korniltsev.telegram.charts.ui.Dimen;
import korniltsev.telegram.charts.ui.MyAnimation;
import korniltsev.telegram.charts.ui.MyColor;

public final class GLRulersProgram {
    public static final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d", Locale.US);

    private static final int BYTES_PER_FLOAT = 4;
    private static final int STRIDE_BYTES = 2 * BYTES_PER_FLOAT;
    private static final int POSITION_DATA_SIZE = 2;
//    public static final int BORDER_COLOR = 0x334b87b4;

//    static final String vertexShader =
//            "uniform mat4 u_MVPMatrix;      \n"
//                    + "attribute vec2 a_Position;     \n"
//                    + "void main()                    \n"
//                    + "{                              \n"
//                    + "   gl_Position = u_MVPMatrix * vec4(a_Position.xy, 0.0, 1.0);   \n"
//                    + "}                              \n";
//
//    static final String fragmentShader =
//            "precision mediump float;       \n"
//                    + "uniform vec4 u_color;       \n"
//                    + "void main()                    \n"
//                    + "{                              \n"
//                    + "   gl_FragColor = u_color;     \n"
//                    + "}                              \n";



//    private final int lineMVPHandle;
//    private final int linePositionHandle;
//    private final int lineProgram;
    private final int lineVerticesVBO;
//    private final int lineColorHandle;
    private final TextPaint paint;
//    private final TextTex textZero;

//    private final int texVerticesVBO;
    private final TexShader texShader;
    private final TextTex zero;
    private final SimpleShader simpleShader;
    private final float wpx;
    private final float hpadding;
    private final int[] vbos;
    private ColorSet colors;
    private ColumnData xColumn;


    private float[] MVP = new float[16];

    public final int canvasW;
    public final int canvasH;

    private final Dimen dimen;


    final ChartViewGL root;


    static final float lineVertices[] = {
            0, 0,
            1, 0,
    };


    int lineColor;
    int textColor;
    private MyAnimation.Color colorAnim;



    private final ArrayList<Ruler> rs = new ArrayList<>();
    private float[] colorParts = new float[4];
    private MyAnimation.Color textColorAnim;
    private float left;
    private float strideChangeLabelWidth
            ;
    private boolean released;


    public GLRulersProgram(int canvasW, int canvasH, Dimen dimen, ChartViewGL root, ColorSet colors, SimpleShader s, ColumnData xColumn) {
        this.colors = colors;
        this.xColumn = xColumn;

        texShader = new TexShader(true, true);
        this.canvasW = canvasW;
        this.canvasH = canvasH;
        this.dimen = dimen;
        this.root = root;
        this.lineColor = colors.ruler;
        this.textColor = colors.rulerLabelColor;
        this.simpleShader = s;
//        lineProgram = MyGL.createProgram(vertexShader, fragmentShader);
//        lineMVPHandle = GLES20.glGetUniformLocation(lineProgram, "u_MVPMatrix");
//        linePositionHandle = GLES20.glGetAttribLocation(lineProgram, "a_Position");
//        lineColorHandle = GLES20.glGetUniformLocation(lineProgram, "u_color");


        FloatBuffer buf1 = ByteBuffer.allocateDirect(lineVertices.length * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        buf1.put(lineVertices);
        buf1.position(0);


        vbos = new int[1];
        GLES20.glGenBuffers(1, vbos, 0);
        lineVerticesVBO = vbos[0];
//        texVerticesVBO = vbos[1];
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, lineVerticesVBO);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, lineVertices.length * BYTES_PER_FLOAT, buf1, GLES20.GL_STATIC_DRAW);
//        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, texVerticesVBO);
//        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, texVertices.length * BYTES_PER_FLOAT, buf2, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.BLUE);
        paint.setTextSize(dimen.dpf(12));

        zero = new TextTex("0", paint);

        hpadding = dimen.dpf(16);
        wpx = canvasW - 2 * hpadding;
    }

    public void init(long max) {
        Ruler r = new Ruler(max, 1.0f, paint);
        rs.add(r);
    }

    public void animate(ColorSet colors, long duration) {
        this.colors = colors;
        colorAnim = new MyAnimation.Color(duration, lineColor, colors.ruler);
        textColorAnim = new MyAnimation.Color(duration, textColor, colors.rulerLabelColor);
    }


    //    public static final int LINE_COLOR = 0xffE7E8E9;

    final float[] LINE_COLOR_PARTS = new float[4];
    //{
//            MyColor.red(LINE_COLOR) / 255f,
//            MyColor.green(LINE_COLOR) / 255f,
//            MyColor.blue(LINE_COLOR) / 255f,
//            MyColor.alpha(LINE_COLOR) / 255f,
//    };

//    public static final int DEBUG_COLOR = 0xff000000;

//    static final float[] DEBUG_COLOR_PARTS = new float[]{
//            MyColor.red(DEBUG_COLOR) / 255f,
//            MyColor.green(DEBUG_COLOR) / 255f,
//            MyColor.blue(DEBUG_COLOR) / 255f,
//            MyColor.alpha(DEBUG_COLOR) / 255f,
//    };


    public final void draw(long t) {




        float vpaddingTextOverPadding = dimen.dpf(3);
        final float zero = dimen.dpf(80);
        drawLine(hpadding, zero + 0, canvasW - 2 * hpadding, 1f);
        drawText(this.zero, hpadding, zero + vpaddingTextOverPadding, 1f);

        final float dip50 = dimen.dpf(50);
        for (int ruler_i = 0, rsSize = rs.size(); ruler_i < rsSize; ruler_i++) {
            Ruler r = rs.get(ruler_i);

            float dy = dip50;

            for (int i = 1; i < 6; ++i) {

                drawLine(hpadding, zero + dy * r.scale, canvasW - 2 * hpadding, r.alpha);

                drawText(r.values.get(i-1), hpadding, zero + dy * r.scale + vpaddingTextOverPadding, r.alpha);

                dy += dip50;
            }
//            drawLine(hpadding, zero + root.dimen_chart_height-10, (canvasW - 2 * hpadding)/2, 1.0f);
        }

        drawX(t);
    }

    List<XValueLable> xValues = new ArrayList<>();
    List<XValueLable> animatingOut = new ArrayList<>();
    public void drawX(long t){


//        if (xValues.size() == 0) {
//            TextPaint p = new TextPaint();
//            p.setTextSize(dimen.dpf(12));
//            xValues.add(new TextTex("Feb 28", p));
//        }
//        Log.d("FUCK", String.format("ruler %d %d", animatingOut.size(), xValues.size()));
        for (int i = 0, animatingOutSize = animatingOut.size(); i < animatingOutSize; i++) {
            XValueLable xValueLable = animatingOut.get(i);
            drawXValue(xValueLable);
        }
        for (int i = 0, xValuesSize = xValues.size(); i < xValuesSize; i++) {
            XValueLable xValue = xValues.get(i);
            drawXValue(xValue);
        }


    }

    private void drawXValue(XValueLable xValue) {
        if (xValue.alpha == 0f) {
            return;
        }
        final float scalex = 2.0f / canvasW;
        final float scaley = 2.0f / canvasH;
        Matrix.setIdentityM(MVP, 0);
        Matrix.translateM(MVP, 0, -1.0f, -1.0f, 0);
        Matrix.scaleM(MVP, 0, scalex, scaley, 1.0f);

        int index = xValue.index;
        final float scaledWidth = wpx / zoom;
        final float npos = (float) index / (xColumn.values.length-1);
        final float pos =  npos * scaledWidth - scaledWidth * left;
        if (xValue.tex == null) {
            long v = xColumn.values[xValue.index];
            String format = dateFormat.format(v);
            TextTex lazyTex = new TextTex(format, paint);
            xValue.tex = lazyTex;
        }
        float left = hpadding + pos - xValue.tex.w;
        if (left > canvasW || left + xValue.tex.w < 0) {//todo lazy x labels
            return;
        }


        Matrix.translateM(MVP, 0, left, dimen.dpf(80-20), 0);
        Matrix.scaleM(MVP, 0, xValue.tex.w, xValue.tex.h, 1f);


        float alpha = xValue.alpha;

        GLES20.glUseProgram(texShader.texProgram);
        MyGL.checkGlError2();
        GLES20.glEnableVertexAttribArray(texShader.texPositionHandle);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, texShader.texVerticesVBO);
        GLES20.glVertexAttribPointer(texShader.texPositionHandle, POSITION_DATA_SIZE, GLES20.GL_FLOAT, false, STRIDE_BYTES, 0);
        GLES20.glUniformMatrix4fv(texShader.texMVPHandle, 1, false, MVP, 0);

        GLES20.glUniform1f(texShader.texAlphaHandle, alpha);

        MyColor.set(colorParts, textColor);
        GLES20.glUniform4fv(texShader.u_color, 1, colorParts, 0);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, xValue.tex.tex[0]);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, TexShader.texVertices.length / 2);
    }

    ;

    public final boolean animationTick(long t) {
        //todo invalidate
        boolean invalidate = false;
        if (colorAnim != null) {
            lineColor = colorAnim.tick(t);
            if (colorAnim.ended) {
                colorAnim = null;
            } else {
                invalidate = true;
            }
        }
        if (textColorAnim != null) {
            textColor = textColorAnim.tick(t);
            if (textColorAnim.ended) {
                textColorAnim = null;
            } else {
                invalidate = true;
            }
        }
        for (int i = rs.size() - 1; i >= 0; i--) {
            Ruler r = rs.get(i);

            if (r.scaleAnim != null) {
                r.scale = r.scaleAnim.tick(t);
                if (r.scaleAnim.ended) {
                    r.scaleAnim = null;
                }
            }
            if (r.alphaAnim != null) {
                r.alpha = r.alphaAnim.tick(t);
                if (r.alphaAnim.ended) {
                    r.alphaAnim = null;
                } else {
                    invalidate = true;
                }
            }
            if (r.toBeDeleted && r.alphaAnim == null && r.scaleAnim == null) {
                List<TextTex> values = r.values;
                try {
                    for (int i1 = 0, valuesSize = values.size(); i1 < valuesSize; i1++) {
                        TextTex value = values.get(i1);
                        value.release();
                    }
                    rs.remove(i);
                } catch (Throwable e) {
                    if (MainActivity.LOGGING) Log.e(MainActivity.TAG, "err", e);
                }
            }
        }
        for (int i = 0, xValuesSize = xValues.size(); i < xValuesSize; i++) {
            XValueLable x = xValues.get(i);
            if (x.alphaAnim != null) {
                x.alpha = x.alphaAnim.tick(t);
                if (x.alphaAnim.ended) {
                    x.alphaAnim = null;
                } else {
                    invalidate = true;
                }
            }
        }

        for (int i = animatingOut.size()-1; i >=0 ; i--) {
            XValueLable x = animatingOut.get(i);
            if (x.alphaAnim != null) {
                x.alpha = x.alphaAnim.tick(t);
                if (x.alphaAnim.ended) {
                    x.alphaAnim = null;
                    try {
                        if (x.tex != null) {
                            x.tex.release();
                        }
                        animatingOut.remove(i);
                    } catch (Throwable e) {
                        if (MainActivity.LOGGING) Log.e(MainActivity.TAG, "err", e);
                    }
                } else {
                    invalidate = true;
                }
            }
        }
        return invalidate;

    }

    private void drawText(TextTex textZero, float x, float y, float alpha) {



//        GLES20.glUniform4fv(texColorHandle, 1, DEBUG_COLOR_PARTS, 0);//todo try to bind only once




        final float scalex = 2.0f / canvasW;
        final float scaley = 2.0f / canvasH;
        Matrix.setIdentityM(MVP, 0);
        Matrix.translateM(MVP, 0, -1.0f, -1.0f, 0);
        Matrix.scaleM(MVP, 0, scalex, scaley, 1.0f);

        Matrix.translateM(MVP, 0, x, y, 0);
        Matrix.scaleM(MVP, 0, textZero.w, textZero.h, 1f);

//        GLES20.glLineWidth(dimen.dpf(2.0f / 3.0f));


        GLES20.glUseProgram(texShader.texProgram);
        MyGL.checkGlError2();
        GLES20.glEnableVertexAttribArray(texShader.texPositionHandle);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, texShader.texVerticesVBO);
        GLES20.glVertexAttribPointer(texShader.texPositionHandle, POSITION_DATA_SIZE, GLES20.GL_FLOAT, false, STRIDE_BYTES, 0);
        GLES20.glUniformMatrix4fv(texShader.texMVPHandle, 1, false, MVP, 0);
        GLES20.glUniform1f(texShader.texAlphaHandle, alpha);

        MyColor.set(colorParts, textColor);
        GLES20.glUniform4fv(texShader.u_color, 1, colorParts, 0);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textZero.tex[0]);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, TexShader.texVertices.length / 2);

    }


    public void drawLine(float x, float y, float w, float alpha) {
        GLES20.glUseProgram(simpleShader.program);
        MyGL.checkGlError2();
        LINE_COLOR_PARTS[0] = MyColor.red(lineColor) / 255f;
        LINE_COLOR_PARTS[1] = MyColor.green(lineColor) / 255f;
        LINE_COLOR_PARTS[2] = MyColor.blue(lineColor) / 255f;
        LINE_COLOR_PARTS[3] = alpha;
        GLES20.glUniform4fv(simpleShader.colorHandle, 1, LINE_COLOR_PARTS, 0);//todo try to bind only once

        GLES20.glEnableVertexAttribArray(simpleShader.positionHandle);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, lineVerticesVBO);
        GLES20.glVertexAttribPointer(simpleShader.positionHandle, POSITION_DATA_SIZE, GLES20.GL_FLOAT, false, STRIDE_BYTES, 0);


        final float scalex = 2.0f / canvasW;
        final float scaley = 2.0f / canvasH;
        Matrix.setIdentityM(MVP, 0);
        Matrix.translateM(MVP, 0, -1.0f, -1.0f, 0);
        Matrix.scaleM(MVP, 0, scalex, scaley, 1.0f);

        Matrix.translateM(MVP, 0, x, y, 0);
        Matrix.scaleM(MVP, 0, w, 1.0f, 1.0f);

        GLES20.glLineWidth(dimen.dpf(2.0f / 3.0f));
        GLES20.glUniformMatrix4fv(simpleShader.MVPHandle, 1, false, MVP, 0);
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, lineVertices.length / 2);

    }

//    public void setLeftRight(float left, float right) {
//        this.left = left;
//        this.right = right;
//        Log.d("OVERLAY", " " + left + " " + right);
//    }

    public void animateScale(float ratio, long maxValue, int checkedCount, int prevCheckedCOunt) {
        for (int i = 0, rsSize = rs.size(); i < rsSize; i++) {
            Ruler r = rs.get(i);
            if (r.toBeDeleted) {
                continue;
            }
            if (checkedCount == 0 && prevCheckedCOunt != 0) {

            } else {
                r.scaleAnim = new MyAnimation.Float(MyAnimation.ANIM_DRATION, r.scale, ratio);
            }
            r.alphaAnim = new MyAnimation.Float(MyAnimation.ANIM_DRATION, r.alpha, 0f);
            r.toBeDeleted = true;
        }
        if (checkedCount != 0) {
            Ruler e = new Ruler(maxValue, 1f/ratio, paint);
            e.alpha = 0f;
            if (prevCheckedCOunt == 0) {
                e.scale = 1f;
            } else {
                e.scaleAnim = new MyAnimation.Float(MyAnimation.ANIM_DRATION, e.scale, 1f);
            }
            e.alphaAnim = new MyAnimation.Float(MyAnimation.ANIM_DRATION, e.alpha, 1f);
            rs.add(e);
        }
    }

    float stride = 0;//draw xlabel every n points
    public static final int WANT_LABELS = 4;
    public static final int WANT_LABELS_MAX = 5;
    public static final int WANT_LABELS_MIN = 3;
    float zoom;

    public void setLeftRight(float left, float right, float scale) {
//        float prevzoom = zoom;
        this.left = left;
        this.zoom = scale;
        float ir = (right * (xColumn.values.length - 1));
//        if (ir >= xColumn.values.length) {
//            ir = xColumn.values.length - 1;
//        }
        float il = (left * (xColumn.values.length - 1));
//        if (il < 0) {
//            il = 0;
//        } else if (il >= ir) {
//            il = ir - 1;
//        }
//        boolean change;

        float n = Math.round(ir - il);// points between left and right
        float newStride = -1f;
        if (stride == 0) {
//            change = true;
            newStride = Math.round(n / WANT_LABELS);
        } else {


//            change = true;

//            float labelWidth = n * approxLabelWidth;
//            labelWidth
//            strideChangeLabelWidth
//            n/
            float labelsCount = n / stride;//todo need rounding fix here
            if (labelsCount > WANT_LABELS_MAX || labelsCount < WANT_LABELS_MIN) {
//                change = true;
                newStride = Math.round(n / WANT_LABELS);
            } else {
//                change = false;
            }
        }


//        float n6 = (float)n / SPLIt;
//        wpx / SPLIt;
//        float zoomDiff = Math.abs(prevzoom - zoom);

//        wpx / approxLabelWidth;
//        n *
//        float diff = Math.abs(lastn6 - n6);
        if (newStride != -1) {
//            Log.d("FUCK", String.format("prev_stride %3f new_stride %3f n %3f left %3f right %3f ", stride, newStride, n, left, right));

//            float approxLabelWidth = dimen.dpf(40);
//            wpx / ;
            int prevStdie = (int) stride;
            int inewStride = (int) newStride;
            if (prevStdie != 0 && inewStride == prevStdie) {
                return;
            }
            stride = n/WANT_LABELS;
//            Log.d("FUCK", "n " + n + " " + n6 + " diff " + diff);
//            lastn6 =  n6;
//            float zoomDiff = Math.abs(prevzoom - zoom);
//            Log.d("FUCK", "n " + n + " " + n6 + " zoom " + zoomDiff);
            for (int i = 0, xValuesSize = xValues.size(); i < xValuesSize; i++) {
                XValueLable xValue = xValues.get(i);
                xValue.alphaAnim = new MyAnimation.Float(MyAnimation.ANIM_DRATION, xValue.alpha, 0f);
            }
            animatingOut.addAll(xValues);
            xValues.clear();
            int i = xColumn.values.length - 1;
//            int istrid = (int) this.stride;
            for (; i >= 0; i = i - inewStride) {
                long v = xColumn.values[i];
                XValueLable e = new XValueLable(i, null);
                e.alpha = 0f;
                e.alphaAnim = new MyAnimation.Float(MyAnimation.ANIM_DRATION, 0f, 1f);
                xValues.add(e);
            }
        }
    }

    public void release() {
        if (released) {
            return;
        }
        released = true;
        GLES20.glDeleteBuffers(1, vbos, 0);
        zero.release();
        for (int i1 = 0, rsSize = rs.size(); i1 < rsSize; i1++) {
            Ruler r = rs.get(i1);
            List<TextTex> values = r.values;
            for (int i = 0, valuesSize = values.size(); i < valuesSize; i++) {
                TextTex value = values.get(i);
                value.release();

            }
            r.values.clear();
        }
        for (int i = 0, xValuesSize = xValues.size(); i < xValuesSize; i++) {
            XValueLable xValue = xValues.get(i);
            xValue.tex.release();
            xValue.tex = null;
        }
        for (int i = 0, animatingOutSize = animatingOut.size(); i < animatingOutSize; i++) {
            XValueLable xValueLable = animatingOut.get(i);
            xValueLable.tex.release();
            xValueLable.tex = null;
        }
        xValues.clear();
        animatingOut.clear();

    }

    public static class XValueLable {
        public final int index;
        public TextTex tex;
        public float alpha = 1f;
        public MyAnimation.Float alphaAnim = null;

        XValueLable(int index, TextTex tex) {
            this.index = index;
            this.tex = tex;
        }
    }

//    Locale locale = new Locale("da", "DK");

//    static final NumberFormat numberFormat = NumberFormat.getInstance(Locale.getDefault());

    public static final class Ruler {
        long maxValue;
        float scale = 1f;
        float alpha = 1f;
        MyAnimation.Float scaleAnim;
        MyAnimation.Float alphaAnim;
        boolean toBeDeleted = false;
        public final List<TextTex> values = new ArrayList<>();
        public Ruler(long maxValue, float scale, TextPaint p) {
            this.maxValue = maxValue;
            this.scale = scale;
            int dy = 50;
            int max = 280;
            for (int i = 1; i < 6; i++) {
                float s = (float)dy / max;
                long v = (long) (maxValue * s);
                String text = String.valueOf(v);
//                String format = numberFormat.format(v);
                values.add(new TextTex(text, p));
                dy += 50;
            }
        }
    }
}
