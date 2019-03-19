package korniltsev.telegram.charts.gl;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import korniltsev.telegram.charts.Dimen;

public final class GLRulesProgram {

    private static final int BYTES_PER_FLOAT = 4;
    private static final int STRIDE_BYTES = 2 * BYTES_PER_FLOAT;
    private static final int POSITION_DATA_SIZE = 2;
    public static final int OVERLAY_COLOR = 0xffE7E8E9;
//    public static final int BORDER_COLOR = 0x334b87b4;

    final String vertexShader =
            "uniform mat4 u_MVPMatrix;      \n"
                    + "attribute vec2 a_Position;     \n"
                    + "void main()                    \n"
                    + "{                              \n"
                    + "   gl_Position = u_MVPMatrix * vec4(a_Position.xy, 0.0, 1.0);   \n"
                    + "}                              \n";

    final String fragmentShader =
            "precision mediump float;       \n"
                    + "uniform vec4 u_color;       \n"
                    + "void main()                    \n"
                    + "{                              \n"
                    + "   gl_FragColor = u_color;     \n"
                    + "}                              \n";
    private final int MVPHandle;
    private final int positionHandle;
    private final int programHorizontalLine;
    private final int vbo;
    private final FloatBuffer buf1;
    private final int colorHandle;
    private final TextPaint paint;
    private final TextTex textZero;

    private float[] MVP = new float[16];

    public final int canvasW;
    public final int canvasH;

    private final Dimen dimen;


    final ChartViewGL root;

//    float left = 0.5f;
//    float right = 1.0f;

    float vertices[] = {
            0, 0,
            1, 0,
//            1, 1,

//            1, 1,
//            1, 0,
//            0, 0,
    };

    public GLRulesProgram(int canvasW, int canvasH, Dimen dimen, ChartViewGL root) {
        this.canvasW = canvasW;
        this.canvasH = canvasH;
        this.dimen = dimen;
        this.root = root;

        buf1 = ByteBuffer.allocateDirect(vertices.length * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        buf1.put(vertices);
        buf1.position(0);


        programHorizontalLine = MyGL.createProgram(vertexShader, fragmentShader);
        MVPHandle = GLES20.glGetUniformLocation(programHorizontalLine, "u_MVPMatrix");
        positionHandle = GLES20.glGetAttribLocation(programHorizontalLine, "a_Position");
        colorHandle = GLES20.glGetUniformLocation(programHorizontalLine, "u_color");


        int[] vbos = new int[1];
        GLES20.glGenBuffers(1, vbos, 0);
        vbo = vbos[0];
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertices.length * BYTES_PER_FLOAT, buf1, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(0xff96A2AA);
        paint.setTextSize(dimen.dpf(32f));

        getTextTexture("100");
        textZero = new TextTex("0", paint);
    }

    public void getTextTexture(String text) {

        System.out.println();
    }

    public class TextTex {
        final String text;
        final int tex;

        TextTex(String text, TextPaint p) {
            this.text = text;

            int w = (int) Math.ceil(paint.measureText(text));
            StaticLayout staticLayout = new StaticLayout(text, 0, text.length(), paint, w, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            int h = staticLayout.getHeight();
            Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b);
            staticLayout.draw(c);



            int[] textures = new int[1];
            GLES20.glGenTextures(1, textures, 0);
//            MyGL.checkGlError2();
            tex = textures[0];
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
//            MyGL.checkGlError2();

            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
//            MyGL.checkGlError2();
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
//            MyGL.checkGlError2();

            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
//            MyGL.checkGlError2();
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
//            MyGL.checkGlError2();

            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, b, 0);
//            MyGL.checkGlError2();
            b.recycle();
        }
    }

