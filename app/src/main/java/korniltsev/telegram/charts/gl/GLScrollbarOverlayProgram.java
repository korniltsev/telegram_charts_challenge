package korniltsev.telegram.charts.gl;

import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import korniltsev.telegram.charts.R;
import korniltsev.telegram.charts.ui.ColorSet;
import korniltsev.telegram.charts.ui.Dimen;
import korniltsev.telegram.charts.ui.MyAnimation;
import korniltsev.telegram.charts.ui.MyColor;

public final class GLScrollbarOverlayProgram {

    private static final int BYTES_PER_FLOAT = 4;
    private static final int STRIDE_BYTES = 2 * BYTES_PER_FLOAT;
    private static final int POSITION_DATA_SIZE = 2;
    //    public static final int OVERLAY_COLOR = 0xbff1f5f7;
    public static final int BORDER_COLOR = 0x334b87b4;


    private final int vbo;
    private final FloatBuffer buf1;
    private final SimpleShader shader;
    private final int[] vbos;
    private final TextTex icLeft;
    private final TextTex icRight;
    private final TexShader texShader;
    private int color_overlay;
    private int color_border;

    private float[] MVP = new float[16];
    private float[] VIEW = new float[16];

    public final int canvasW;
    public final int canvasH;

    private final Dimen dimen;


    final ChartViewGL root;

    public ZoomState zoom = new ZoomState();
    public ZoomState zoomStash = new ZoomState();
//    public float left = 0.5f;
//    public float right = 1.0f;
//    public float scale;

    float vertices[] = {
            0, 0,
            0, 1,
            1, 1,

            1, 1,
            1, 0,
            0, 0,
    };
    private MyAnimation.Color borderAnim;
    private MyAnimation.Color overlayAnim;
    private boolean released;


