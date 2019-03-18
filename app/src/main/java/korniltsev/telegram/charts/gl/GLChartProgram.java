package korniltsev.telegram.charts.gl;

import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import korniltsev.telegram.charts.ColumnData;
import korniltsev.telegram.charts.Dimen;

// line width uniform, proper scaling
// extra vertex in the join
// lost last point
// alpha animation blending
// create custom dataset for testing
public final class GLChartProgram {

    private static final int BYTES_PER_FLOAT = 4;


    final String vertexShader =
            "uniform mat4 u_MVPMatrix;      \n"
                    + "uniform float u_scaley;      \n"
                    + "uniform float u_scalex;      \n"
                    + "attribute vec3 a_Position;     \n"
                    + "attribute vec2 a_normales;     \n"
                    + "void main()                    \n"
                    + "{                                \n"
                    + "   vec4 delta = vec4(a_normales.x * u_scalex , a_normales.y * u_scaley , 0.0, 0.0);                         \n"
                    + "   vec4 shifted = vec4(a_Position.x,a_Position.y,0.0, 1.0) + delta;                        \n"
                    + "   vec4 t = u_MVPMatrix * shifted;                                   \n"
                    + "   t = t;                         \n"
                    + "   gl_Position = t;   \n"
                    + "}                              \n";

    final String fragmentShader =
            "precision mediump float;       \n"
                    + "uniform vec4 u_color;       \n"
                    + "void main()                    \n"
                    + "{                              \n"
                    + "   gl_FragColor = u_color;     \n"
//                    + "   gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0);     \n"
                    + "}                              \n";
    private final int MVPHandle;
    private final int positionHandle;
    private final int normalesHandle;
    private final int u_scaleyHandle;
    private final int program;
    private final int vboVertices;
    //    private final int vboNormales;
    private final ArrayList<Vertex> vertices;
    private final FloatBuffer verticesBuffer;
    public final ColumnData column;
    private final int colorHandle;
    //    private final int vertexCount;
    private final int maxXValue;
    private final int u_scalexHandle;
//    private final float[] normales;
//    private final FloatBuffer linesBuffer;


    private float[] MVP = new float[16];
    private float[] NORMALE_ROT = new float[16];

    public final int w;
    public final int h;

    private final Dimen dimen;

    public long maxValue;
    public long minValue;

    final ChartViewGL root;

    final boolean scrollbar;
    static long idCounter;
    final long id = idCounter++;

    public static class Vertex {
        public static final int SIZE_FLOATS = 5;
        public static final int STRIDE = SIZE_FLOATS * BYTES_PER_FLOAT;
        float x;
        float y;
        float z;

        float normale_x;
        float normale_y;
    }

