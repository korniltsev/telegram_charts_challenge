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
    final int[] tex = new int[1];
    final int w;
    final int h;

    int color;
    private boolean released;

    TextTex(String text, TextPaint p) {
        w = (int) Math.ceil(p.measureText(text));
        StaticLayout staticLayout = new StaticLayout(text, 0, text.length(), p, w, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        h = staticLayout.getHeight();
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        staticLayout.draw(c);


        GLES20.glGenTextures(1, tex, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex[0]);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, b, 0);
        b.recycle();
    }

    TextTex(Bitmap b) {//todo rename class
        w = b.getWidth();
        h = b.getHeight();

        GLES20.glGenTextures(1, tex, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex[0]);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, b, 0);
    }

    public final void release() {
        if (released) {
            return;
        }
        released = true;
        try {
            GLES20.glDeleteTextures(1, tex, 0);
        } catch (Throwable e) {
            if (MainActivity.LOGGING) Log.e(MainActivity.TAG, "err", e);
        }
    }
}
