package korniltsev.telegram.charts.gl;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.text.TextPaint;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import korniltsev.telegram.charts.data.ChartData;
import korniltsev.telegram.charts.data.ColumnData;
import korniltsev.telegram.charts.ui.ColorSet;
import korniltsev.telegram.charts.ui.Dimen;
import korniltsev.telegram.charts.ui.MyAnimation;
import korniltsev.telegram.charts.ui.MyColor;

import static android.opengl.GLES10.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES10.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES10.glClear;
import static android.opengl.GLES10.glClearColor;

// actual tooltip
//todo rename
class TooltipFramebuffer {

    public static final int PADDING_BETWEEN_VALUES = 20;
    //    public static final int HEIGHT = 62;
    public static final int LEFT_RIGHT_PADDING = 12;
    public static final float FONT_SIZE_16 = 12f;
    public static final float FONT_SIZE22 = 16f;
    public static final int PADDING_BETWEEN_NAME_AND_VALUE = 20;
    //    public static final int VMARGIN = 8;
    private final int fakeShadowSimulatorLine;
    private ColorSet colorset;
    int[] fbos = new int[1];
    private final int fbo;
    int[] textures = new int[1];
    public final int tex;

    private final TexShader shader_;
    private final ChartData data;
    private final int index;
    private final Dimen dimen;
    private int bgColor;
    public int w;
    public int h;
    private final float[] colorParts = new float[4];
    private int dateColor;
    private MyAnimation.Color bgAnim;
    private MyAnimation.Color titleColorAnim;
    private final SimpleShader simple;

    static final float shadowSimulatorLines[] = {
            0, 0,
            0, 1,
            1, 1,
            1, 0,
            0, 0,
    };
    int[] vbos = new int[1];
    private int fakeShadowColor;
    private MyAnimation.Color fakeShadowColorAnim;
    private boolean released;
    public float realH;
    public int realW;

    private static final DecimalFormat formatter;

    static {
        formatter = (DecimalFormat) NumberFormat.getInstance(Locale.US);
        DecimalFormatSymbols symbols = formatter.getDecimalFormatSymbols();

        symbols.setGroupingSeparator(' ');
        formatter.setDecimalFormatSymbols(symbols);

    }

    public TooltipFramebuffer(TexShader shader, ChartData data, int index, Dimen dimen, ColorSet set, boolean[] checked, SimpleShader simple) {
        this.dateColor = set.tooltipTitleColor;
        this.bgColor = set.tooltipBGColor;
        this.shader_ = shader;
        this.colorset = set;
        this.data = data;
        this.index = index;
        this.dimen = dimen;
        this.simple = simple;
        fakeShadowColor = set.tooltipFakeSHadowColor;

        FloatBuffer buf2 = ByteBuffer.allocateDirect(shadowSimulatorLines.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        buf2.put(shadowSimulatorLines);
        buf2.position(0);


        GLES20.glGenBuffers(1, vbos, 0);
//        lineVerticesVBO = vbos[0];
        fakeShadowSimulatorLine = vbos[0];

//        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, lineVerticesVBO);
//        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, lineVertices.length * BYTES_PER_FLOAT, buf1, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, fakeShadowSimulatorLine);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, shadowSimulatorLines.length * 4, buf2, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        prepareTextTexturesAndMeasure(checked);

        //todo delete previous
//        glDeleteFramebuffers(1, &fbo);

        GLES20.glGenFramebuffers(1, fbos, 0);
        fbo = fbos[0];
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo);
        MyGL.checkGlError2();


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
//            System.out.println(check);
        }
//        drawTooltip(text);


        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
//            if (texShader == null) {
//                texShader = new TexShader();
//            }

    }

    public final void drawTooltip() {
        realH = dimen.dpf(8) * 2 + title.h;
        realW = w;


        for (int i = 0; i < lines.size(); i++) {
            Line l = lines.get(i);
            TextTex name = l.name;
            float lh = l.h * l.visibility;
            realH += lh;
        }

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo);
        GLES20.glViewport(0, 0, w, h);
        //todo probably need glViewPort()
        glClearColor(
                MyColor.red(bgColor) / 255f,
                MyColor.green(bgColor) / 255f,
                MyColor.blue(bgColor) / 255f,
                1.0f
        );
        glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);

        GLES20.glUseProgram(shader_.texProgram);
        MyGL.checkGlError2();

        float titleY = realH - title.h - dimen.dpi(8);
        drawText(title, dimen.dpf(LEFT_RIGHT_PADDING), titleY, dateColor, 1f);
        float ity = titleY;

