package korniltsev.telegram.charts.gl;

import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import korniltsev.telegram.charts.ui.Dimen;
import korniltsev.telegram.charts.ui.MyAnimation;

public final class GLScrollbarOverlayProgram {

    private static final int BYTES_PER_FLOAT = 4;
    private static final int STRIDE_BYTES = 2 * BYTES_PER_FLOAT;
    private static final int POSITION_DATA_SIZE = 2;
//    public static final int OVERLAY_COLOR = 0xbff1f5f7;
    public static final int BORDER_COLOR = 0x334b87b4;

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
    private final int program;
    private final int vbo;
    private final FloatBuffer buf1;
    private final int colorHandle;
    private int color_overlay;
    private int color_border;

    private float[] MVP = new float[16];

    public final int canvasW;
    public final int canvasH;

    private final Dimen dimen;


    final ChartViewGL root;

    public float left = 0.5f;
    public float right = 1.0f;

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

    public GLScrollbarOverlayProgram(int canvasW, int canvasH, Dimen dimen, ChartViewGL root, int colorBorder, int coloroVerlay) {
        this.color_overlay = coloroVerlay;
        this.color_border = colorBorder;
        this.canvasW = canvasW;
        this.canvasH = canvasH;
        this.dimen = dimen;
        this.root = root;

        buf1 = ByteBuffer.allocateDirect(vertices.length * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        buf1.put(vertices);
        buf1.position(0);


        program = MyGL.createProgram(vertexShader, fragmentShader);
        MVPHandle = GLES20.glGetUniformLocation(program, "u_MVPMatrix");
        positionHandle = GLES20.glGetAttribLocation(program, "a_Position");
        colorHandle = GLES20.glGetUniformLocation(program, "u_color");


        int[] vbos = new int[1];
        GLES20.glGenBuffers(1, vbos, 0);
        vbo = vbos[0];
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertices.length * BYTES_PER_FLOAT, buf1, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

    }

    float[] color_parts = new float[4];

//    float [] color_border = new float[]{ //todo try to do only once
//            Color.red(BORDER_COLOR) / 255f,
//            Color.green(BORDER_COLOR) / 255f,
//            Color.blue(BORDER_COLOR) / 255f,
//            Color.alpha(BORDER_COLOR) / 255f,
//    };

    public final void draw(long t) {
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
        GLES20.glUseProgram(program);
        MyGL.checkGlError2();

        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
        GLES20.glVertexAttribPointer(positionHandle, POSITION_DATA_SIZE, GLES20.GL_FLOAT, false, STRIDE_BYTES, 0);


        final float hpadding = dimen.dpf(16);
        final float scrollerW = this.canvasW - 2 * hpadding;
        final float vline1w = dimen.dpf(4f);
        final float vline2h = dimen.dpf(1f);


        color_parts[0] = Color.red(color_overlay) / 255f;
        color_parts[1] = Color.green(color_overlay) / 255f;
        color_parts[2] = Color.blue(color_overlay) / 255f;
        color_parts[3] = Color.alpha(color_overlay) / 255f;
        GLES20.glUniform4fv(colorHandle, 1, color_parts, 0);
        if (left != 0.0f) {
            drawRect(hpadding, root.dimen_v_padding8, left*scrollerW, root.dimen_scrollbar_height);
        }
        if (right != 1.0f) {
            drawRect(hpadding + scrollerW * right, root.dimen_v_padding8, scrollerW * (1.0f-right), root.dimen_scrollbar_height);
        }

        color_parts[0] = Color.red(color_border) / 255f;
        color_parts[1] = Color.green(color_border) / 255f;
        color_parts[2] = Color.blue(color_border) / 255f;
        color_parts[3] = Color.alpha(color_border) / 255f;

        GLES20.glUniform4fv(colorHandle, 1, color_parts, 0);
        drawRect(hpadding + scrollerW * left, root.dimen_v_padding8, vline1w, root.dimen_scrollbar_height);
        drawRect(hpadding + scrollerW * right - vline1w, root.dimen_v_padding8, vline1w, root.dimen_scrollbar_height);
        float l = hpadding + scrollerW * left + vline1w;
        float r = hpadding + scrollerW * right - vline1w;
        drawRect(l, root.dimen_v_padding8, r-l, vline2h);
        drawRect(l, root.dimen_v_padding8 + root.dimen_scrollbar_height - vline2h, r-l, vline2h);
    }



    public void drawRect(float x, float y, float w, float h) {
        final float scalex = 2.0f / canvasW;
        final float scaley = 2.0f / canvasH;
        Matrix.setIdentityM(MVP, 0);
        Matrix.translateM(MVP, 0, -1.0f, -1.0f, 0);
        Matrix.scaleM(MVP, 0, scalex, scaley, 1.0f);

        Matrix.translateM(MVP, 0, x, y, 0);
        Matrix.scaleM(MVP, 0, w, h, 1.0f);

        GLES20.glUniformMatrix4fv(MVPHandle, 1, false, MVP, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertices.length / 2);

    }

    public void setLeftRight(float left, float right) {
        this.left = left;
        this.right = right;
//        Log.d("OVERLAY", " " + left + " " + right);
    }

    public void animate(int scrollbarBorder, int scrollbarOverlay) {
        borderAnim = new MyAnimation.Color(MyAnimation.ANIM_DRATION, color_border, scrollbarBorder);
        overlayAnim = new MyAnimation.Color(MyAnimation.ANIM_DRATION, color_overlay, scrollbarOverlay);
    }
}
