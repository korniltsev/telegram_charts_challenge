package korniltsev.telegram.charts.gl;

import korniltsev.telegram.charts.ui.MyAnimation;

public class ZoomState {
    public float left;
    public float right;
    public float scale;

    public MyAnimation.Float leftAnim;
    public MyAnimation.Float rightAnim;
}
