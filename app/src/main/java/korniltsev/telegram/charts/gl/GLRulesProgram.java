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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import korniltsev.telegram.charts.Dimen;

public final class GLRulesProgram {

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




    static final String texVertexShader =
            "uniform mat4 u_MVPMatrix;      \n"
                    + "attribute vec2 a_Position;     \n"
                    + "varying vec2 textureCoordinate;\n"
                    + "void main()                    \n"
                    + "{                              \n"
                    + "   gl_Position = u_MVPMatrix * vec4(a_Position.xy, 0.0, 1.0);   \n"
                    + "   textureCoordinate = a_Position;   \n"
                    + "}                              \n";

    static final String texFragmentShader =
            "precision mediump float;       \n"
                    + "varying highp vec2 textureCoordinate;\n;"
                    + "uniform sampler2D frame;\n;"
                    + "void main()                    \n"
                    + "{                              \n"
                    + "   gl_FragColor = texture2D(frame, textureCoordinate);     \n"
                    + "}                              \n";
    private final int lineMVPHandle;
    private final int linePositionHandle;
    private final int lineProgram;
    private final int lineVerticesVBO;
    private final int lineColorHandle;
    private final TextPaint paint;
    private final TextTex textZero;

    private final int texVerticesVBO;
    private final int texProgram;
    private final int texMVPHandle;
    private final int texPositionHandle;

    private float[] MVP = new float[16];

    public final int canvasW;
    public final int canvasH;

    private final Dimen dimen;


    final ChartViewGL root;


    static final float lineVertices[] = {
            0, 0,
            1, 0,
    };

    static final float texVertices[] = {
            0, 0,
            0, 1,
            1, 0,
            1, 1,
    };

    public GLRulesProgram(int canvasW, int canvasH, Dimen dimen, ChartViewGL root) {
        this.canvasW = canvasW;
        this.canvasH = canvasH;
        this.dimen = dimen;
        this.root = root;


        lineProgram = MyGL.createProgram(vertexShader, fragmentShader);
        lineMVPHandle = GLES20.glGetUniformLocation(lineProgram, "u_MVPMatrix");
        linePositionHandle = GLES20.glGetAttribLocation(lineProgram, "a_Position");
        lineColorHandle = GLES20.glGetUniformLocation(lineProgram, "u_color");

        texProgram = MyGL.createProgram(texVertexShader, texFragmentShader);
        texMVPHandle = GLES20.glGetUniformLocation(texProgram, "u_MVPMatrix");
        texPositionHandle = GLES20.glGetAttribLocation(texProgram, "a_Position");



        FloatBuffer buf1 = ByteBuffer.allocateDirect(lineVertices.length * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        buf1.put(lineVertices);
        buf1.position(0);

        FloatBuffer buf2 = ByteBuffer.allocateDirect(texVertices.length * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        buf2.put(texVertices);
        buf2.position(0);

        int[] vbos = new int[2];
        GLES20.glGenBuffers(2, vbos, 0);
        lineVerticesVBO = vbos[0];
        texVerticesVBO = vbos[1];
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, lineVerticesVBO);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, lineVertices.length * BYTES_PER_FLOAT, buf1, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, texVerticesVBO);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, texVertices.length * BYTES_PER_FLOAT, buf2, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(0xff96A2AA);
        paint.setTextSize(dimen.dpf(32f));

        textZero = new TextTex("0", paint);
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
            tex = textures[0];
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);

            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);

            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, b, 0);
            b.recycle();
        }
    }

    public static final int LINE_COLOR = 0xffE7E8E9;

    static final float[] LINE_COLOR_PARTS = new float[]{
            Color.red(LINE_COLOR) / 255f,
            Color.green(LINE_COLOR) / 255f,
            Color.blue(LINE_COLOR) / 255f,
            Color.alpha(LINE_COLOR) / 255f,
    };

    public static final int DEBUG_COLOR = 0xff000000;

    static final float[] DEBUG_COLOR_PARTS = new float[]{
            Color.red(DEBUG_COLOR) / 255f,
            Color.green(DEBUG_COLOR) / 255f,
            Color.blue(DEBUG_COLOR) / 255f,
            Color.alpha(DEBUG_COLOR) / 255f,
    };


    public final void draw(float t) {
        final float hpadding = dimen.dpf(16);
        float dy = dimen.dpf(80);
        for (int i = 0; i < 6; ++i) {
            //todo draw lines first, then text, so we can minimize program switch
            drawLine(hpadding, dy, canvasW - 2 * hpadding);
            drawText(textZero, hpadding, dy);
            dy += dimen.dpf(51);
        }
    }

    private void drawText(TextTex textZero, float x, float y) {
        GLES20.glUseProgram(texProgram);
        MyGL.checkGlError2();

//        GLES20.glUniform4fv(texColorHandle, 1, DEBUG_COLOR_PARTS, 0);//todo try to bind only once

        GLES20.glEnableVertexAttribArray(texPositionHandle);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, texVerticesVBO);
        GLES20.glVertexAttribPointer(texPositionHandle, POSITION_DATA_SIZE, GLES20.GL_FLOAT, false, STRIDE_BYTES, 0);


        final float scalex = 2.0f / canvasW;
        final float scaley = 2.0f / canvasH;
        Matrix.setIdentityM(MVP, 0);
        Matrix.translateM(MVP, 0, -1.0f, -1.0f, 0);
        Matrix.scaleM(MVP, 0, scalex, scaley, 1.0f);

        Matrix.translateM(MVP, 0, x, y, 0);
        Matrix.scaleM(MVP, 0, 100f,100f,1f);

        GLES20.glLineWidth(dimen.dpf(2.0f / 3.0f));
        GLES20.glUniformMatrix4fv(texMVPHandle, 1, false, MVP, 0);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textZero.tex);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, texVertices.length/ 2);

    }



    public void drawLine(float x, float y, float w) {
        GLES20.glUseProgram(lineProgram);
        MyGL.checkGlError2();
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
}
