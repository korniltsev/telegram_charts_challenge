package korniltsev.telegram.charts.gl;

import android.opengl.GLES20;
import android.util.Log;

import static android.opengl.GLES10.GL_NO_ERROR;
import static android.opengl.GLES10.glGetError;
import static korniltsev.telegram.charts.gl.ChartViewGL.LOG_TAG;

public class MyGL {
    public static int createProgram(String vertexShaderSource, String fragmentShadersource) {
        int[] success = new int[1];

        int vertexShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        GLES20.glShaderSource(vertexShader, vertexShaderSource);
        GLES20.glCompileShader(vertexShader);
        GLES20.glGetShaderiv(vertexShader, GLES20.GL_COMPILE_STATUS, success, 0);
        if (success[0] == 0) {
            String log = GLES20.glGetShaderInfoLog(vertexShader);
            throw new RuntimeException(log);
        }


        int fragmentShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        GLES20.glShaderSource(fragmentShader, fragmentShadersource);
        GLES20.glCompileShader(fragmentShader);
        GLES20.glGetShaderiv(fragmentShader, GLES20.GL_COMPILE_STATUS, success, 0);
        if (success[0] == 0) {
            String log = GLES20.glGetShaderInfoLog(fragmentShader);
            throw new RuntimeException(log);
        }


        int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);

        GLES20.glLinkProgram(program);
        checkGlError2();

        GLES20.glDeleteShader(vertexShader);
        GLES20.glDeleteShader(fragmentShader);
        return program;
    }

    public static void checkGlError2() {
        int error = glGetError();
        if (error != GL_NO_ERROR) {
            String msg = "GL error = 0x" +
                    Integer.toHexString(error);
            Log.w(LOG_TAG, msg);
            throw new AssertionError(msg);
        }
    }
}
