package korniltsev.telegram.charts.gl;

import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

import korniltsev.telegram.charts.data.ChartData;
import korniltsev.telegram.charts.ui.ColorSet;
import korniltsev.telegram.charts.ui.Dimen;
import korniltsev.telegram.charts.ui.MyAnimation;
import korniltsev.telegram.charts.ui.MyColor;

//import static android.opengl.GLES20.GL_FRAMEBUFFER;
//import static android.opengl.GLES20.GL_TEXTURE_2D;

// vertical line & label with values
public class Tooltip {
    public static final SimpleDateFormat dateFormat = new SimpleDateFormat("E, MMM d", Locale.US);
    static {
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private final float[] colorParts= new float[4];
    public final SimpleShader shader;
    final Dimen dimen;
    private final int vbo;

    static final float lineVertices[] = {
            0, 0,
            0, 1,
    };
    private final int lineVerticesVBO;
    private final int w;
    private final int h;
    private final TexShader texShaderFlip;
    private final TexShader texShaderNoflip
            ;
    private final int[] vbos;
    private ColorSet colorsSet;
    private TooltipFramebuffer framebuffer;
    private final ChartData data;
    //    private int fbo;
//    private int tex;
    private int lineColor;
    private SimpleShader simple;
    private MyAnimation.Color lineANim;
    private int fbindex;
    private float ndcx;
    private boolean released;
    public final ChartViewGL rot;
//    private TexShader texShader;

    public Tooltip(Dimen dimen, int w, int h, ColorSet colors, ChartData data, SimpleShader simple, ChartViewGL rot) {
        this.data = data;
        this.colorsSet = colors;
        this.shader = simple;
        this.dimen = dimen;
        this.w = w;
        this.h = h;
        this.lineColor = colors.ruler;
        this.simple = simple;
        this.rot = rot;


        FloatBuffer buf1 = ByteBuffer.allocateDirect(lineVertices.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        buf1.put(lineVertices);
        buf1.position(0);

        vbos = new int[1];
        GLES20.glGenBuffers(1, vbos, 0);
        vbo = vbos[0];
        lineVerticesVBO = vbos[0];
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, lineVerticesVBO);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, lineVertices.length * 4, buf1, GLES20.GL_STATIC_DRAW);


//        prepareFramebuffer();

        texShaderFlip = new TexShader(true, true);//todo reuse and cleanup
        texShaderNoflip = new TexShader(false, false);//todo reuse and cleanup

    }


    public void animateTo(ColorSet colors, long duration) {
        this.colorsSet = colors;
        lineANim = new MyAnimation.Color(duration, lineColor, colors.ruler);
        if (framebuffer != null) {
            framebuffer.animateToColors(colors, duration);
        }
    }

//    public static class Shader {//todo replace with simple
//
//        static final String vertexShader =
//                "uniform mat4 u_MVPMatrix;      \n"
//                        + "attribute vec2 a_Position;     \n"
//                        + "void main()                    \n"
//                        + "{                              \n"
//                        + "   gl_Position = u_MVPMatrix * vec4(a_Position.xy, 0.0, 1.0);   \n"
//                        + "}                              \n";
//
//        static final String fragmentShader =
//                "precision mediump float;       \n"
//                        + "uniform vec4 u_color;       \n"
//                        + "void main()                    \n"
//                        + "{                              \n"
//                        + "   gl_FragColor = u_color;     \n"
//                        + "}                              \n";
//
//        private final int lineProgram;
//        private final int lineMVPHandle;
//        private final int linePositionHandle;
//        private final int lineColorHandle;
//
//
//        public Shader() {
//            lineProgram = MyGL.createProgram(vertexShader, fragmentShader);
//            lineMVPHandle = GLES20.glGetUniformLocation(lineProgram, "u_MVPMatrix");
//            linePositionHandle = GLES20.glGetAttribLocation(lineProgram, "a_Position");
//            lineColorHandle = GLES20.glGetUniformLocation(lineProgram, "u_color");
//        }
//
//    }