//        float currentX = dimen.dpf(LEFT_RIGHT_PADDING);

        for (int i = 0; i < lines.size(); i++) {
            Line l = lines.get(i);
            TextTex name = l.name;
            float lh = l.h * l.visibility;
//            realH += lh;

//            int ny = titleY - dimen.dpi(VMARGIN) - i * (dimen.dpi(VMARGIN) + name.h);
            drawText(name, dimen.dpf(LEFT_RIGHT_PADDING), ity-l.h, dateColor, l.visibility);
            TextTex value = l.value;
            float vx = w - dimen.dpi(LEFT_RIGHT_PADDING) - value.w;
            int tcolor = colorset.mapLineText(value.color);
            drawText(value, vx, ity-l.h, tcolor, l.visibility);
            ity -= lh;
//            currentX += Math.max(name.w, value.w) + dimen.dpf(PADDING_BETWEEN_VALUES);
        }

//        int width = w;
//        int height = h;
//        ByteBuffer buffer = ByteBuffer.allocate(width * height * 4);
//        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
//        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
//        bitmap.copyPixelsFromBuffer(buffer);

//        System.out.println();


        Matrix.setIdentityM(VIEW, 0);
//        Matrix.translateM(VIEW, 0, 0, dimen.dpf(80f), 0f);
        Matrix.scaleM(VIEW, 0, w, realH, 1f);
