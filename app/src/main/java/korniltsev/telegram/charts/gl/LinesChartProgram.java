package korniltsev.telegram.charts.gl;

import android.opengl.GLES20;
import android.opengl.Matrix;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import korniltsev.telegram.charts.MainActivity;
import korniltsev.telegram.charts.R;
import korniltsev.telegram.charts.data.ColumnData;
import korniltsev.telegram.charts.ui.ColorSet;
import korniltsev.telegram.charts.ui.Dimen;
import korniltsev.telegram.charts.ui.MyAnimation;
import korniltsev.telegram.charts.ui.MyColor;

public final class LinesChartProgram {

    public static final int BYTES_PER_FLOAT = 4;
    private static final int STRIDE_BYTES = 2 * BYTES_PER_FLOAT;
    private static final int POSITION_DATA_SIZE = 2;
//    public static final int CHART_HEIGHT = 280;


    private final int vbo;
    private final float[] vertices;
    private final FloatBuffer buf1;
    public final ColumnData column;
    public final MyCircles lineJoining;
    public final MyShader shader;
    public float zoom = 1f;//1 -- all, 0.2 - partial
    public float left = 0;
    public long scaledViewporMax;
    public long scaledViewporMin;
    private int tooltipIndex = -1;

    //    private float[] M = new float[16];
    public float[] V_ = new float[16];
    public float[] V2_ = new float[16];
    public final float[] MVP_ = new float[16];
    public final float[] MVP2_ = new float[16];

    public final int w;
    public final int h;

    private final Dimen dimen;

    public float maxValue;
    public float minValue;

    final ChartViewGL root;

    final boolean scrollbar;
    public MyCircles goodCircle;
    //    private int goodCircleIndex;
    private MyAnimation.Color tooltipFillColorAnim;
    private final int[] vbos;
    private boolean released;
    public boolean zoomedIn;
    private float animInCenter;
    private float animInY;