    public GLChartProgram(ColumnData column, int w, int h, Dimen dimen, ChartViewGL root, boolean scrollbar) {

        this.w = w;
        this.h = h;
        this.column = column;
        this.dimen = dimen;
        this.root = root;
        this.scrollbar = scrollbar;

        long[] values = column.values;
        int vertexCount = values.length * 2;
        vertices = new ArrayList<>();
        maxXValue = column.values.length - 1;
//        normales = new float[2 * vertexCount];
//        float prevxx = Float.NaN;
//        float prevyy = Float.NaN;
        for (int i = 0, valuesLength = values.length; i < valuesLength; i++) {//do not allocate twice
            float value = (float) values[i] / column.maxValue;
//            float unitValue = (float)value / column.maxValue;
            Vertex v1 = new Vertex();//todo rewrite without allocatinos
            Vertex v2 = new Vertex();//todo rewrite without allocatinos
            float x = i;
            float y = value;
            if (vertices.size() > 0) {
                Vertex t1 = new Vertex();
                Vertex t2 = new Vertex();
                t1.x = x;
                t1.y = y;
                t1.normale_x = vertices.get(vertices.size() - 2).normale_x;
                t1.normale_y = vertices.get(vertices.size() - 2).normale_y;
                t2.x = x;
                t2.y = y;
                t2.normale_x = vertices.get(vertices.size() - 1).normale_x;
                t2.normale_y = vertices.get(vertices.size() - 1).normale_y;
                vertices.add(t1);
                vertices.add(t2);
            }
            v1.x = x;
            v1.y = y;
            v1.z = 1;

            v2.x = x;
            v2.y = y;
            v2.z = -1;

            vertices.add(v1);
            vertices.add(v2);


            if (i == valuesLength - 1) {
                int j = i - 1;
//                normales[i * 4 ]    = normales[j * 4 ]    ;
//                normales[i * 4 + 1] = normales[j * 4 + 1] ;
//                normales[i * 4 + 2] = normales[j * 4 + 2] ;
//                normales[i * 4 + 3] =normales[j * 4 + 3] ;
                //todo
            } else {
                double nextValue = (float) values[i + 1] / column.maxValue;
                double dy = nextValue - value;
                double l = (float) Math.sqrt(1f + dy * dy);
                dy /= l;
                double dx = 1f / l;

                v1.normale_x = (float) -dy * 10;
                v1.normale_y = (float) dx;
                v2.normale_x = (float) dy * 10;
                v2.normale_y = (float) -dx;
            }
        }
        verticesBuffer = ByteBuffer.allocateDirect(vertices.size() * Vertex.SIZE_FLOATS * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        for (Vertex vertex : vertices) {
            verticesBuffer.put(vertex.x);
            verticesBuffer.put(vertex.y);
            verticesBuffer.put(vertex.z);
            verticesBuffer.put(vertex.normale_x);
            verticesBuffer.put(vertex.normale_y);
        }
//        verticesBuffer.put(vertices);
        verticesBuffer.position(0);

//        linesBuffer = ByteBuffer.allocateDirect(normales.length * BYTES_PER_FLOAT)
//                .order(ByteOrder.nativeOrder())
//                .asFloatBuffer();
//        linesBuffer.put(normales);
//        linesBuffer.position(0);


        program = MyGL.createProgram(vertexShader, fragmentShader);
        MVPHandle = GLES20.glGetUniformLocation(program, "u_MVPMatrix");
        positionHandle = GLES20.glGetAttribLocation(program, "a_Position");
        normalesHandle = GLES20.glGetAttribLocation(program, "a_normales");
        colorHandle = GLES20.glGetUniformLocation(program, "u_color");
        u_scaleyHandle = GLES20.glGetUniformLocation(program, "u_scaley");
        u_scalexHandle = GLES20.glGetUniformLocation(program, "u_scalex");


        int[] vbos = new int[1];
        GLES20.glGenBuffers(1, vbos, 0);
        vboVertices = vbos[0];
//        vboNormales = vbos[1];
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboVertices);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertices.size() * Vertex.STRIDE, verticesBuffer, GLES20.GL_STATIC_DRAW);
//        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboNormales);
//        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, normales.length * BYTES_PER_FLOAT, linesBuffer, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        MyGL.checkGlError2();

    }

    public final void draw(long t) {
        GLES20.glUseProgram(program);
        MyGL.checkGlError2();

        if (alphaAnim != null) {
            alpha = alphaAnim.tick(t);
            if (alphaAnim.ended) {
                alphaAnim = null;
            }
        }
        if (minAnim != null) {
            minValue = minAnim.tick(t);
            if (minAnim.ended) {
                minAnim = null;
            }
        }

        if (maxAnim != null) {
            maxValue = maxAnim.tick(t);
            if (maxAnim.ended) {
                maxAnim = null;
            }
        }

        //////// todo allocations!!!!!!!!!!!!!
        float[] colors = new float[]{ //todo try to do only once
                Color.red(column.color) / 255f,
                Color.green(column.color) / 255f,
                Color.blue(column.color) / 255f,
                alpha,
        };
        GLES20.glUniform4fv(colorHandle, 1, colors, 0);


        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboVertices);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, Vertex.STRIDE, 0);

        GLES20.glEnableVertexAttribArray(normalesHandle);
        MyGL.checkGlError2();
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboVertices);//todo remove
        MyGL.checkGlError2();
        GLES20.glVertexAttribPointer(normalesHandle, 2, GLES20.GL_FLOAT, false, Vertex.STRIDE, 3 * BYTES_PER_FLOAT);
        MyGL.checkGlError2();

        float hpadding = dimen.dpf(16);
        float minx = 0;
        float maxx = maxXValue;
        float scalex = 2.0f / w;
        float scaley = 2.0f / h;


        Matrix.setIdentityM(MVP, 0);

        Matrix.translateM(MVP, 0, -1.0f, -1.0f, 0);
        Matrix.scaleM(MVP, 0, scalex, scaley, 1.0f);
        final float dip2 = dimen.dpf(2);
        if (scrollbar) {
            //todo learn matrixes ¯\_(ツ)_/¯
            Matrix.translateM(MVP, 0, hpadding, root.dimen_v_padding8 + dip2, 0);
            float w = this.w - 2 * hpadding;
            float h = root.dimen_scrollbar_height - 2 * dip2;
            float ydiff = 1 - (float) minValue / maxValue; // normalized to 1.0
            float yscale = h / (ydiff * (float) maxValue / (float) column.maxValue);
            float dy = -yscale * minValue / maxValue;
            Matrix.translateM(MVP, 0, 0, dy, 0);
            Matrix.scaleM(MVP, 0, w / ((maxx - minx)), yscale, 1.0f);
            // todo animatino problem on dataset 0
            GLES20.glUniform1f(u_scaleyHandle, 0.01f);
            GLES20.glUniform1f(u_scalexHandle, 0.01f);
            MyGL.checkGlError2();
//            GLES20.glLineWidth(dimen.dpf(1f));
        } else {
            int ypx = root.dimen_v_padding8
                    + root.dimen_scrollbar_height
                    + root.dimen_v_padding8
                    + root.dimen_v_padding8;
            Matrix.translateM(MVP, 0, hpadding, ypx, 0);
            float w = this.w - 2 * hpadding;
            int h = root.dimen_chart_height;
            float yscale = (float) maxValue / (float) column.maxValue;
            Matrix.scaleM(MVP, 0, w / ((maxx - minx)), h / yscale, 1.0f);
//            GLES20.glLineWidth(dimen.dpf(2f));
//            float yyscale = 1f;
//            GLES20.glUniform1f(u_scaleyHandle, dip2 * yscale/h);
//            GLES20.glUniform1f(u_scalexHandle, dip2 * (maxx-minx)/w);




            GLES20.glUniform1f(u_scaleyHandle, 8 * dip2 / h * yscale );
            GLES20.glUniform1f(u_scalexHandle, 0.002f);
            MyGL.checkGlError2();
        }

//        Matrix.scaleM(MVP, 0, 5.0f, 1.0f, 1.0f);
        if (scrollbar) {
            return;
        }
        GLES20.glUniformMatrix4fv(MVPHandle, 1, false, MVP, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, vertices.size());
    }

    boolean checked = true;
    MyAnimation.Float alphaAnim;
    MyAnimation.Long minAnim;
    MyAnimation.Long maxAnim;
    float alpha = 1f;

    public void animateAlpha(boolean isChecked) {
        if (this.checked != isChecked) {
            alphaAnim = new MyAnimation.Float(160, alpha, isChecked ? 1.0f : 0.0f);
            this.checked = isChecked;
        }
    }

    public void animateMinMax(long min, long max, boolean animate) {
        if (alpha == 0f || !animate) {
            minValue = min;
            maxValue = max;
        } else {
//            minValue = min;
//            maxValue = max;
            minAnim = new MyAnimation.Long(160, minValue, min);
            maxAnim = new MyAnimation.Long(160, maxValue, max);
        }

    }
}
