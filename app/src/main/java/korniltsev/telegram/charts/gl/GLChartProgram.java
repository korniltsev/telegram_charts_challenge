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

public final class GLChartProgram {

    private static final int BYTES_PER_FLOAT = 4;

    private static final int VERTEX_STRIDE_BYTES = 3 * BYTES_PER_FLOAT;
    private static final int VERTEX_SIZE = 3;

    private static final int LINE_WIDTH_VECTOR_SIZE = 2;
    private static final int LINE_WIDTH_VECTOR_STRIDE = 2 * BYTES_PER_FLOAT;

    final String vertexShader =
            "uniform mat4 u_MVPMatrix;      \n"
            + "uniform float u_scaley;      \n"
                    + "attribute vec3 a_Position;     \n"
                    + "attribute vec2 a_normales;     \n"
                    + "void main()                    \n"
                    + "{                              \n"
                    + "   gl_Position = u_MVPMatrix * vec4(a_Position.x + a_normales.x * 0.1, a_Position.y + u_scaley * a_normales.y * 0.1 , 0.0, 1.0);   \n"
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
    private final int vboNormales;
    private final ArrayList<Vertex> vertices;
    private final FloatBuffer verticesBuffer;
    public final ColumnData column;
    private final int colorHandle;
    //    private final int vertexCount;
    private final int maxXValue;
    private final float[] normales;
    private final FloatBuffer linesBuffer;


    private float[] MVP = new float[16];

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
        public static final int SIZE_FLOATS = 3;
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
        maxXValue = column.values.length;
        normales = new float[2 * vertexCount];
//        float prevxx = Float.NaN;
//        float prevyy = Float.NaN;
        for (int i = 0, valuesLength = values.length; i < valuesLength; i++) {//do not allocate twice
            float value = (float)values[i] / column.maxValue;
//            float unitValue = (float)value / column.maxValue;
            Vertex v1 = new Vertex();//todo rewrite without allocatinos
            Vertex v2 = new Vertex();//todo rewrite without allocatinos
            v1.x = i;
            v1.y = value;
            v1.z = 1;

            v2.x = i;
            v2.y = value;
            v2.z = -1;

            vertices.add(v1);
            vertices.add(v2);



            if (i == valuesLength - 1) {
                int j = i-1;
                normales[i * 4 ]    = normales[j * 4 ]    ;
                normales[i * 4 + 1] = normales[j * 4 + 1] ;
                normales[i * 4 + 2] = normales[j * 4 + 2] ;
                normales[i * 4 + 3] =normales[j * 4 + 3] ;
                //todo
            } else {
                float nextValue = (float)values[i+1] / column.maxValue;
                float dy = nextValue - value;
                float l = (float) Math.sqrt(1f + dy * dy);
                dy /= l;
                float dx = 1f / l;

                normales[i * 4 ] = -dy;
                normales[i * 4 + 1] = dx;
                normales[i * 4 + 2] = dy;
                normales[i * 4 + 3] = -dx;
            }
        }
        verticesBuffer = ByteBuffer.allocateDirect(vertices.size()* Vertex.SIZE_FLOATS * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        for (Vertex vertex : vertices) {
            verticesBuffer.put(vertex.x);
            verticesBuffer.put(vertex.y);
            verticesBuffer.put(vertex.z);
        }
//        verticesBuffer.put(vertices);
        verticesBuffer.position(0);

        linesBuffer = ByteBuffer.allocateDirect(normales.length * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        linesBuffer.put(normales);
        linesBuffer.position(0);


        program = MyGL.createProgram(vertexShader, fragmentShader);
        MVPHandle = GLES20.glGetUniformLocation(program, "u_MVPMatrix");
        positionHandle = GLES20.glGetAttribLocation(program, "a_Position");
        normalesHandle = GLES20.glGetAttribLocation(program, "a_normales");
        colorHandle = GLES20.glGetUniformLocation(program, "u_color");
        u_scaleyHandle = GLES20.glGetUniformLocation(program, "u_scaley");


        int[] vbos = new int[2];
        GLES20.glGenBuffers(2, vbos, 0);
        vboVertices = vbos[0];
        vboNormales = vbos[1];
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboVertices);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertices.size()* Vertex.SIZE_FLOATS* BYTES_PER_FLOAT, verticesBuffer, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboNormales);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, normales.length * BYTES_PER_FLOAT, linesBuffer, GLES20.GL_STATIC_DRAW);
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
        GLES20.glVertexAttribPointer(positionHandle, VERTEX_SIZE, GLES20.GL_FLOAT, false, VERTEX_STRIDE_BYTES, 0);

        GLES20.glEnableVertexAttribArray(normalesHandle);
        MyGL.checkGlError2();
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboNormales);
        MyGL.checkGlError2();
        GLES20.glVertexAttribPointer(normalesHandle, LINE_WIDTH_VECTOR_SIZE, GLES20.GL_FLOAT, false, LINE_WIDTH_VECTOR_STRIDE, 0);
        MyGL.checkGlError2();

        float hpadding = dimen.dpf(16);
        float minx = 0;
        float maxx = maxXValue;
        float scalex = 2.0f / w;
        float scaley = 2.0f / h;


        Matrix.setIdentityM(MVP, 0);

        Matrix.translateM(MVP, 0, -1.0f, -1.0f, 0);
        Matrix.scaleM(MVP, 0, scalex, scaley, 1.0f);
        if (scrollbar) {
            //todo learn matrixes ¯\_(ツ)_/¯
            final float dip2 = dimen.dpf(2);
            Matrix.translateM(MVP, 0, hpadding, root.dimen_v_padding8 + dip2, 0);
            float w = this.w - 2 * hpadding;
            float h = root.dimen_scrollbar_height - 2 * dip2;
            float ydiff = 1 - (float)minValue/maxValue; // normalized to 1.0
            float yscale = h / (ydiff * (float) maxValue / (float)column.maxValue );
            float dy = -yscale * minValue / maxValue;
            Matrix.translateM(MVP, 0, 0, dy, 0);
            Matrix.scaleM(MVP, 0, w / ((maxx - minx)), yscale, 1.0f);
            // todo animatino problem on dataset 0
            GLES20.glUniform1f(u_scaleyHandle, 1.0f);
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
            float yscale = (float) maxValue / (float)column.maxValue;
            Matrix.scaleM(MVP, 0, w / ((maxx - minx)), h/yscale  , 1.0f);
//            GLES20.glLineWidth(dimen.dpf(2f));
            GLES20.glUniform1f(u_scaleyHandle, 0.1f);
            MyGL.checkGlError2();
        }

//        Matrix.scaleM(MVP, 0, 5.0f, 1.0f, 1.0f);
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
