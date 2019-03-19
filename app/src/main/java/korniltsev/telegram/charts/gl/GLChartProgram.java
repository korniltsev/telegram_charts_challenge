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


    /*
    attribute vec2 a_pos;
attribute vec2 a_normal;

uniform float u_linewidth;
uniform mat4 u_mv_matrix;
uniform mat4 u_p_matrix;

void main() {
 vec4 delta = vec4(a_normal * u_linewidth, 0, 0);
 vec4 pos = u_mv_matrix * vec4(a_pos, 0, 1);
 gl_Position = u_p_matrix * (pos + delta);
}
`
     */
//    final String vertexShader =
//            "uniform mat4 m;      \n"
//            +"uniform mat4 v;      \n"
//            +"uniform mat4 p;      \n"
//                    + "attribute vec3 pos;     \n"
//                    + "attribute vec2 normale;     \n"
//
//
//                    + "void main()                    \n"
//                    + "{                                \n"
//                    +"     mat4 mv = v * m;                         \n"
//                    +"     vec4 delta = vec4(32.0 * normale.xy, 0.0,1.0);                         \n"
//                    +"     vec4 pos2 = mv * vec4(pos.xy, 0.0, 1.0) ;                         \n"
//                    + "   gl_Position = p * (pos2 + delta);   \n"
//                    + "}                              \n";

    final String vertexShader =
            "uniform mat4 m;      \n"
                    +"uniform mat4 v;      \n"
                    +"uniform mat4 p;      \n"
                    + "attribute vec3 pos;     \n"
                    + "attribute vec2 normale;     \n"


                    + "void main()                    \n"
                    + "{                                \n"
                    +"     float linewidth = 4.0;                         \n"
                    +"     mat4 mv = v * m;                         \n"
                    +"     vec4 delta = vec4(linewidth * normale.x, linewidth * normale.y, 0.0,0.0);                         \n"
                    +"     vec4 pos2 = mv * vec4(pos.xy, 0.0, 1.0) ;                         \n"
                    + "   gl_Position = p * (pos2 + delta);   \n"
                    + "}                              \n";


    final String fragmentShader =
            "precision mediump float;       \n"
                    + "uniform vec4 u_color;       \n"
                    + "void main()                    \n"
                    + "{                              \n"
                    + "   gl_FragColor = u_color;     \n"
//                    + "   gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0);     \n"
                    + "}                              \n";
    private final int mHandle;
    private final int positionHandle;
//    private final int normalesHandle;
//    private final int u_scaleyHandle;
    private final int program;
    private final int vboVertices;
    //    private final int vboNormales;
    private final ArrayList<Vertex> vertices;
    private final FloatBuffer verticesBuffer;
    public final ColumnData column;
    private final int colorHandle;
    //    private final int vertexCount;
    private final int maxXValue;
    private final int vHandle;
    private final int pHandle;
    private final int normaleHandle;
//    private final int u_scalexHandle;
//    private final float[] normales;
//    private final FloatBuffer linesBuffer;


    private float[] MVP = new float[16];
    private float[] VIEW = new float[16];
    private float[] PROJ = new float[16];
    private float[] MODEL = new float[16];
//    private float[] TN = new float[16];


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
            float unitValue = (float)value / column.maxValue;
            Vertex v1 = new Vertex();//todo rewrite without allocatinos
            Vertex v2 = new Vertex();//todo rewrite without allocatinos
            float x = i;
            float y = value;
            if (vertices.size() > 0) {
                Vertex t1 = new Vertex();
                Vertex t2 = new Vertex();
                t1.x = x;
                t1.y = y;
                t1.z = 1;
                t1.normale_x = vertices.get(vertices.size() - 2).normale_x;
                t1.normale_y = vertices.get(vertices.size() - 2).normale_y;
                t2.x = x;
                t2.y = y;
                t2.z = -1;
                t2.normale_x = vertices.get(vertices.size() - 1).normale_x;
                t2.normale_y = vertices.get(vertices.size() - 1).normale_y;
                vertices.add(t1);
                vertices.add(t2);
            }
            v1.x = x ;
            v1.y = y ;
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
        mHandle = GLES20.glGetUniformLocation(program, "m");
        vHandle = GLES20.glGetUniformLocation(program, "v");
        pHandle = GLES20.glGetUniformLocation(program, "p");
        positionHandle = GLES20.glGetAttribLocation(program, "pos");
        normaleHandle = GLES20.glGetAttribLocation(program, "normale");
        colorHandle = GLES20.glGetUniformLocation(program, "u_color");
//        u_scaleyHandle = GLES20.glGetUniformLocation(program, "u_scaley");
//        u_scalexHandle = GLES20.glGetUniformLocation(program, "u_scalex");


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

        GLES20.glEnableVertexAttribArray(normaleHandle);
        MyGL.checkGlError2();
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboVertices);//todo remove
        MyGL.checkGlError2();
        GLES20.glVertexAttribPointer(normaleHandle, 2, GLES20.GL_FLOAT, false, Vertex.STRIDE, 3 * BYTES_PER_FLOAT);
        MyGL.checkGlError2();


        if (scrollbar) {
            return;
        }
        float myhpx = h - dimen.dpf(8) - root.dimen_scrollbar_height - dimen.dpf(16) - dimen.dpf(8);
        float mywpx = w - dimen.dpf(16 * 2);
        float dxpx = dimen.dpf(16);
        float dypx = dimen.dpf(8) + root.dimen_scrollbar_height + dimen.dpf(16);


        Matrix.orthoM(PROJ, 0, 0, w, 0, h, -1.0f, 1.0f);

        Matrix.setIdentityM(MODEL, 0);
//        Matrix.scaleM(MODEL, 0, mywpx/(float)maxXValue, myhpx , 1.0f);

        Matrix.setIdentityM(VIEW, 0);
        Matrix.translateM(VIEW, 0, dxpx, dypx ,0);
        Matrix.scaleM(VIEW, 0, mywpx/(float)maxXValue, myhpx , 1.0f);




        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, vertices.size());


        GLES20.glUniformMatrix4fv(mHandle, 1, false, MODEL, 0);
        GLES20.glUniformMatrix4fv(vHandle, 1, false, VIEW, 0);
        GLES20.glUniformMatrix4fv(pHandle, 1, false, PROJ, 0);
//        float r = mywpx/myhpx;
//        float v1[] = new float[]{
//                1, 1, 0, 1,
//        };
//        float tmp[] = new float[16];
//        Matrix.setIdentityM(tmp, 0);
//        Matrix.multiplyMM(tmp, 0, VIEW, 0, MODEL, 0);
//        Matrix.multiplyMM(tmp, 0, PROJ, 0, tmp, 0);
//        float v2[] = new float[4];
//        Matrix.multiplyMV(v2, 0, tmp, 0, v1, 0);

//        GLES20.glLineWidth(34f);

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
