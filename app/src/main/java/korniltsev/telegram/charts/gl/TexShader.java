package korniltsev.telegram.charts.gl;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class TexShader {
    static final String texVertexShader =
            "uniform mat4 u_MVPMatrix;      \n"
                    + "attribute vec2 a_Position;     \n"
                    + "varying vec2 textureCoordinate;\n"
                    + "void main()                    \n"
                    + "{                              \n"
                    + "   gl_Position = u_MVPMatrix * vec4(a_Position.xy, 0.0, 1.0);   \n"
                    + "   textureCoordinate = a_Position;   \n"
                    + "}                              \n";

    static final String texFragmentShaderFlip =
            "precision mediump float;       \n"
                    + "varying vec2 textureCoordinate;\n"
                    + "uniform float alpha;\n"
                    + "uniform sampler2D frame;\n"
                    + "void main()                    \n"
                    + "{                              \n"
                    + "   vec4 c=texture2D(frame, vec2(textureCoordinate.x, 1.0-textureCoordinate.y));                               \n"
                    + "   gl_FragColor = vec4(c.xyz, c.w * alpha);     \n"
                    + "}                              \n";

    static final String texFragmentShaderFlipMasked =
            "precision mediump float;       \n"
                    + "varying vec2 textureCoordinate;\n"
                    + "uniform float alpha;\n"
                    + "uniform vec4 u_color;\n"
                    + "uniform sampler2D frame;\n"
                    + "void main()                    \n"
                    + "{                              \n"
                    + "   vec4 c=texture2D(frame, vec2(textureCoordinate.x, 1.0-textureCoordinate.y));                               \n"
                    + "   gl_FragColor = vec4(u_color.xyz, c.w * alpha);     \n"
                    + "}                              \n";

    static final String texFragmentShaderNoFlip =
            "precision mediump float;       \n"
                    + "varying vec2 textureCoordinate;\n"
                    + "uniform float alpha;\n"
                    + "uniform sampler2D frame;\n"
                    + "void main()                    \n"
                    + "{                              \n"
                    + "   vec4 c=texture2D(frame, vec2(textureCoordinate.x, textureCoordinate.y));                               \n"
                    + "   gl_FragColor = vec4(c.xyz, c.w * alpha);     \n"
                    + "}                              \n";

    static final float texVertices[] = {
            0, 0,
            0, 1,
            1, 0,
            1, 1,
    };

    public final int texProgram;
    public final int texMVPHandle;
    public final int texPositionHandle;
    public final int texAlphaHandle;
    public final int texVerticesVBO;

    public final boolean flip;
    private final boolean masked;
    public final int u_color;

    public TexShader(boolean flip, boolean masked) {
        this.flip = flip;
        this.masked = masked;
        if (flip) {
            if (masked) {
                texProgram = MyGL.createProgram(texVertexShader, texFragmentShaderFlipMasked);
            } else {
                texProgram = MyGL.createProgram(texVertexShader, texFragmentShaderFlip);
            }
        } else {
            texProgram = MyGL.createProgram(texVertexShader, texFragmentShaderNoFlip);
        }
        texMVPHandle = GLES20.glGetUniformLocation(texProgram, "u_MVPMatrix");
        texAlphaHandle = GLES20.glGetUniformLocation(texProgram, "alpha");
        texPositionHandle = GLES20.glGetAttribLocation(texProgram, "a_Position");
        u_color = GLES20.glGetUniformLocation(texProgram, "u_color");

        FloatBuffer buf2 = ByteBuffer.allocateDirect(TexShader.texVertices.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        buf2.put(texVertices);
        buf2.position(0);

        int[] vbos = new int[1];
        GLES20.glGenBuffers(1, vbos, 0);
//        lineVerticesVBO = vbos[0];
        texVerticesVBO = vbos[0];
//        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, lineVerticesVBO);
//        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, lineVertices.length * BYTES_PER_FLOAT, buf1, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, texVerticesVBO);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, texVertices.length * 4, buf2, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

    }

    public void release() {
        //todo
    }
}
