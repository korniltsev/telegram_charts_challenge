package korniltsev.telegram.charts.gl;

import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import korniltsev.telegram.charts.ui.Dimen;

public class MyCircles {
//    private final float[] MVP = new float[16];
    //    private final float[] V = new float[16];
//    final int x;
//    final int y;
    private final ArrayList<Vertex> vs;
    private final int attributeNoHandle;
    //    private final int VHandle;
    private final int anglesHandle;
    private final float[] angles;
    private final int triangle_count;
    //    private final float[] vertices;
//    private int color;

    final String vertexShader = "const int triangle_count = %d;\n" +
            "uniform vec2 u_angles[triangle_count];\n" +
            "uniform mat4 u_MVPMatrix;\n" +
            "attribute vec2 a_Position;\n" +
            "attribute float a_no;\n" +
            "void main()\n" +
            "{\n" +
            "   highp int index = int(a_no);\n" +
            "   vec2 delta;\n" +
            "   if (index == -1) {\n" +
            "       delta = vec2(0.0, 0.0);\n" +
            "   } else {\n" +
            "       delta = u_angles[index];\n" +
            "   }\n" +
            "   gl_Position =  vec4(delta.xy, 0.0, 0.0) + u_MVPMatrix * vec4(a_Position.xy, 0.0, 1.0) ;\n" +
            "}\n";

    final String fragmentShader =
            "precision mediump float;       \n"
                    + "uniform vec4 u_color;       \n"
                    + "void main()                    \n"
                    + "{                              \n"
                    + "   gl_FragColor = u_color;     \n"
                    + "}                              \n";
    private final int program;
    private final int MVPHandle;
    private final int positionHandle;
    private final int colorHandle;
    private int vbo;

    private final int canvasw;
    private final int canvash;

    public MyCircles(Dimen dimen, int canvasw, int canvash, long []values, float radiusDip) {
//        this.x = x;
//        this.y = y;
//        this.color = Color.RED;
        this.canvasw = canvasw;
        this.canvash = canvash;

        triangle_count = 16;
        String vertexShaderFormatted = String.format(Locale.US, vertexShader, triangle_count);
        program = MyGL.createProgram(vertexShaderFormatted, fragmentShader);
        MVPHandle = GLES20.glGetUniformLocation(program, "u_MVPMatrix");
//        VHandle = GLES20.glGetUniformLocation(program, "u_VMatrix");
        positionHandle = GLES20.glGetAttribLocation(program, "a_Position");
        colorHandle = GLES20.glGetUniformLocation(program, "u_color");
        anglesHandle = GLES20.glGetUniformLocation(program, "u_angles");
        attributeNoHandle = GLES20.glGetAttribLocation(program, "a_no");
        if (anglesHandle == -1 || attributeNoHandle == -1) {
            throw new AssertionError();
        }

        float[] V = new float[16];
        float[] radiusVector = new float[4];
        Matrix.setIdentityM(V, 0);
        Matrix.scaleM(V, 0, 2f / canvasw, 2f / canvash, 1f);
        Matrix.multiplyMV(radiusVector, 0, V, 0, new float[]{dimen.dpf(radiusDip), 0f, 0f, 1f}, 0);

        float[] tmp = new float[16];
        angles = new float[triangle_count * 2];//uniform matrixes
        float[] tmpres = new float[4];
        for (int i = 0; i < triangle_count; i++) {
//            int o = i * 16;
            Matrix.setIdentityM(tmp, 0);
            float angle = 360f / triangle_count;
            Matrix.rotateM(tmp, 0, angle * i, 0, 0, 1.0f);
            Matrix.multiplyMV(tmpres, 0, tmp, 0, radiusVector, 0);
            angles[i * 2] = tmpres[0];
            angles[i * 2 + 1] = tmpres[1];
        }
        vs = new ArrayList<Vertex>();
        for (int x = 0, valuesLength = values.length; x < valuesLength; x++) {
            long y = values[x];
//            vertices[x * 2] = x;
//            vertices[x * 2 + 1] = y;
//        }
//        for (int xx = 0; xx < 500; xx += 100) {
            for (int i = 0; i < triangle_count; i++) {
//                boolean b = xx / 100 % 2 == 1;
                vs.add(new Vertex(x, y, -1));
                vs.add(new Vertex(x, y, i));
                int no = (i + 1) % triangle_count;
                vs.add(new Vertex(x, y, no));
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
//            buf1.put(vertices);
        buf1.position(0);

        int[] vbos = new int[1];
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

    public final void draw(float[] MVP, float[] colors) {

        GLES20.glUseProgram(program);
//        float[] colors = new float[]{
//                Color.red(color),
//                Color.green(color),
//                Color.blue(color),
//                Color.alpha(color),
//        };

//        GLES20.glUseProgram(program);
//        MyGL.checkGlError2();
        GLES20.glUniform2fv(anglesHandle, triangle_count, angles, 0);
        MyGL.checkGlError2();
        GLES20.glUniform4fv(colorHandle, 1, colors, 0);//todo try to bind only once

        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 12, 0);

        GLES20.glEnableVertexAttribArray(attributeNoHandle);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
        GLES20.glVertexAttribPointer(attributeNoHandle, 1, GLES20.GL_FLOAT, false, 12, 8);
        MyGL.checkGlError2();

//        final float scalex = 2.0f / canvasw;
//        final float scaley = 2.0f / canvash;
//        Matrix.setIdentityM(MVP, 0);
//        Matrix.translateM(MVP, 0, -1.0f, -1.0f, 0);
//        Matrix.scaleM(MVP, 0, scalex, scaley, 1.0f);
//
////        System.arraycopy(MVP, 0, V, 0, 16);
//
//        Matrix.translateM(MVP, 0, 100f, y, 0);
//        Matrix.scaleM(MVP, 0, 1f, 10, 1.0f);

//        GLES20.glLineWidth(20);
        GLES20.glUniformMatrix4fv(MVPHandle, 1, false, MVP, 0);
//        GLES20.glUniformMatrix4fv(VHandle, 1, false, V, 0);
        //todo relace with draw elements
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vs.size() );
        MyGL.checkGlError2();
//        GLES20.glDrawArrays(GLES20.GL_LINES, 0, vertices.length / 2);


    }
}
