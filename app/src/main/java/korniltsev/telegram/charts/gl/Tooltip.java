package korniltsev.telegram.charts.gl;

import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import korniltsev.telegram.charts.ui.ColorSet;
import korniltsev.telegram.charts.ui.Dimen;
import korniltsev.telegram.charts.ui.MyAnimation;
import korniltsev.telegram.charts.ui.MyColor;

// vertical line & label with values
public class Tooltip {
    private final float[] colorParts= new float[4];
    public final Shader shader;
    final Dimen dimen;
    private final int vbo;

    static final float lineVertices[] = {
            0, 0,
            0, 1,
    };
    private final int lineVerticesVBO;
    private final int w;
    private int lineColor;
    private MyAnimation.Color lineANim;

    public Tooltip(Shader shader, Dimen dimen, int w, ColorSet colors) {
        this.shader = shader;
        this.dimen = dimen;
        this.w = w;
        this.lineColor = colors.tooltipVerticalLine;


        FloatBuffer buf1 = ByteBuffer.allocateDirect(lineVertices.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        buf1.put(lineVertices);
        buf1.position(0);

        int[] vbos = new int[1];
        GLES20.glGenBuffers(1, vbos, 0);
        vbo = vbos[0];
        lineVerticesVBO = vbos[0];
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, lineVerticesVBO);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, lineVertices.length * 4, buf1, GLES20.GL_STATIC_DRAW);
    }

    public void animateTo(ColorSet colors) {
        lineANim = new MyAnimation.Color(MyAnimation.ANIM_DRATION, lineColor, colors.tooltipVerticalLine);
    }

    public static class Shader {

        static final String vertexShader =
                "uniform mat4 u_MVPMatrix;      \n"
                        + "attribute vec2 a_Position;     \n"
                        + "void main()                    \n"
                        + "{                              \n"
                        + "   gl_Position = u_MVPMatrix * vec4(a_Position.xy, 0.0, 1.0);   \n"
                        + "}                              \n";

        static final String fragmentShader =
                "precision mediump float;       \n"
                        + "uniform vec4 u_color;       \n"
                        + "void main()                    \n"
                        + "{                              \n"
                        + "   gl_FragColor = u_color;     \n"
                        + "}                              \n";

        private final int lineProgram;
        private final int lineMVPHandle;
        private final int linePositionHandle;
        private final int lineColorHandle;


        public Shader() {
            lineProgram = MyGL.createProgram(vertexShader, fragmentShader);
            lineMVPHandle = GLES20.glGetUniformLocation(lineProgram, "u_MVPMatrix");
            linePositionHandle = GLES20.glGetAttribLocation(lineProgram, "a_Position");
            lineColorHandle = GLES20.glGetUniformLocation(lineProgram, "u_color");
        }

    }



    final float vec1[] = new float[4];
    final float vec2[] = new float[4];
    final float []VIEW = new float[16];
    final float []MVP = new float[16];
    public boolean animationTick(long time) {
        boolean invalidate = false;
        if (lineANim != null) {
            lineColor = lineANim.tick(time);
            if (lineANim.ended) {
                lineANim = null;
            } else {
                invalidate = true;
            }
        }
        return invalidate;
    }
    public void draw(float[]proj, float [] chartMVP, float x) {


        vec1[0] = x;
        vec1[3] = 1;


        Matrix.multiplyMV(vec2, 0, chartMVP, 0 , vec1, 0);



        float ndcx = (vec2[0] + 1f )/2f;

        Log.d("FUCK", "v" + ndcx);
        Matrix.setIdentityM(VIEW, 0);
        Matrix.translateM(VIEW, 0, 0, dimen.dpf(80f), 0f);
        Matrix.scaleM(VIEW, 0, w, dimen.dpf(290), 1f);
        Matrix.translateM(VIEW, 0, ndcx, 0f, 0f);

        Matrix.multiplyMM(MVP, 0, proj, 0, VIEW, 0);
//        Matrix.translateM(MVP, 0, ndcx, 0f, 0f);


        GLES20.glUseProgram(shader.lineProgram);


        MyColor.set(colorParts, lineColor);
        GLES20.glUniform4fv(shader.lineColorHandle, 1, colorParts, 0);

        GLES20.glEnableVertexAttribArray(shader.linePositionHandle);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
        GLES20.glVertexAttribPointer(shader.linePositionHandle, 2, GLES20.GL_FLOAT, false, 8, 0);

        GLES20.glLineWidth(dimen.dpf(1f));
        GLES20.glUniformMatrix4fv(shader.lineMVPHandle, 1, false, MVP, 0);
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, lineVertices.length / 2);



    }
}
