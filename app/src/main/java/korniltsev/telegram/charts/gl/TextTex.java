package korniltsev.telegram.charts.gl;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;

import korniltsev.telegram.charts.MainActivity;

public class TextTex {
    final String text;
    final int[] tex = new int[1];
    final int w;
    final int h;

    int color;
    private final TextPaint p;

    TextTex(String text, TextPaint p) {
        this.text = text;
        this.p = p;
        w = (int) Math.ceil(p.measureText(text));
        StaticLayout staticLayout = new StaticLayout(text, 0, text.length(), p, w, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        h = staticLayout.getHeight();
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        staticLayout.draw(c);


//            int[] textures = new int[1];
        GLES20.glGenTextures(1, tex, 0);
//            tex = textures[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex[0]);

        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

//        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
//        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);

//        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
//        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
//
////Different possible texture parameters, e.g. GL10.GL_CLAMP_TO_EDGE
//        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_REPEAT);
//        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_REPEAT);

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, b, 0);
        b.recycle();
    }

    public final void release() {
        try {
            GLES20.glDeleteTextures(1, tex, 0);
        } catch (Throwable e) {
            if (MainActivity.LOGGING) Log.e(MainActivity.TAG, "err", e);
        }
    }
}