    public LinesChartProgram(ColumnData column, int w, int h, Dimen dimen, ChartViewGL root, boolean scrollbar, ColorSet colors, MyShader shader, MyCircles.Shader joiningShader) {
        this.tooltipFillColor = colors.lightBackground;
        this.w = w;
        this.h = h;
        this.column = column;
        this.dimen = dimen;
        this.root = root;
        this.scrollbar = scrollbar;

        long[] values = column.values;
        vertices = new float[values.length * 2];
        for (int i = 0, valuesLength = values.length; i < valuesLength; i++) {
            long value = values[i];
            vertices[i * 2] = i;
            vertices[i * 2 + 1] = value;
        }
        buf1 = ByteBuffer.allocateDirect(vertices.length * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        buf1.put(vertices);
        buf1.position(0);


        this.shader = shader;


        vbos = new int[1];
        GLES20.glGenBuffers(1, vbos, 0);
        vbo = vbos[0];
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertices.length * BYTES_PER_FLOAT, buf1, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        //todo reuse line joining with scrollbar
        lineJoining = new MyCircles(w, h, 0, column.values, 6, joiningShader);

    }

    public static void initMinMax(boolean y_scaled, LinesChartProgram[]cs, float left, float right, GLRulersProgram ruler, boolean animateRuler, ChartViewGL root) {
        if (y_scaled) {
            for (LinesChartProgram c : cs) {
                LinesChartProgram.calculateChartLinesMaxScaled(c, left, right, root);
                c.animateMinMax(c.scaledViewporMin, c.scaledViewporMax, false, 0);
            }
            if (animateRuler) {
                ruler.animateScale(
                        cs[0].scaledViewporMin, cs[0].scaledViewporMax,
                        cs[1].scaledViewporMin, cs[1].scaledViewporMax, 208
                );
            } else {
                ruler.init(
                        cs[0].scaledViewporMin, cs[0].scaledViewporMax,
                        cs[1].scaledViewporMin, cs[1].scaledViewporMax,
                        cs[0].column.color,
                        cs[1].column.color
                );
            }
        } else {
            LinesChartProgram.calculateChartLinesMax3(cs, left, right, root); // draw ( init)
            if (animateRuler) {
                ruler.animateScale(cs[0].scaledViewporMin, cs[0].scaledViewporMax, 208);
            } else {
                ruler.init(cs[0].scaledViewporMin, cs[0].scaledViewporMax);
            }
            for (LinesChartProgram c : cs) {
                c.animateMinMax(c.scaledViewporMin, c.scaledViewporMax, false, 0);
            }
        }
    }

    public void release() {
        if (released) {
            return;
        }
        released = true;
        GLES20.glDeleteBuffers(1, vbos, 0);
        lineJoining.release();
    }

    float[] colors = new float[4];

    //    float tooltipFillColorAlpha = 1f;
    int tooltipFillColor;
    float[] white = new float[4];

    public boolean animateionTick(long t) {
        boolean invalidate = false;
        if (alphaAnim != null) {
            alpha = alphaAnim.tick(t);
            if (alphaAnim.ended) {
                alphaAnim = null;
            } else {
                invalidate = true;
            }
        }
        if (minAnim != null) {
            minValue = minAnim.tick(t);
            if (minAnim.ended) {
                minAnim = null;
            } else {
                invalidate = true;
            }
        }

        if (maxAnim != null) {
            maxValue = maxAnim.tick(t);
            if (maxAnim.ended) {
                maxAnim = null;
            } else {
                invalidate = true;
            }
        }
        if (tooltipFillColorAnim != null) {
            tooltipFillColor = tooltipFillColorAnim.tick(t);
            if (tooltipFillColorAnim.ended) {
                tooltipFillColorAnim = null;
            } else {
                invalidate = true;
            }
        }
        if (animateOutValueAnim != null) {
            animateOutValue = animateOutValueAnim.tick(t);
            if (animateOutValueAnim.ended) {
                animateOutValueAnim = null;
                if (!zoomedIn) {
                    animateOutValue = -1f;
                }
            } else {
                invalidate = true;
            }
        }
        if (animateInValueAnim != null) {
            animateInValue = animateInValueAnim.tick(t);
            if (animateInValueAnim.ended) {
                animateInValueAnim = null;
                if (!zoomedIn) {
                    animateInValue = -1f;
                }
            } else {
                invalidate = true;
            }
        }

        return invalidate;

    }

    float[] tmpvec = new float[4];
    float[] tmpvec2 = new float[4];

    public void step1(float[] PROJ) {
        float hpadding = dimen.dpf(16);
        float maxx = vertices[vertices.length - 2];

        if (scrollbar) {

            hpadding += dimen.dpf(1);
            final float dip2 = dimen.dpf(2);

            final float w = this.w - 2 * hpadding;
            final float h = root.dimen_scrollbar_height - 2 * dip2;
            final float yscale = h / (maxValue - minValue);
            final float dy = -yscale * minValue;

            final float y = root.dimen_v_padding8 + root.checkboxesHeight + dip2;


            if (animateOutValue != -1f) {
                float leftAnimDistance = getTooltipX();//todo padding?
                float rightAnimDistance = this.w - tmpvec2[0];
                float posndcx = (tmpvec2[0] + 1f) / 2f;

                Matrix.setIdentityM(V_, 0);
                Matrix.translateM(V_, 0, hpadding, y, 0);
                Matrix.translateM(V_, 0, 0, dy, 0);
                Matrix.translateM(V_, 0, -leftAnimDistance * animateOutValue, 0f, 0f);
                Matrix.scaleM(V_, 0, w / ((maxx)), yscale, 1.0f);
                Matrix.multiplyMM(MVP_, 0, PROJ, 0, V_, 0);

                Matrix.setIdentityM(V2_, 0);
                Matrix.translateM(V2_, 0, hpadding, y, 0);
                Matrix.translateM(V2_, 0, 0, dy, 0);
                Matrix.translateM(V2_, 0, rightAnimDistance * animateOutValue, 0f, 0f);
                Matrix.scaleM(V2_, 0, w / ((maxx)), yscale, 1.0f);
                Matrix.multiplyMM(MVP2_, 0, PROJ, 0, V2_, 0);
            } else {
                Matrix.setIdentityM(V_, 0);
                Matrix.translateM(V_, 0, hpadding, y, 0);
                Matrix.translateM(V_, 0, 0, dy, 0);
                Matrix.scaleM(V_, 0, w / ((maxx)), yscale, 1.0f);
                Matrix.multiplyMM(MVP_, 0, PROJ, 0, V_, 0);
            }
        } else {
            final int ypx = dimen.dpi(80) + root.checkboxesHeight;

            final float w = this.w - 2 * hpadding;
            final int h = root.dimen_chart_usefull_height;
            final float xdiff = maxx;
            final float ws = w / xdiff / zoom;
            final float hs = h / (maxValue - minValue);
            final float dy = -hs * minValue;

            if (animateInValue != -1f) {
                Matrix.setIdentityM(V_, 0);

                Matrix.translateM(V_, 0, animInCenter, 0, 0);
                Matrix.scaleM(V_, 0, animateInValue, 1f, 1f);
                Matrix.translateM(V_, 0, -animInCenter, 0, 0);

                Matrix.translateM(V_, 0, hpadding, ypx, 0);
                Matrix.translateM(V_, 0, 0, dy, 0);

                Matrix.scaleM(V_, 0, ws, hs, 1.0f);
                Matrix.translateM(V_, 0, -left * xdiff, 0f, 0f);
                Matrix.multiplyMM(MVP_, 0, PROJ, 0, V_, 0);
            } else if (animateOutValue != -1f) {
                float x = getTooltipX();
                float leftAnimDistance = x - leftx + dimen.dpf(16);
                float rightAnimDistance = rightx-x + dimen.dpf(16);
                float posndcx = (tmpvec2[0] + 1f) / 2f;

                Matrix.setIdentityM(V_, 0);
                Matrix.translateM(V_, 0, hpadding, ypx, 0);
                Matrix.translateM(V_, 0, 0, dy, 0);
                Matrix.translateM(V_, 0, -leftAnimDistance * animateOutValue, 0f, 0f);
                Matrix.scaleM(V_, 0, ws, hs, 1.0f);
                Matrix.translateM(V_, 0, -left * xdiff, 0f, 0f);
                Matrix.multiplyMM(MVP_, 0, PROJ, 0, V_, 0);

                Matrix.setIdentityM(V2_, 0);
                Matrix.translateM(V2_, 0, hpadding, ypx, 0);
                Matrix.translateM(V2_, 0, 0, dy, 0);
                Matrix.translateM(V2_, 0, rightAnimDistance * animateOutValue, 0f, 0f);
                Matrix.scaleM(V2_, 0, ws, hs, 1.0f);
                Matrix.translateM(V2_, 0, -left * xdiff, 0f, 0f);
                Matrix.multiplyMM(MVP2_, 0, PROJ, 0, V2_, 0);
            } else {
                {
                    Matrix.setIdentityM(V_, 0);
                    Matrix.translateM(V_, 0, hpadding, ypx, 0);
                    Matrix.translateM(V_, 0, 0, dy, 0);
                    Matrix.scaleM(V_, 0, ws, hs, 1.0f);
                    Matrix.translateM(V_, 0, -left * xdiff, 0f, 0f);
                    Matrix.multiplyMM(MVP_, 0, PROJ, 0, V_, 0);
                }
            }
        }

    }
    public float outTooltipX;// screen space
    public float outTooltipY;// screen space
    public float getTooltipX() {
        float hpadding = dimen.dpf(16);
        float maxx = vertices[vertices.length - 2];
        if (scrollbar) {
            hpadding += dimen.dpf(1);
            final float dip2 = dimen.dpf(2);

            final float w = this.w - 2 * hpadding;
            final float h = root.dimen_scrollbar_height - 2 * dip2;
            final float yscale = h / (maxValue - minValue);
            final float dy = -yscale * minValue;

            final float y = root.dimen_v_padding8 + root.checkboxesHeight + dip2;
            Matrix.setIdentityM(V_, 0);
            Matrix.translateM(V_, 0, hpadding, y, 0);
            Matrix.translateM(V_, 0, 0, dy, 0);
            Matrix.scaleM(V_, 0, w / ((maxx)), yscale, 1.0f);
        } else {
            final int ypx = dimen.dpi(80) + root.checkboxesHeight;

            final float w = this.w - 2 * hpadding;
            final int h = root.dimen_chart_usefull_height;
            final float xdiff = maxx;
            final float ws = w / xdiff / zoom;
            final float hs = h / (maxValue - minValue);
            final float dy = -hs * minValue;
            Matrix.setIdentityM(V_, 0);
            Matrix.translateM(V_, 0, hpadding, ypx, 0);
            Matrix.translateM(V_, 0, 0, dy, 0);
            Matrix.scaleM(V_, 0, ws, hs, 1.0f);
            Matrix.translateM(V_, 0, -left * xdiff, 0f, 0f);
        }

        tmpvec[0] = tooltipIndex;
        tmpvec[1] = column.values[tooltipIndex];
        tmpvec[2] = 0f;
        tmpvec[3] = 1;
        Matrix.multiplyMV(tmpvec2, 0, V_, 0, tmpvec, 0);
        outTooltipX = tmpvec2[0];
        outTooltipY = tmpvec2[1];
        return tmpvec2[0];
    }

    public void step4() {

        float scalex = 2.0f / w;
        if (scrollbar) {
        } else {
            if (alpha != 0f) {

                if (goodCircle != null && animateOutValue == -1f) {
                    goodCircle.draw(MVP_, colors, 0, 1, dimen.dpf(5) * scalex, (float) h / w);
                    white[0] = MyColor.red(tooltipFillColor) / 255f;
                    white[1] = MyColor.green(tooltipFillColor) / 255f;
                    white[2] = MyColor.blue(tooltipFillColor) / 255f;
                    white[3] = alpha;
                    goodCircle.draw(MVP_, white, 0, 1, dimen.dpf(3) * scalex, (float) h / w);
                }
            }
        }

    }

    public void step2() {

        colors[0] = MyColor.red(column.color) / 255f;
        colors[1] = MyColor.green(column.color) / 255f;
        colors[2] = MyColor.blue(column.color) / 255f;
//        if (animateOutValue == -1f) {
            colors[3] = alpha;
//        } else {
//            colors[3] = alpha;
//        }
        GLES20.glUniform4fv(shader.colorHandle, 1, colors, 0);


        GLES20.glEnableVertexAttribArray(shader.positionHandle);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
        GLES20.glVertexAttribPointer(shader.positionHandle, POSITION_DATA_SIZE, GLES20.GL_FLOAT, false, STRIDE_BYTES, 0);

        float scalex = 2.0f / w;


        if (scrollbar) {
            GLES20.glLineWidth(dimen.dpf(1f));
        } else {
            GLES20.glLineWidth(dimen.dpf(2f));
        }
        if (animateInValue != -1f) {
            GLES20.glUniformMatrix4fv(shader.MVPHandle, 1, false, MVP_, 0);
            if (animateInValue == 1f) {
                GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, vertices.length / 2);
            } else {
                GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, leftxi, rightxi - leftxi+1);
            }
        } else if (animateOutValue != -1f) {
            if (tooltipIndex != -1) {
                GLES20.glUniformMatrix4fv(shader.MVPHandle, 1, false, MVP_, 0);
                GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, (tooltipIndex + 1));

                GLES20.glUniformMatrix4fv(shader.MVPHandle, 1, false, MVP2_, 0);
                int n = vertices.length / 2;
                int from = tooltipIndex;
                GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, tooltipIndex, n - from);
            }
        } else {
            GLES20.glUniformMatrix4fv(shader.MVPHandle, 1, false, MVP_, 0);
            GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, vertices.length / 2);
        }

    }


    public void step3() {
        float scalex = 2.0f / w;
        float r_ndc = scalex * dimen.dpf(scrollbar ? 0.5f : 1f);
        if (animateInValue != -1f) {
            if (animateInValue == 1f) {
                lineJoining.draw(MVP_, colors, r_ndc, (float) h / w);
            } else {
                lineJoining.draw(MVP_, colors, leftxi, rightxi, r_ndc, (float) h / w);
            }
        } else if (animateOutValue != -1f) {
            if (tooltipIndex != -1) {
                int n = vertices.length / 2;
                lineJoining.draw(MVP_, colors, 0, tooltipIndex + 1, r_ndc, (float) h / w);
                MyGL.checkGlError2();
                lineJoining.draw(MVP2_, colors, tooltipIndex, n, r_ndc, (float) h / w);
                MyGL.checkGlError2();
            }
        } else {
            lineJoining.draw(MVP_, colors, r_ndc, (float) h / w);
        }
    }

    boolean checked = true;
    MyAnimation.Float alphaAnim;
    MyAnimation.Float minAnim;
    MyAnimation.Float maxAnim;
    float alpha = 1f;

    public void animateAlpha(boolean isChecked) {
        if (this.checked != isChecked) {
            alphaAnim = new MyAnimation.Float(MyAnimation.ANIM_DRATION, alpha, isChecked ? 1.0f : 0.0f);
            this.checked = isChecked;
        }
    }

    public void animateMinMax(long min, long max, boolean animate, long duration) {
        if (alpha == 0f || !animate) {
            minValue = min;
            maxValue = max;
            minAnim = null;
            maxAnim = null;
        } else {
            if (minValue == min) {
                minAnim = null;
            } else {
                minAnim = new MyAnimation.Float(duration, minValue, min);
            }
            if (maxValue == max) {
                maxAnim = null;
            } else {
                maxAnim = new MyAnimation.Float(duration, maxValue, max);
            }
        }

    }

    public void animateColors(ColorSet colors, long duration) {
        tooltipFillColorAnim = new MyAnimation.Color(duration, tooltipFillColor, colors.lightBackground);
    }

    public int getTooltipIndex() {
        return tooltipIndex;
    }

    public float animateOutValue = -1f;
    public float animateInValue = -1f;
    private MyAnimation.Float animateOutValueAnim;
    private MyAnimation.Float animateInValueAnim;



    public void setTooltipIndex(int tooltipIndex) {
        if (!scrollbar) {
            if (goodCircle == null || this.tooltipIndex != tooltipIndex) {
                if (goodCircle != null) {
//                goodCircle.release();//todo
                    goodCircle = null;

                }
                if (tooltipIndex != -1) {
                    long[] vs = new long[]{column.values[tooltipIndex]};
                    goodCircle = new MyCircles(this.w, this.h, tooltipIndex, vs, 24, new MyCircles.Shader(24));
                }
            }
        }
        this.tooltipIndex = tooltipIndex;
    }

    public static void calculateChartLinesMax3(LinesChartProgram[] cs, float left, float right, ChartViewGL root) {
        float scale = right - left;// between paddings
        int padding = root.scrollbarPos.left;
        float x = scale * padding / root.scrollbarPos.width();


        long max = Long.MIN_VALUE;
        long min = Long.MAX_VALUE;
        int len = cs[0].column.values.length;
        int from = Math.max(0,  -1 + (int) Math.ceil(len * (left - x)));
        int to = Math.min(len, 1 + (int) Math.floor(len * (right + x)));
        for (LinesChartProgram glChartProgram : cs) {
            if (glChartProgram.checked) {
                long[] values = glChartProgram.column.values;
                for (int i = from; i < to; i++) {
                    long value = values[i];
                    max = (max >= value) ? max : value;
                    min = Math.min(min, value);
                }
            }
        }
        for (LinesChartProgram c : cs) {
            c.scaledViewporMin = min;
            c.scaledViewporMax = max;
        }

    }

    public static void calculateChartLinesMaxScaled(LinesChartProgram p, float left, float right, ChartViewGL root) {
        float scale = right - left;// between paddings
        int padding = root.scrollbarPos.left;
        float x = scale * padding / root.scrollbarPos.width();

        long max = Long.MIN_VALUE;
        long min = Long.MAX_VALUE;
        int len = p.column.values.length;
        int from = Math.max(0, -1 + (int) Math.ceil(len * (left - x)));
        int to = Math.min(len,  1 + (int) Math.floor(len * (right + x)));
        long[] values = p.column.values;
        for (int i = from; i < to; i++) {
            long value = values[i];
            max = (max >= value) ? max : value;
            min = Math.min(min, value);
        }
        p.scaledViewporMax = max;
        p.scaledViewporMin = min;
    }

    public static void setChecked(String id, ZoomState zoom, boolean isChecked, LinesChartProgram[] cs, GLRulersProgram ruler, boolean y_scaled, ChartViewGL root) {
        float left = zoom.left;
        float right = zoom.right;
        for (LinesChartProgram c : cs) {
//                            c.setTooltipIndex(-1);
            if (c.column.id.equals(id)) {
                c.animateAlpha(isChecked);
            }
        }
        if (y_scaled) {
            for (LinesChartProgram c : cs) {
                LinesChartProgram.calculateChartLinesMaxScaled(c, left, right, root);
                c.animateMinMax(c.scaledViewporMin, c.scaledViewporMax, true, 256);
            }
            if (ruler != null) {
                ruler.animateScale(
                        cs[0].scaledViewporMin, cs[0].scaledViewporMax,
                        cs[1].scaledViewporMin, cs[1].scaledViewporMax,
                        208);
            }
        } else {
            LinesChartProgram.calculateChartLinesMax3(cs, left, right, root);// set checked
            for (LinesChartProgram c : cs) {
                c.animateMinMax(c.scaledViewporMin, c.scaledViewporMax, true, 208);
            }
            if (ruler != null) {
                ruler.animateScale(cs[0].scaledViewporMin, cs[0].scaledViewporMax, 208);
            }
        }

    }

    public static void updateLeftRight(LinesChartProgram[] cs, float left, float right, float scale, boolean rulerInitDone, GLRulersProgram ruler, boolean y_scaled, boolean firstLeftRightUpdate, ChartViewGL root) {
        if (y_scaled) {
            for (LinesChartProgram c : cs) {
                LinesChartProgram.calculateChartLinesMaxScaled(c, left, right, root);
                c.zoom = scale;
                c.left = left;
                c.animateMinMax(c.scaledViewporMin, c.scaledViewporMax, !firstLeftRightUpdate, 256);
            }
            ruler.setLeftRight(left, right, scale);
            if (rulerInitDone) {
                ruler.animateScale(
                        cs[0].scaledViewporMin, cs[0].scaledViewporMax,
                        cs[1].scaledViewporMin, cs[1].scaledViewporMax,
                        256);
            }

        } else {
            LinesChartProgram.calculateChartLinesMax3(cs, left, right, root);// updateLeftRight

            for (LinesChartProgram glChartProgram : cs) {
                glChartProgram.zoom = scale;
                glChartProgram.left = left;
                glChartProgram.animateMinMax(glChartProgram.scaledViewporMin, glChartProgram.scaledViewporMax, !firstLeftRightUpdate, 256);
            }
            ruler.setLeftRight(left, right, scale);

            if (rulerInitDone) {
                ruler.animateScale(cs[0].scaledViewporMin, cs[0].scaledViewporMax, 256);
            }
        }
    }
    float leftx = Float.NaN;
    float rightx = Float.NaN;
    int leftxi = -1;
    int rightxi = -1;
    public void animateIn(int duration, boolean zoomedIn, float[] PROJ, float tooltipScreenpx, float outTooltipY) {
        if (zoomedIn == this.zoomedIn) {
            return;
        }
        calcAnimOffsets(PROJ);
        this.animInCenter = tooltipScreenpx;
        this.animInY = outTooltipY;
        this.zoomedIn = zoomedIn;
        if (animateInValue == -1f) {
            animateInValue = 0f;
        }
        animateInValueAnim = new MyAnimation.Float(duration, animateInValue, zoomedIn ? 1f : 0f);
    }

    private void calcAnimOffsets(float[] PROJ) {
        step1(PROJ);
        leftxi = -1;
        rightxi = -1;
        for (int i = 0; i < column.values.length; i++) {
            tmpvec[0] = i;
            tmpvec[1] = 0f;
            tmpvec[2] = 0f;
            tmpvec[3] = 1f;
            Matrix.multiplyMV(tmpvec2, 0, V_, 0, tmpvec, 0);
            float x = tmpvec2[0];
            if (leftxi == -1 || x <= 0) {
                leftx = x;
                leftxi = i;
            } else if (x >= w) {
                rightx = x;
                rightxi = i;
                break;
            } else if (i == column.values.length - 1) {
                rightx = x;
                rightxi = i;
            }
        }
    }

    public void animateOut(long duration, boolean zoomedIn, float leftx, float rightx) {
        if (zoomedIn == this.zoomedIn) {
            return;
        }
        this.leftx = leftx;
        this.rightx = rightx;
        this.zoomedIn = zoomedIn;
        if (animateOutValue == -1f) {
            animateOutValue = 0f;
        }
        animateOutValueAnim = new MyAnimation.Float(duration, animateOutValue, zoomedIn ? 1f : 0f);
    }

    public static final class MyShader {
        final String fragmentShader =
                "precision mediump float;       \n"
                        + "uniform vec4 u_color;       \n"
                        + "void main()                    \n"
                        + "{                              \n"
                        + "   gl_FragColor = u_color;     \n"
                        + "}                              \n";
        public final int MVPHandle;
        public final int positionHandle;
        public final int program;
        public final int colorHandle;
        private boolean released;

        public MyShader() {

            try {
                byte[] vertex = MainActivity.readAll(MainActivity.ctx.getResources().openRawResource(R.raw.lines_vertex));
                program = MyGL.createProgram(new String(vertex, "UTF-8"), fragmentShader);
            } catch (IOException e) {
                throw new AssertionError();
            }
            MVPHandle = GLES20.glGetUniformLocation(program, "u_MVPMatrix");
            positionHandle = GLES20.glGetAttribLocation(program, "a_Position");
            colorHandle = GLES20.glGetUniformLocation(program, "u_color");
        }

        public final void use() {
            GLES20.glUseProgram(program);
        }

        public void release() {
            if (released) {
                return;
            }
            released = true;
            GLES20.glDeleteProgram(program);
        }
    }

}
