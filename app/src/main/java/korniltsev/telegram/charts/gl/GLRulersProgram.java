package korniltsev.telegram.charts.gl;

import android.graphics.Color;
import android.graphics.Paint;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.text.TextPaint;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import korniltsev.telegram.charts.ui.ColorSet;
import korniltsev.telegram.charts.ui.Dimen;
import korniltsev.telegram.charts.ui.MyAnimation;
import korniltsev.telegram.charts.ui.MyColor;

public final class GLRulersProgram {

    private static final int BYTES_PER_FLOAT = 4;
    private static final int STRIDE_BYTES = 2 * BYTES_PER_FLOAT;
    private static final int POSITION_DATA_SIZE = 2;
//    public static final int BORDER_COLOR = 0x334b87b4;

    static final String vertexShader =
            "uniform mat4 u_MVPMatrix;      \n"
                    + "attribute vec2 a_Position;     \n"
                    + "void main()                    \n"
                    + "{                              \n"
                    + "   gl_Position = u_MVPMatrix * vec4(a_Position.xy, 0.0, 1.0);   \n"
                    + "}                              \n";

    static final String fragmentShader =
            "precision mediump float;       \n"
                    + "uniform vec4 u_color;       \n"
                    + "void main()                    \n"
                    + "{                              \n"
                    + "   gl_FragColor = u_color;     \n"
                    + "}                              \n";



    private final int lineMVPHandle;
    private final int linePositionHandle;
    private final int lineProgram;
    private final int lineVerticesVBO;
    private final int lineColorHandle;
    private final TextPaint paint;
//    private final TextTex textZero;

//    private final int texVerticesVBO;
    private final TexShader texShader;
    private final TextTex zero;
    private ColorSet colors;


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

    public GLRulersProgram(int canvasW, int canvasH, Dimen dimen, ChartViewGL root, ColorSet colors) {
        this.colors = colors;

        texShader = new TexShader(true, true);
        this.canvasW = canvasW;
        this.canvasH = canvasH;
        this.dimen = dimen;
        this.root = root;
        this.lineColor = colors.ruler;
        this.textColor = colors.rulerLabelColor;
        lineProgram = MyGL.createProgram(vertexShader, fragmentShader);
        lineMVPHandle = GLES20.glGetUniformLocation(lineProgram, "u_MVPMatrix");
        linePositionHandle = GLES20.glGetAttribLocation(lineProgram, "a_Position");
        lineColorHandle = GLES20.glGetUniformLocation(lineProgram, "u_color");


        FloatBuffer buf1 = ByteBuffer.allocateDirect(lineVertices.length * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        buf1.put(lineVertices);
        buf1.position(0);


        int[] vbos = new int[1];
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
        paint.setTextSize(dimen.dpi(12));

        zero = new TextTex("0", paint);
    }

    public void init(long max) {
        Ruler r = new Ruler(max, 1.0f, paint);
        rs.add(r);
    }

    public void animate(ColorSet colors) {
        this.colors = colors;
        colorAnim = new MyAnimation.Color(lineColor, colors.ruler);
        textColorAnim = new MyAnimation.Color(textColor, colors.rulerLabelColor);
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
        animationTick(t);

        final float hpadding = dimen.dpf(16);


        //todo draw zero only once

        final float zero = dimen.dpf(80);
        drawLine(hpadding, zero + 0, canvasW - 2 * hpadding, 1f);
        drawText(this.zero, hpadding, zero, 1f);

        final float dip50 = dimen.dpf(50);
        for (int ruler_i = 0, rsSize = rs.size(); ruler_i < rsSize; ruler_i++) {
            Ruler r = rs.get(ruler_i);

            float dy = dip50;

            for (int i = 1; i < 6; ++i) {
                //todo draw lines first, then text, so we can minimize program switch

                drawLine(hpadding, zero + dy * r.scale, canvasW - 2 * hpadding, r.alpha);
                drawText(r.values.get(i-1), hpadding, zero + dy * r.scale, r.alpha);

                dy += dip50;
            }
//            drawLine(hpadding, zero + root.dimen_chart_height-10, (canvasW - 2 * hpadding)/2, 1.0f);
        }
    }

    private void animationTick(long t) {
        //todo invalidate
        if (colorAnim != null) {
            lineColor = colorAnim.tick(t);
            if (colorAnim.ended) {
                colorAnim = null;
            }
        }
        if (textColorAnim != null) {
            textColor = textColorAnim.tick(t);
            if (textColorAnim.ended) {
                textColorAnim = null;
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
                }
            }
            if (r.toBeDeleted && r.alphaAnim == null && r.scaleAnim == null) {
                List<TextTex> values = r.values;
                for (int i1 = 0, valuesSize = values.size(); i1 < valuesSize; i1++) {
                    TextTex value = values.get(i1);
                    value.release();
                }
                rs.remove(i);
            }
        }
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
        GLES20.glUseProgram(lineProgram);
        MyGL.checkGlError2();
        LINE_COLOR_PARTS[0] = MyColor.red(lineColor) / 255f;
        LINE_COLOR_PARTS[1] = MyColor.green(lineColor) / 255f;
        LINE_COLOR_PARTS[2] = MyColor.blue(lineColor) / 255f;
        LINE_COLOR_PARTS[3] = alpha;
        GLES20.glUniform4fv(lineColorHandle, 1, LINE_COLOR_PARTS, 0);//todo try to bind only once

        GLES20.glEnableVertexAttribArray(linePositionHandle);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, lineVerticesVBO);
        GLES20.glVertexAttribPointer(linePositionHandle, POSITION_DATA_SIZE, GLES20.GL_FLOAT, false, STRIDE_BYTES, 0);


        final float scalex = 2.0f / canvasW;
        final float scaley = 2.0f / canvasH;
        Matrix.setIdentityM(MVP, 0);
        Matrix.translateM(MVP, 0, -1.0f, -1.0f, 0);
        Matrix.scaleM(MVP, 0, scalex, scaley, 1.0f);

        Matrix.translateM(MVP, 0, x, y, 0);
        Matrix.scaleM(MVP, 0, w, 1.0f, 1.0f);

        GLES20.glLineWidth(dimen.dpf(2.0f / 3.0f));
        GLES20.glUniformMatrix4fv(lineMVPHandle, 1, false, MVP, 0);
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
        private final TextPaint p;
        public Ruler(long maxValue, float scale, TextPaint p) {
            this.maxValue = maxValue;
            this.scale = scale;
            this.p = p;
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