    float[] color_overlay = new float[]{ //todo try to do only once
            Color.red(OVERLAY_COLOR) / 255f,
            Color.green(OVERLAY_COLOR) / 255f,
            Color.blue(OVERLAY_COLOR) / 255f,
            Color.alpha(OVERLAY_COLOR) / 255f,
    };

//    float [] color_border = new float[]{ //todo try to do only once
//            Color.red(BORDER_COLOR) / 255f,
//            Color.green(BORDER_COLOR) / 255f,
//            Color.blue(BORDER_COLOR) / 255f,
//            Color.alpha(BORDER_COLOR) / 255f,
//    };

    public final void draw(float t) {
        final float hpadding = dimen.dpf(16);
//        final float scrollerW = this.canvasW - 2 * hpadding;
//        final float vline1w = dimen.dpf(2f);
//        final float vline2h = dimen.dpf(1f);



//        drawRect(0,0,100, 100);
        float dy = dimen.dpf(80);
        for (int i = 0; i < 6; ++i) {
            //todo draw lines first, then text, so we can minimize program switch
            drawLine(hpadding, dy, canvasW - 2 * hpadding);
            drawText(textZero, hpadding, dy);
            dy += dimen.dpf(51);
        }

//        if (left != 0.0f) {
//        }
//        if (right != 1.0f) {
//            drawRect(hpadding + scrollerW * right, root.dimen_v_padding8, scrollerW * (1.0f-right), root.dimen_scrollbar_height);
//        }





//        GLES20.glUniform4fv(colorHandle, 1, color_border, 0);
//        drawRect(hpadding + scrollerW * left, root.dimen_v_padding8, vline1w, root.dimen_scrollbar_height);
//        drawRect(hpadding + scrollerW * right - vline1w, root.dimen_v_padding8, vline1w, root.dimen_scrollbar_height);
//        float l = hpadding + scrollerW * left + vline1w;
//        float r = hpadding + scrollerW * right - vline1w;
//        drawRect(l, root.dimen_v_padding8, r-l, vline2h);
//        drawRect(l, root.dimen_v_padding8 + root.dimen_scrollbar_height - vline2h, r-l, vline2h);
    }

    private void drawText(TextTex textZero, float x, float y) {
        final float scalex = 2.0f / canvasW;
        final float scaley = 2.0f / canvasH;
        Matrix.setIdentityM(MVP, 0);
        Matrix.translateM(MVP, 0, -1.0f, -1.0f, 0);
        Matrix.scaleM(MVP, 0, scalex, scaley, 1.0f);

        Matrix.translateM(MVP, 0, x, y, 0);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textZero.tex);
//        GLES20.
//        GLES20.glEnable(GLES20.GL_TEXTURE_2D);
//        GLES20.glDisable(GLES20.GL_TEXTURE_2D);

//        GL

    }



    public void drawLine(float x, float y, float w) {
        GLES20.glUseProgram(programHorizontalLine);
        MyGL.checkGlError2();
        GLES20.glUniform4fv(colorHandle, 1, color_overlay, 0);

        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
        GLES20.glVertexAttribPointer(positionHandle, POSITION_DATA_SIZE, GLES20.GL_FLOAT, false, STRIDE_BYTES, 0);


        final float scalex = 2.0f / canvasW;
        final float scaley = 2.0f / canvasH;
        Matrix.setIdentityM(MVP, 0);
        Matrix.translateM(MVP, 0, -1.0f, -1.0f, 0);
        Matrix.scaleM(MVP, 0, scalex, scaley, 1.0f);

        Matrix.translateM(MVP, 0, x, y, 0);
        Matrix.scaleM(MVP, 0, w, 1.0f, 1.0f);

        GLES20.glLineWidth(dimen.dpf(2.0f / 3.0f));
        GLES20.glUniformMatrix4fv(MVPHandle, 1, false, MVP, 0);
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, vertices.length / 2);

    }

//    public void setLeftRight(float left, float right) {
//        this.left = left;
//        this.right = right;
//        Log.d("OVERLAY", " " + left + " " + right);
//    }
}
