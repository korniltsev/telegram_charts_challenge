package korniltsev.telegram.charts.gl;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.text.TextPaint;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import korniltsev.telegram.charts.data.ChartData;
import korniltsev.telegram.charts.ui.ColorSet;
import korniltsev.telegram.charts.ui.Dimen;
import korniltsev.telegram.charts.ui.MyAnimation;
import korniltsev.telegram.charts.ui.MyColor;

import static android.opengl.GLES10.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES10.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES10.glClear;
import static android.opengl.GLES10.glClearColor;

//import static android.opengl.GLES20.GL_FRAMEBUFFER;
//import static android.opengl.GLES20.GL_TEXTURE_2D;

// vertical line & label with values
public class Tooltip {
    private final float[] colorParts= new float[4];
    public final Shader shader;
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
    private MyTex framebuffer;
    private final ChartData data;
    //    private int fbo;
//    private int tex;
    private int lineColor;
    private MyAnimation.Color lineANim;
//    private TexShader texShader;

    public Tooltip(Shader shader, Dimen dimen, int w, int h, ColorSet colors, ChartData data) {
        this.data  = data;
        this.shader = shader;
        this.dimen = dimen;
        this.w = w;
        this.h = h;
        this.lineColor = colors.tooltipVerticalLine;


        FloatBuffer buf1 = ByteBuffer.allocateDirect(lineVertices.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        buf1.put(lineVertices);
        buf1.position(0);

        int[] vbos = new int[1];
        GLES20.glGenBuffers(1, vbos, 0);
        vbo = vbos[0];
        lineVerticesVBO = vbos[0];
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, lineVerticesVBO);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, lineVertices.length * 4, buf1, GLES20.GL_STATIC_DRAW);


//        prepareFramebuffer();

        texShaderFlip = new TexShader(true);//todo reuse and cleanup
        texShaderNoflip = new TexShader(false);//todo reuse and cleanup

    }


    public void animateTo(ColorSet colors) {
        lineANim = new MyAnimation.Color(MyAnimation.ANIM_DRATION, lineColor, colors.tooltipVerticalLine);
    }

    public static class Shader {

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

        private final int lineProgram;
        private final int lineMVPHandle;
        private final int linePositionHandle;
        private final int lineColorHandle;


        public Shader() {
            lineProgram = MyGL.createProgram(vertexShader, fragmentShader);
            lineMVPHandle = GLES20.glGetUniformLocation(lineProgram, "u_MVPMatrix");
            linePositionHandle = GLES20.glGetAttribLocation(lineProgram, "a_Position");
            lineColorHandle = GLES20.glGetUniformLocation(lineProgram, "u_color");
        }

    }



    final float vec1[] = new float[4];
    final float vec2[] = new float[4];
    final float []VIEW = new float[16];
    final float []MVP = new float[16];
    public boolean animationTick(long time) {
        boolean invalidate = false;
        if (lineANim != null) {
            lineColor = lineANim.tick(time);
            if (lineANim.ended) {
                lineANim = null;
            } else {
                invalidate = true;
            }
        }
        return invalidate;
    }

    public void draw(float[] proj, float[] chartMVP, int index) {
        if (framebuffer == null) {//or index change


            framebuffer = new MyTex(texShaderFlip, data, index, dimen);
            GLES20.glViewport(0, 0, w, h);
        }

        // calc vline pos
        vec1[0] = index;
        vec1[3] = 1;
        Matrix.multiplyMV(vec2, 0, chartMVP, 0, vec1, 0);
        float ndcx = (vec2[0] + 1f) / 2f;

        // draw wline
        Matrix.setIdentityM(VIEW, 0);
        Matrix.translateM(VIEW, 0, 0, dimen.dpf(80f), 0f);
        Matrix.scaleM(VIEW, 0, w, dimen.dpf(290), 1f);
        Matrix.translateM(VIEW, 0, ndcx, 0f, 0f);
        Matrix.multiplyMM(MVP, 0, proj, 0, VIEW, 0);

        GLES20.glUseProgram(shader.lineProgram);
        MyColor.set(colorParts, lineColor);
        GLES20.glUniform4fv(shader.lineColorHandle, 1, colorParts, 0);
        GLES20.glEnableVertexAttribArray(shader.linePositionHandle);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
        GLES20.glVertexAttribPointer(shader.linePositionHandle, 2, GLES20.GL_FLOAT, false, 8, 0);
        GLES20.glLineWidth(dimen.dpf(1f));
        GLES20.glUniformMatrix4fv(shader.lineMVPHandle, 1, false, MVP, 0);
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, lineVertices.length / 2);

        // ----------------
        // tooltip        todo draw over chart
        // ----------------
        //


        Matrix.setIdentityM(VIEW, 0);
        int texw = framebuffer.w;
        int texh = framebuffer.h;
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




    }
    // actual tooltip
    private  static class MyTex {

        private final int fbo;
        private final int tex;

        private final TexShader shader_;
        private final ChartData data;
        private final int index;
        private final Dimen dimen;
        public int w;
        public int h;

        public MyTex(TexShader shader, ChartData data, int index, Dimen dimen) {
            this.shader_ = shader;
            this.data = data;
            this.index = index;
            this.dimen = dimen;
            TextTex text = prepareTextTextures();
            w = text.w;
            h = text.h * 2;
            //todo delete previous
//        glDeleteFramebuffers(1, &fbo);
            int[] fbos = new int[1];
            GLES20.glGenFramebuffers(1, fbos, 0);
            fbo = fbos[0];
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo);
            MyGL.checkGlError2();

            int []textures = new int[1];
            GLES20.glGenTextures(1, textures, 0);
            tex = textures[0];
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex);

