package korniltsev.telegram.charts.gl;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.text.TextPaint;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import korniltsev.telegram.charts.data.ChartData;
import korniltsev.telegram.charts.ui.ColorSet;
import korniltsev.telegram.charts.ui.Dimen;
import korniltsev.telegram.charts.ui.MyColor;

import static android.opengl.GLES10.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES10.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES10.glClear;
import static android.opengl.GLES10.glClearColor;

// actual tooltip
//todo rename
class TooltipFramebuffer {

    private final int fbo;
    public final int tex;

    private final TexShader shader_;
    private final ChartData data;
    private final int index;
    private final Dimen dimen;
    public int w;
    public int h;
    private final float[] colorParts = new float[4];
    private int dateColor;

    public TooltipFramebuffer(TexShader shader, ChartData data, int index, Dimen dimen, ColorSet set) {
        this.dateColor = set.tooltipTitleColor;
        this.shader_ = shader;
        this.data = data;
        this.index = index;
        this.dimen = dimen;
        prepareTextTexturesAndMeasure();

        //todo delete previous
//        glDeleteFramebuffers(1, &fbo);
        int[] fbos = new int[1];
        GLES20.glGenFramebuffers(1, fbos, 0);
        fbo = fbos[0];
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo);
        MyGL.checkGlError2();

        int[] textures = new int[1];
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
        if (check == GLES20.GL_FRAMEBUFFER_COMPLETE) {
            System.out.println(check);
        }
//        drawTooltip(text);


        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
//            if (texShader == null) {
//                texShader = new TexShader();
//            }
    }

    public final void drawTooltip() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo);
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
        Matrix.scaleM(VIEW, 0, title.w, title.h,1);
        Matrix.multiplyMM(MVP, 0, PROJ, 0, VIEW, 0);

        GLES20.glUseProgram(shader_.texProgram);
        MyGL.checkGlError2();
        GLES20.glEnableVertexAttribArray(shader_.texPositionHandle);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, shader_.texVerticesVBO);
        GLES20.glVertexAttribPointer(shader_.texPositionHandle, 2, GLES20.GL_FLOAT, false, 8, 0);
        GLES20.glUniformMatrix4fv(shader_.texMVPHandle, 1, false, MVP, 0);
        GLES20.glUniform1f(shader_.texAlphaHandle, 1f);
        MyColor.set(colorParts, dateColor);
        GLES20.glUniform4fv(shader_.u_color, 1, colorParts, 0);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, title.tex[0]);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, TexShader.texVertices.length / 2);

        int width = w;
        int height = h;
        ByteBuffer buffer = ByteBuffer.allocate(width * height * 4);
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);

        System.out.println();

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

    }

    TextTex title;
    List<TextTex> values = new ArrayList<>();
    List<TextTex> names = new ArrayList<>();

    public void prepareTextTexturesAndMeasure() {
        long value = data.data[0].values[index];
        String date = Tooltip.dateFormat.format(value);

        TextPaint p = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.BLACK);
        p.setAntiAlias(true);
        p.setTextSize(dimen.dpf(16f));
        title = new TextTex(date, p);

        w = title.w;
        h = title.h * 2;
    }

    public void release() {
        //todo
    }
}
