package telegram.challenge.charts;

import android.content.Context;
import android.opengl.Matrix;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import static org.junit.Assert.*;


@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {


    @Test
    public void calc2() {
        float[] MVP = new float[16];
        Matrix.setIdentityM(MVP, 0);

//        Matrix.translateM(MVP, 0, -1.0f, -1.0f, 0);
        Matrix.scaleM(MVP, 0, 2f / 320f, 2f / 240f, 1.0f);

        float[] POS = new float[]{2f, 0f, 0f, 1f};
        float[] RES = new float[4];

        Matrix.multiplyMV(RES, 0, MVP, 0, POS, 0);
        System.out.println(Arrays.toString(RES));
    }

    @Test
    public void mvp() {
        float[] PROJ = new float[16];
        float[] MODEL = new float[16];
        float[] VIEW = new float[16];
        final int w = 720;
        final int h = 1280;
        Matrix.orthoM(PROJ, 0, 0, w, 0, h, -1.0f, 1.0f);

        Matrix.setIdentityM(MODEL, 0);
//        Matrix.scaleM(MODEL, 0, mywpx/(float)maxXValue, myhpx , 1.0f);

        int dx = 10;
        int dy = 10;
        Matrix.setIdentityM(VIEW, 0);
        Matrix.translateM(VIEW, 0, dx, dy, 0);
//        Matrix.scaleM(VIEW, 0, mywpx/(float)maxXValue, myhpx , 1.0f);
        eval(100, 100, MODEL, VIEW);
    }

    public void eval(int x, int y, float[]M, float[]V) {
        float[] vec = new float[]{x, y, 0, 0};
        float[] res = new float[]{0,0,0,0};

        float[] mv = new float[16];

        Matrix.multiplyMM(mv, 0, V, 0, M, 0);
        Matrix.multiplyMV(res, 0, mv, 0, vec, 0);
        Log.d("EVAL", Arrays.toString(res));


    }

    @Test
    public void date() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("E, MMM d", Locale.US);
        String format = simpleDateFormat.format(new Date());
        Log.d("DateFormat", "f " + format);
    }
}