//        Matrix.translateM(VIEW, 0, ndcx, 0f, 0f);
        Matrix.multiplyMM(MVP, 0, PROJ, 0, VIEW, 0);

        GLES20.glUseProgram(simple.program);
        MyColor.set(colorParts, fakeShadowColor);
        GLES20.glUniform4fv(simple.colorHandle, 1, colorParts, 0);
        GLES20.glEnableVertexAttribArray(simple.positionHandle);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, fakeShadowSimulatorLine);
        GLES20.glVertexAttribPointer(simple.positionHandle, 2, GLES20.GL_FLOAT, false, 8, 0);
        GLES20.glLineWidth(dimen.dpf(2f));
        GLES20.glUniformMatrix4fv(simple.MVPHandle, 1, false, MVP, 0);
        GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, shadowSimulatorLines.length / 2);


        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);


    }

    float[] PROJ = new float[16];
    float[] VIEW = new float[16];
    float[] MVP = new float[16];

    private void drawText(TextTex text, float x, float y, int color, float alpha) {

        Matrix.setIdentityM(VIEW, 0);
        Matrix.translateM(VIEW, 0, x, y, 0);
        Matrix.scaleM(VIEW, 0, text.w, text.h, 1);
        Matrix.multiplyMM(MVP, 0, PROJ, 0, VIEW, 0);


        GLES20.glEnableVertexAttribArray(shader_.texPositionHandle);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, shader_.texVerticesVBO);
        GLES20.glVertexAttribPointer(shader_.texPositionHandle, 2, GLES20.GL_FLOAT, false, 8, 0);
        GLES20.glUniform1f(shader_.texAlphaHandle, alpha);

        GLES20.glUniformMatrix4fv(shader_.texMVPHandle, 1, false, MVP, 0);
        MyColor.set(colorParts, color);
        GLES20.glUniform4fv(shader_.u_color, 1, colorParts, 0);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, text.tex[0]);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, TexShader.texVertices.length / 2);
    }

    TextTex title;
    List<Line> lines = new ArrayList<Line>();


    public void prepareTextTexturesAndMeasure(boolean[] checked) {
        long dateTimestamp = data.data[0].values[index];
        String date;
        if (data.index == -1) {
            date = Tooltip.timeFormat.format(dateTimestamp);
        } else {
            date = Tooltip.dateFormat.format(dateTimestamp);
        }
        Typeface medium = Typeface.create("sans-serif-medium", Typeface.NORMAL);
        TextPaint p16 = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        p16.setColor(Color.BLACK);
        p16.setAntiAlias(true);
        p16.setTextSize(dimen.dpf(FONT_SIZE_16));


        p16.setTypeface(medium);
        title = new TextTex(date, p16);
        p16.setTypeface(null);

        long total = 0;
        for (int i = 1; i < data.data.length; i++) {
            ColumnData datum = data.data[i];
            String name1 = datum.name;
            long v = datum.values[index];
            total += v;
            TextTex name = new TextTex(name1, p16);
            name.color = title.color;
            p16.setTypeface(medium);

            String text = formatter.format(v);
            TextTex value = new TextTex(text, p16);
            p16.setTypeface(null);
            value.color = datum.color;
            Line e = new Line(name, value, value.h + dimen.dpi(4), datum.id);
            if (checked[i - 1]) {
                e.visibility = 1f;
            } else {
                e.visibility = 0f;
            }
            lines.add(e);
        }

        if (data.stacked && data.type == ColumnData.Type.bar) {
            String name1 = "All";
            long v = total;
            total += v;
            TextTex name = new TextTex(name1, p16);
            name.color = title.color;
            p16.setTypeface(medium);

            String text = formatter.format(v);
            TextTex value = new TextTex(text, p16);
            p16.setTypeface(null);
            value.color = dateColor;
            Line e = new Line(name, value, value.h + dimen.dpi(4), "unknown");
            e.visibility = 1f;
            lines.add(e);
        }
        int vw = -1;
        for (int i = 0; i < lines.size(); i++) {
            Line line = lines.get(i);
            vw = Math.max(vw, line.name.w + line.value.w + dimen.dpi(PADDING_BETWEEN_NAME_AND_VALUE));
        }


        //todo rename to maxW maxH
        w = 2 * dimen.dpi(LEFT_RIGHT_PADDING) + Math.max(title.w + 0/*arror*/, vw);
        int hspace = dimen.dpi(8) * 2;//top and bottom margin
        h = hspace + title.h + lines.get(0).h * lines.size();

        Matrix.orthoM(PROJ, 0, 0, w, 0, h, -1.0f, 1.0f);
    }

    public void release() {
        if (released) {
            return;
        }
        released = true;
        title.release();
        for (Line line : lines) {
            line.name.release();
            line.value.release();
        }
        GLES20.glDeleteFramebuffers(1, fbos, 0);
        GLES20.glDeleteTextures(1, textures, 0);
    }

    public boolean animtionTick(long time) {
        boolean invalidate = false;
//        for (Line line : lines) {
//
//        }
        if (bgAnim != null) {
            bgColor = bgAnim.tick(time);
            if (bgAnim.ended) {
                bgAnim = null;
            } else {
                invalidate = true;
            }
        }
        if (titleColorAnim != null) {
            dateColor = titleColorAnim.tick(time);
            if (titleColorAnim.ended) {
                titleColorAnim = null;
            } else {
                invalidate = true;
            }
        }
        if (fakeShadowColorAnim != null) {
            fakeShadowColor = fakeShadowColorAnim.tick(time);
            if (fakeShadowColorAnim.ended) {
                fakeShadowColorAnim = null;
            } else {
                invalidate = true;
            }
        }

        for (int i = 0; i < lines.size(); i++) {
            Line line = lines.get(i);
            MyAnimation.Float a = line.visibilityANim;
            if (a != null) {
                line.visibility = a.tick(time);
                if (a.ended) {
                    line.visibilityANim = null;
                } else {
                    invalidate = true;
                }
            }

        }
        return invalidate;
    }

    public void animateToColors(ColorSet c, long duration) {
        this.colorset = c;
        bgAnim = new MyAnimation.Color(duration, bgColor, c.tooltipBGColor);
        titleColorAnim = new MyAnimation.Color(duration, dateColor, c.tooltipTitleColor);
        fakeShadowColorAnim = new MyAnimation.Color(duration, fakeShadowColor, c.tooltipFakeSHadowColor);
    }

    public void setChecked(String id, boolean isChecked) {
        for (Line line : lines) {
            if (line.id.equals(id)) {
                line.visibilityANim = new MyAnimation.Float(208, line.visibility, isChecked ? 1f : 0f);
                break;
            }
        }
    }

    public static class Line {
        public float visibility = 1f;
        public TextTex name;
        public TextTex value;
        public final int h;
        public MyAnimation.Float visibilityANim;
        final String id;
        public Line(TextTex name, TextTex value, int h, String id) {
            this.name = name;
            this.value = value;
            this.h = h;
            this.id = id;
        }
    }
}
