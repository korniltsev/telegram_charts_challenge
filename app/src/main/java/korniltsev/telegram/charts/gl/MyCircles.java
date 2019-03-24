package korniltsev.telegram.charts.gl;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Locale;

import korniltsev.telegram.charts.MainActivity;

public class MyCircles {
//    private final float[] MVP = new float[16];
    //    private final float[] V = new float[16];
//    final int x;
//    final int y;
    private final ArrayList<Vertex> vs;

    private final float[] angles;
    private final int triangle_count;
    //    private final float[] vertices;
//    private int color;


    public final Shader shader;

    int[] vbos = new int[1];
    private int vbo;

    private final int canvasw;
    private final int canvash;

    private final int count;

    public static class Shader {
        final int triangleCount;
        private final int program;
        private final int MVPHandle;
        private final int positionHandle;
        private final int colorHandle;
        private final int attributeNoHandle;
        private final int anglesHandle;
        private final int u_radiusHandle;
        private final int u_radtioHandle;

        final String vertexShader = "\n" +
                "const int triangle_count = %d;\n" +
                "uniform vec2 u_angles[triangle_count];\n" +
                "uniform mat4 u_MVPMatrix;\n" +
                "uniform float u_radius;\n" +
                "uniform float u_ratio;\n" +
                "attribute vec2 a_Position;\n" +
                "attribute float a_no;\n" +
                "void main()\n" +
                "{\n" +
                "   highp int index = int(a_no);\n" +
                "   vec4 delta;\n" +
                "   if (index == -1) {\n" +
                "       delta = vec4(0.0, 0.0, 0.0, 0.0);\n" +
                "   } else {\n" +
                "       vec2 a = u_angles[index];\n" +
                "       delta = vec4(a.x, a.y*u_ratio, 0.0, 0.0);\n" +
                "   }\n" +
                "   float fixmeplease = 1.0;\n" +
                "   gl_Position =  u_radius * delta + u_MVPMatrix * vec4(a_Position.xy, 0.0, 1.0) ;\n" +
                "}\n";

        final String fragmentShader =
                "precision mediump float;       \n"
                        + "uniform vec4 u_color;       \n"
                        + "void main()                    \n"
                        + "{                              \n"
                        + "   gl_FragColor = u_color;     \n"
                        + "}                              \n";

        public Shader(int triangleCount) {

            this.triangleCount = triangleCount;
            String vertexShaderFormatted = String.format(Locale.US, vertexShader, triangleCount);
            program = MyGL.createProgram(vertexShaderFormatted, fragmentShader);
            MVPHandle = GLES20.glGetUniformLocation(program, "u_MVPMatrix");
//        VHandle = GLES20.glGetUniformLocation(program, "u_VMatrix");
            positionHandle = GLES20.glGetAttribLocation(program, "a_Position");
            colorHandle = GLES20.glGetUniformLocation(program, "u_color");
            anglesHandle = GLES20.glGetUniformLocation(program, "u_angles");
            u_radiusHandle = GLES20.glGetUniformLocation(program, "u_radius");
            u_radtioHandle = GLES20.glGetUniformLocation(program, "u_ratio");
            attributeNoHandle = GLES20.glGetAttribLocation(program, "a_no");

        }

        public void use() {
            GLES20.glUseProgram(program);
        }

        public void release() {
            GLES20.glDeleteProgram(program);

        }
    }


    public MyCircles(int canvasw, int canvash, int xOffset, long[] yValues, int trianglesCount, Shader shader) {
        this.canvasw = canvasw;
        this.canvash = canvash;
        this.count = yValues.length;

        triangle_count = trianglesCount;

        this.shader = shader;
//        if (anglesHandle == -1 || attributeNoHandle == -1) {
//            throw new AssertionError();
//        }

        float[] V = new float[16];

        float[] one = {1f, 0f, 0f, 0f};


        angles = new float[triangle_count * 2];//uniform matrixes
        float[] tmpres = new float[4];
        for (int i = 0; i < triangle_count; i++) {
            float angle = 360f / triangle_count;
            Matrix.setIdentityM(V, 0);
            Matrix.rotateM(V, 0, angle * i, 0, 0, 1.0f);
            Matrix.multiplyMV(tmpres, 0, V, 0, one, 0);
//            double l = Matrix.length(tmpres[0], tmpres[1], 0f);
//            double x = tmpres[0];
//            x = x / l;
//            double y = tmpres[1];
//            y = y / l;
            angles[i * 2] = tmpres[0];
            angles[i * 2 + 1] = tmpres[1];
        }
        vs = new ArrayList<Vertex>();
        for (int x = 0, valuesLength = yValues.length; x < valuesLength; x++) {
            long y = yValues[x];
            for (int i = 0; i < triangle_count; i++) {
//                boolean b = xx / 100 % 2 == 1;
                float xx = x + xOffset;
                vs.add(new Vertex(xx, y, -1));
                vs.add(new Vertex(xx, y, i));
                int no = (i + 1) % triangle_count;
                vs.add(new Vertex(xx, y, no));
            }
        }
        ByteBuffer b = ByteBuffer.allocateDirect(vs.size() * 3 * 4)
                .order(ByteOrder.nativeOrder());
        FloatBuffer buf1 = b.asFloatBuffer();
        for (Vertex v : vs) {
            b.putFloat(v.x);
            b.putFloat(v.y);
            b.putFloat(v.no);
        }
        buf1.position(0);


        GLES20.glGenBuffers(1, vbos, 0);
        vbo = vbos[0];
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vs.size() * 3 * 4, buf1, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);


    }

    static class Vertex {
        final float x;
        final float y;
        final int no;

        public Vertex(float x, float y, int no) {
            this.x = x;
            this.y = y;
            this.no = no;
        }
    }

    public final void draw(float[] MVP, float[] colors, float radiusx, float ratio) {
        draw(MVP, colors, 0, count, radiusx, ratio);
    }


    public final void draw(float[] MVP, float[] colors, int from, int to, float radiusx, float ratio) {

//        GLES20.glUseProgram(shader.program);
        GLES20.glUniform2fv(shader.anglesHandle, triangle_count, angles, 0);
        GLES20.glUniform1f(shader.u_radiusHandle, radiusx);
        GLES20.glUniform1f(shader.u_radtioHandle, 1/ratio);
        GLES20.glUniform4fv(shader.colorHandle, 1, colors, 0);

        GLES20.glEnableVertexAttribArray(shader.positionHandle);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
        GLES20.glVertexAttribPointer(shader.positionHandle, 2, GLES20.GL_FLOAT, false, 12, 0);

        GLES20.glEnableVertexAttribArray(shader.attributeNoHandle);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
        GLES20.glVertexAttribPointer(shader.attributeNoHandle, 1, GLES20.GL_FLOAT, false, 12, 8);

        GLES20.glUniformMatrix4fv(shader.MVPHandle, 1, false, MVP, 0);
        //todo relace with draw elements
        int n = to - from;
        int ifrom = from * triangle_count * 3;
        int size = n * triangle_count * 3;
        try {
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, ifrom, size );
        } catch (Exception e) {
            if (MainActivity.LOGGING) Log.e(MainActivity.TAG, "circles ", e);
        }


    }

    public void release(){
        try {
            GLES20.glDeleteBuffers(1, vbos, 0);
        } catch (Throwable e) {
            if (MainActivity.LOGGING) Log.e(MainActivity.TAG, "err", e);
        }
    }
}
