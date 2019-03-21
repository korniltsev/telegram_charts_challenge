package telegram.challenge.charts;

import android.content.Context;
import android.opengl.Matrix;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

import static org.junit.Assert.*;


@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {


    @Test
    public void calc2() {
        float[] MVP = new float[16];
        Matrix.setIdentityM(MVP, 0);

//        Matrix.translateM(MVP, 0, -1.0f, -1.0f, 0);
        Matrix.scaleM(MVP, 0, 2f/320f, 2f/240f, 1.0f);

        float[] POS = new float[]{2f, 0f, 0f, 1f};
        float[] RES = new float[4];

        Matrix.multiplyMV(RES, 0, MVP, 0, POS, 0);
        System.out.println(Arrays.toString(RES));
    }
}
