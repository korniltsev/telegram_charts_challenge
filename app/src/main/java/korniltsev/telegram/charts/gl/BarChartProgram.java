package korniltsev.telegram.charts.gl;

import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import korniltsev.telegram.charts.data.ColumnData;
import korniltsev.telegram.charts.ui.Dimen;
import korniltsev.telegram.charts.ui.MyAnimation;
import korniltsev.telegram.charts.ui.MyColor;

import static korniltsev.telegram.charts.gl.LinesChartProgram.BYTES_PER_FLOAT;

public class BarChartProgram {
    public final ColumnData column;
    private final int w;
    private final int h;
    private final Dimen dimen;
    private final ChartViewGL root;
    private final boolean scrollbar;
    private final float[] vertices;
    private final int[] vbos;
    private final int vbo;

    private final MyShader shader;
    public float zoom;
    public float left;
    float max;
    private MyAnimation.Float maxAnim;
    private int tooltipIndex = -1;

    float[] tmpvec = new float[4];
    float[] tmpvec2 = new float[4];
    private float aniamteOutPivot;
    public MyAnimation.Float animateOutValueAnim;
    public float animateOutValue = -1f;
    public boolean zoomedIn;

    public BarChartProgram(ColumnData column, int w, int h, Dimen dimen, ChartViewGL root, boolean scrollbar, MyShader shader) {
        this.column = column;
        this.w = w;
        this.h = h;
        this.dimen = dimen;
        this.root = root;
        this.scrollbar = scrollbar;
        this.shader = shader;


        long[] values = column.values;

        int n = values.length;
//        int n = values.length/20;
        vertices = new float[n * 18];
        for (int i = 0, valuesLength = n; i < valuesLength; i++) {
            long value = values[i];
            vertices[18 * i ] = i;
            vertices[18 * i + 1] = 0;
            vertices[18 * i + 2] = i;

            vertices[18 * i + 3] = i;
            vertices[18 * i + 4] = value;
            vertices[18 * i + 5] = i;

            vertices[18 * i + 6] = i + 1;
            vertices[18 * i + 7] = 0;
            vertices[18 * i + 8] = i;

            vertices[18 * i + 9] = i + 1;
            vertices[18 * i + 10] = 0;
            vertices[18 * i + 11] = i;

            vertices[18 * i + 12] = i;
            vertices[18 * i + 13] = value;
            vertices[18 * i + 14] = i;

            vertices[18 * i + 15] = i + 1;
            vertices[18 * i + 16] = value;
            vertices[18 * i + 17] = i;
        }
        FloatBuffer buf1 = ByteBuffer.allocateDirect(vertices.length * BYTES_PER_FLOAT)
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
    }

    float[] colors = new float[4];
    float[] MVP = new float[16];
    float[] V = new float[16];

    public boolean animate(long t) {
        boolean invalidate = false;
        if (maxAnim != null) {
            max = maxAnim.tick(t);
            if (maxAnim.ended) {
                maxAnim = null;
            } else {
                invalidate = true;
            }
        }
        if (animateOutValueAnim != null) {
            animateOutValue = animateOutValueAnim.tick(t);
            if (animateOutValueAnim.ended) {
                animateOutValueAnim = null;
                if (!zoomedIn) {
                    animateOutValue = -1f;
                }
            } else {
                invalidate = true;
            }
        }
        return invalidate;
    }

    public void prepare(float[] PROJ) {
        float hpadding = dimen.dpf(16);
        float maxx = vertices[vertices.length - 3];


        Matrix.setIdentityM(V, 0);
        if (scrollbar) {
            final long max = column.max;
//            hpadding += dimen.dpf(1);
//            final float dip2 = dimen.dpf(2);

            final float w = this.w - 2 * hpadding;
            final float h = root.dimen_scrollbar_height;

            final float yscale = h / (max - 0);
            final float dy = -yscale * 0;
            if (animateOutValue != -1f && animateOutValue != 0f) {
                Matrix.translateM(V, 0, aniamteOutPivot, 0, 0);
                Matrix.scaleM(V, 0, 8 * animateOutValue + 1f, 1f, 1f);
                Matrix.translateM(V, 0, -aniamteOutPivot, 0, 0);
            }
            Matrix.translateM(V, 0, hpadding, root.dimen_v_padding8 + root.checkboxesHeight, 0);
            Matrix.translateM(V, 0, 0, dy, 0);
            Matrix.scaleM(V, 0, w / ((maxx)), yscale, 1.0f);
            Matrix.multiplyMM(MVP, 0, PROJ, 0, V, 0);
        } else {
            final float max = this.max;


            final int ypx = dimen.dpi(80) + root.checkboxesHeight;

            final float w = this.w - 2 * hpadding;
            final float h = root.dimen_chart_usefull_height;
            final float xdiff = maxx;
            final float ws = w / xdiff / zoom;
            final float hs = h / (max - 0);
            final float dy = -hs * 0;
            if (animateOutValue != -1f && animateOutValue != 0f) {
                Matrix.translateM(V, 0, aniamteOutPivot, 0, 0);
                Matrix.scaleM(V, 0, 8 * animateOutValue + 1f, 1f, 1f);
                Matrix.translateM(V, 0, -aniamteOutPivot, 0, 0);
            }
            Matrix.translateM(V, 0, hpadding, ypx, 0);
            Matrix.translateM(V, 0, 0, dy, 0);
            Matrix.scaleM(V, 0, ws, hs, 1.0f);
            Matrix.translateM(V, 0, -left * xdiff, 0f, 0f);

            Matrix.multiplyMM(MVP, 0, PROJ, 0, V, 0);
        }

    }