//            TextTex t = prepareTextTextures();
            MyGL.checkGlError2();
//            int width = w;
//            int h = h;
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGB, w, h, 0, GLES20.GL_RGB, GLES20.GL_UNSIGNED_BYTE, null);

            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);


            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, tex, 0);

            int check = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
            if(check == GLES20.GL_FRAMEBUFFER_COMPLETE) {
                System.out.println(check);
            }
            drawTooltip(text);


            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
//            if (texShader == null) {
//                texShader = new TexShader();
//            }
        }

        private void drawTooltip(TextTex t) {
            GLES20.glViewport(0, 0, w, h);
            //todo probably need glViewPort()
            glClearColor(
                    1f,
                    0f,
                    0f,
                    1.0f
            );
            glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);


            float[] PROJ = new float[16];
            Matrix.orthoM(PROJ, 0, 0, w, 0, h, -1.0f, 1.0f);
            float[] VIEW = new float[16];
            float[] MVP = new float[16];
            Matrix.setIdentityM(VIEW, 0);
            Matrix.scaleM(VIEW, 0, t.w, t.h,1);
            Matrix.multiplyMM(MVP, 0, PROJ, 0, VIEW, 0);

            GLES20.glUseProgram(shader_.texProgram);
            MyGL.checkGlError2();
            GLES20.glEnableVertexAttribArray(shader_.texPositionHandle);
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, shader_.texVerticesVBO);
            GLES20.glVertexAttribPointer(shader_.texPositionHandle, 2, GLES20.GL_FLOAT, false, 8, 0);
            GLES20.glUniformMatrix4fv(shader_.texMVPHandle, 1, false, MVP, 0);
            GLES20.glUniform1f(shader_.texAlphaHandle, 1f);

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t.tex[0]);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, TexShader.texVertices.length / 2);




//            float[] PROJ = new float[16];
//            Matrix.orthoM(PROJ, 0, 0, w, 0, h, -1.0f, 1.0f);
////            float[] VIEW = new float[16];
////            float[] MVP = new float[16];
//            Matrix.setIdentityM(VIEW, 0);
//            Matrix.scaleM(VIEW, 0, t.w, t.h,1);
//            Matrix.translateM(VIEW, 0, 0, t.h, 0);
//            Matrix.multiplyMM(MVP, 0, PROJ, 0, VIEW, 0);
//
//            GLES20.glUseProgram(shader_.texProgram);
//            MyGL.checkGlError2();
//            GLES20.glEnableVertexAttribArray(shader_.texPositionHandle);
//            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, shader_.texVerticesVBO);
//            GLES20.glVertexAttribPointer(shader_.texPositionHandle, 2, GLES20.GL_FLOAT, false, 8, 0);
//            GLES20.glUniformMatrix4fv(shader_.texMVPHandle, 1, false, MVP, 0);
//            GLES20.glUniform1f(shader_.texAlphaHandle, 1f);
//
//            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t.tex[0]);
//            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, TexShader.texVertices.length / 2);





            int width = w;
            int height = h;
            ByteBuffer buffer = ByteBuffer.allocate(width * height * 4);
            GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(buffer);

            System.out.println();

        }

        public TextTex prepareTextTextures() {
            TextPaint p = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(Color.BLACK);
            p.setAntiAlias(true);
            p.setTextSize(dimen.dpf(16f));
            TextTex t = new TextTex("Sat, Feb 24", p);
            return t;
        }
    }
}