    final float vec1[] = new float[4];
    final float vec2[] = new float[4];
    final float []VIEW = new float[16];
    final float []MVP = new float[16];
    public boolean animationTick(long time, int index, boolean[]checked) {
        boolean invalidate = false;
        if (lineANim != null) {
            lineColor = lineANim.tick(time);
            if (lineANim.ended) {
                lineANim = null;
            } else {
                invalidate = true;
            }
        }
        if (framebuffer != null) {
            framebuffer.animtionTick(time);
        }

        if (framebuffer == null || index != fbindex) {//or index change
            if (framebuffer != null) {
                framebuffer.release();
                framebuffer = null;
            }
            fbindex = index;
            if (index != -1) {
                framebuffer = new TooltipFramebuffer(texShaderFlip, data, index, dimen, colorsSet, checked, simple);
            }
        }

        return invalidate;
    }

//    public void draw(float[] proj, float[] chartMVP, int index) {
//        drawVLine(proj, chartMVP, index);
//        drawTooltip(proj);
//    }

    public void drawTooltip(float[] proj) {
//        GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
        // ndcx
//        float
//        Log.d("FUCK", " " + ndcx);
        float dip16 = dimen.dpf(16);
        float xpos;
        if (framebuffer.w <= w - 2 * dip16) {
            xpos = ndcx * w - dip16;
            if (xpos < dip16) {
                xpos = dip16;
            } else if (xpos + framebuffer.w > w - dip16) {
                xpos = w - framebuffer.w - dip16;
            }
        } else {
            xpos = w / 2f - framebuffer.w / 2f;
        }
        framebuffer.drawTooltip();
        GLES20.glViewport(0, 0, w, h);

        Matrix.setIdentityM(VIEW, 0);
        int texw = framebuffer.w;
        int texh = framebuffer.h;
        Matrix.translateM(VIEW, 0, xpos, dimen.dpf(80 + 290 - TooltipFramebuffer.HEIGHT) + rot.checkboxesHeight, 0f);
        Matrix.scaleM(VIEW, 0, texw, texh, 1f);

        Matrix.multiplyMM(MVP, 0, proj, 0, VIEW, 0);


//        float[] VIEW = new float[16];
//        float[] MVP = new float[16];
//        Matrix.setIdentityM(VIEW, 0);
//        Matrix.scaleM(VIEW, 0, t.w, t.h,1);
//        Matrix.multiplyMM(MVP, 0, PROJ, 0, VIEW, 0);

        GLES20.glUseProgram(texShaderNoflip.texProgram);
        MyGL.checkGlError2();
        GLES20.glEnableVertexAttribArray(texShaderNoflip.texPositionHandle);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, texShaderNoflip.texVerticesVBO);
        GLES20.glVertexAttribPointer(texShaderNoflip.texPositionHandle, 2, GLES20.GL_FLOAT, false, 8, 0);
        GLES20.glUniformMatrix4fv(texShaderNoflip.texMVPHandle, 1, false, MVP, 0);
        GLES20.glUniform1f(texShaderNoflip.texAlphaHandle, 1f);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, framebuffer.tex);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, TexShader.texVertices.length / 2);

//        GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
    }

    public void drawVLine(float[] proj, float[] chartMVP, int index) {


        // draw wline
        Matrix.setIdentityM(VIEW, 0);
        Matrix.translateM(VIEW, 0, 0, dimen.dpf(80f) + rot.checkboxesHeight, 0f);
        Matrix.scaleM(VIEW, 0, w, dimen.dpf(290), 1f);
        Matrix.translateM(VIEW, 0, ndcx, 0f, 0f);
        Matrix.multiplyMM(MVP, 0, proj, 0, VIEW, 0);

        GLES20.glUseProgram(shader.program);
        MyColor.set(colorParts, lineColor);
        GLES20.glUniform4fv(shader.colorHandle, 1, colorParts, 0);
        GLES20.glEnableVertexAttribArray(shader.positionHandle);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
        GLES20.glVertexAttribPointer(shader.positionHandle, 2, GLES20.GL_FLOAT, false, 8, 0);
        GLES20.glLineWidth(dimen.dpf(1f));
        GLES20.glUniformMatrix4fv(shader.MVPHandle, 1, false, MVP, 0);
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, lineVertices.length / 2);
    }

    public void calcPos(float[] chartMVP, int index) {
        // calc vline pos
        vec1[0] = index;
        vec1[3] = 1;
        Matrix.multiplyMV(vec2, 0, chartMVP, 0, vec1, 0);
        ndcx = (vec2[0] + 1f) / 2f;
    }

    public void rlease() {
        if (released) {
            return;
        }
        released = true;
        GLES20.glDeleteBuffers(1, vbos, 0);
        texShaderFlip.release();
        texShaderNoflip.release();
        if (framebuffer != null) {
            framebuffer.release();
            framebuffer = null;
        }
    }
}