    public GLScrollbarOverlayProgram(int canvasW, int canvasH, Dimen dimen, ChartViewGL root, int colorBorder, int coloroVerlay, SimpleShader shader) {
        this.color_overlay = coloroVerlay;
        this.color_border = colorBorder;
        this.canvasW = canvasW;
        this.canvasH = canvasH;
        this.dimen = dimen;
        this.root = root;
        this.shader = shader;

        buf1 = ByteBuffer.allocateDirect(vertices.length * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        buf1.put(vertices);
        buf1.position(0);


        vbos = new int[1];
        GLES20.glGenBuffers(1, vbos, 0);
        vbo = vbos[0];
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertices.length * BYTES_PER_FLOAT, buf1, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        texShader = new TexShader(true, false);
        icLeft = Tooltip.loadTex(R.drawable.ic_roller_left);
        icRight = Tooltip.loadTex(R.drawable.ic_roller_right);
    }

    float[] color_parts = new float[4];

//    float [] color_border = new float[]{ //todo try to do only once
//            MyColor.red(BORDER_COLOR) / 255f,
//            MyColor.green(BORDER_COLOR) / 255f,
//            MyColor.blue(BORDER_COLOR) / 255f,
//            MyColor.alpha(BORDER_COLOR) / 255f,
//    };

    public final boolean draw(long t, float[]proj) {
        boolean invalidate = false;
        float left = zoom.left;
        float right = zoom.right;
        if (borderAnim != null) {
            color_border = borderAnim.tick(t);
            if (borderAnim.ended) {
                borderAnim = null;
            }
        }
        if (overlayAnim != null) {
            color_overlay = overlayAnim.tick(t);
            if (overlayAnim.ended) {
                overlayAnim = null;
            }
        }
        if (zoom.leftAnim != null) {
            zoom.left = zoom.leftAnim.tick(t);
            if (zoom.leftAnim.ended) {
                zoom.leftAnim = null;
            } else {
                invalidate = true;
            }
        }
        if (zoom.rightAnim != null) {
            zoom.right = zoom.rightAnim.tick(t);
            if (zoom.rightAnim.ended) {
                zoom.rightAnim = null;
            } else {
                invalidate = true;
            }
        }
        GLES20.glUseProgram(shader.program);
        MyGL.checkGlError2();

        GLES20.glEnableVertexAttribArray(shader.positionHandle);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
        GLES20.glVertexAttribPointer(shader.positionHandle, POSITION_DATA_SIZE, GLES20.GL_FLOAT, false, STRIDE_BYTES, 0);


        final float hpadding = dimen.dpf(16);
        final float scrollerW = this.canvasW - 2 * hpadding;
        final float vline1w = dimen.dpf(4f);
        final float vline2h = dimen.dpf(1f);


        MyColor.set(color_parts, color_overlay);
        GLES20.glUniform4fv(shader.colorHandle, 1, color_parts, 0);
        int y = root.dimen_v_padding8 + root.checkboxesHeight;
        drawRect(hpadding , y, left * scrollerW + icLeft.w, root.dimen_scrollbar_height);
        drawRect(hpadding + scrollerW * right - icLeft.w, y, icLeft.w + scrollerW * (1.0f - right), root.dimen_scrollbar_height);

        MyColor.set(color_parts, color_border);
        GLES20.glUniform4fv(shader.colorHandle, 1, color_parts, 0);
//        drawText(ic);
        drawRect(hpadding + scrollerW * left, y, icLeft.w, root.dimen_scrollbar_height);
        drawRect(hpadding + scrollerW * right - icLeft.w, y, icLeft.w, root.dimen_scrollbar_height);
        float l = hpadding + scrollerW * left + icLeft.w;
        float r = hpadding + scrollerW * right - icLeft.w;
        drawRect(l, y, r - l, vline2h);
        drawRect(l, y + root.dimen_scrollbar_height - vline2h, r - l, vline2h);

//        MyGL.checkGlError2();
        drawText(icLeft, l - icLeft.w, y, Color.RED, 1f, proj);
        drawText(icLeft, r, y, Color.RED, 1f, proj);
//        MyGL.checkGlError2();
        return invalidate;
    }


    private void drawText(TextTex text, float x, float y, int color, float alpha, float []proj) {

        Matrix.setIdentityM(VIEW, 0);
        Matrix.translateM(VIEW, 0, x, y, 0);
        Matrix.scaleM(VIEW, 0, text.w, text.h, 1);
        Matrix.multiplyMM(MVP, 0, proj, 0, VIEW, 0);

        GLES20.glUseProgram(texShader.texProgram);
        GLES20.glEnableVertexAttribArray(texShader.texPositionHandle);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, texShader.texVerticesVBO);
        GLES20.glVertexAttribPointer(texShader.texPositionHandle, 2, GLES20.GL_FLOAT, false, 8, 0);
        GLES20.glUniform1f(texShader.texAlphaHandle, alpha);

        GLES20.glUniformMatrix4fv(texShader.texMVPHandle, 1, false, MVP, 0);

        MyColor.set(color_parts, color);
        GLES20.glUniform4fv(texShader.u_color, 1, color_parts, 0);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, text.tex[0]);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, TexShader.texVertices.length / 2);
    }


    public void drawRect(float x, float y, float w, float h) {
        final float scalex = 2.0f / canvasW;
        final float scaley = 2.0f / canvasH;
        Matrix.setIdentityM(MVP, 0);
        Matrix.translateM(MVP, 0, -1.0f, -1.0f, 0);
        Matrix.scaleM(MVP, 0, scalex, scaley, 1.0f);

        Matrix.translateM(MVP, 0, x, y, 0);
        Matrix.scaleM(MVP, 0, w, h, 1.0f);

        GLES20.glUniformMatrix4fv(shader.MVPHandle, 1, false, MVP, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertices.length / 2);

    }

    public void setLeftRight(float left, float right, float scale) {
        zoom.left = left;
        zoom.right = right;
        zoom.scale = scale;
//        this.left = left;
//        this.right = right;
//        this.scale = scale;
//        Log.d("OVERLAY", " " + left + " " + right);
    }

    public void animate(ColorSet colors, long duration) {
        borderAnim = new MyAnimation.Color(duration, color_border, colors.scrollbarBorder);
        overlayAnim = new MyAnimation.Color(duration, color_overlay, colors.scrollbarOverlay);
    }

    public void release() {
        if (released) {
            return;
        }
        released = true;
        GLES20.glDeleteBuffers(1, vbos, 0);
//        texShader = new TexShader(true, true);
//        icLeft = Tooltip.loadTex(R.drawable.ic_roller_left);
//        icRight = Tooltip.loadTex(R.drawable.ic_roller_left);
        icLeft.release();
        icRight.release();
        texShader.release();
    }
}