    public void draw(long t) {

        shader.use();


        if (animateOutValue != -1 && animateOutValue != 0f) {
            float colorAlpha = (column.color >>> 24) / 255f;
            float animOutAlpha = 1f - animateOutValue;
            colors[0] = ((column.color >> 16) & 0xFF) / 255f;
            colors[1] = ((column.color >> 8) & 0xFF) / 255f;
            colors[2] = (column.color & 0xFF) / 255f;
            colors[3] = colorAlpha * animOutAlpha;
        } else {
            MyColor.set(colors, column.color);
        }
        GLES20.glUniform4fv(shader.colorHandle, 1, colors, 0);
        GLES20.glEnableVertexAttribArray(shader.positionHandle);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
        GLES20.glVertexAttribPointer(shader.positionHandle, 3, GLES20.GL_FLOAT, false, 12, 0);
        GLES20.glUniform1f(shader.u_selected_index, tooltipIndex);

        GLES20.glUniformMatrix4fv(shader.MVPHandle, 1, false, MVP, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertices.length / 3);
        MyGL.checkGlError2();
    }

    public void animateMinMax(long viewportMax, boolean animate, int druation) {
        if (animate) {
            maxAnim = new MyAnimation.Float(druation, max, viewportMax);
        } else {
            max = viewportMax;
            maxAnim = null;
        }

    }

    public void setTooltipIndex(int finali) {
        tooltipIndex = finali;
    }

    public int getTooltipIndex() {
        return tooltipIndex;
    }

    public void animateOut(int duration, boolean zoomedIn, float leftX) {
        this.zoomedIn = zoomedIn;
        if (zoomedIn) {
            this.aniamteOutPivot = leftX;
        }
        if (animateOutValue == -1f) {
            animateOutValue = 0f;
        }
        animateOutValueAnim = new MyAnimation.Float(duration, animateOutValue, zoomedIn ? 1f : 0f);
    }
    public float getTooltipX(float[] PROJ) {
        prepare(PROJ);
        tmpvec[0] = tooltipIndex;
        tmpvec[1] = 1f;
        tmpvec[2] = 0f;
        tmpvec[3] = 1;
        Matrix.multiplyMV(tmpvec2, 0, V, 0, tmpvec, 0);
        return tmpvec2[0];
    }

    public static final class MyShader {
        final String vertexShader =
                "uniform mat4 u_MVPMatrix;\n" +
                        "uniform float u_selected_index;\n" +
                        "attribute vec3 a_Position;\n" +
                        "uniform vec4 u_color;\n" +
                        "varying vec4 v_color;\n" +
                        "void main()\n" +
                        "{\n" +
                        "   if (u_selected_index >= 0.0) {\n" +
                        "        if (a_Position.z == u_selected_index) {\n" +
                        "            v_color = u_color;\n" +
                        "        } else {\n" +
                        "            v_color = vec4(u_color.xyz,u_color.w * 0.5);\n" +
                        "        }\n" +
                        "   } else {\n" +
                        "        v_color = u_color;\n" +
                        "   }\n" +
                        "   gl_Position = u_MVPMatrix * vec4(a_Position.xy, 0.0, 1.0);\n" +
                        "}\n";
        final String fragmentShader =
                "precision mediump float;       \n"
                        + "varying vec4 v_color;       \n"
                        + "void main()                    \n"
                        + "{                              \n"
                        + "   gl_FragColor = v_color;     \n"
                        + "}                              \n";
        public final int MVPHandle;
        public final int positionHandle;
        public final int program;
        public final int colorHandle;
        public final int u_selected_index;
        private boolean released;

        public MyShader() {

            program = MyGL.createProgram(vertexShader, fragmentShader);
            MVPHandle = GLES20.glGetUniformLocation(program, "u_MVPMatrix");
            u_selected_index = GLES20.glGetUniformLocation(program, "u_selected_index");
            positionHandle = GLES20.glGetAttribLocation(program, "a_Position");
            colorHandle = GLES20.glGetUniformLocation(program, "u_color");
        }

        public final void use() {
            GLES20.glUseProgram(program);
        }

        public void release() {
            if (released) {
                return;
            }
            released = true;
            GLES20.glDeleteProgram(program);
        }
    }

    public void release(){
        GLES20.glDeleteBuffers(1, vbos, 0);
    }
}
