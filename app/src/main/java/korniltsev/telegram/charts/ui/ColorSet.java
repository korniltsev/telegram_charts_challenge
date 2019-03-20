package korniltsev.telegram.charts.ui;


public class ColorSet {
    //    public static final int COLOR_ACTION_BAR_LIGHT = 0xff517DA2;//todo make a theme object
    //    public static final int COLOR_ACTION_BAR_DARK = 0xff212D3B;//todo make a theme object
        public static final ColorSet DAY = new ColorSet(0xff517DA2,
            0xffF0F0F0,
            0xffffffff,
            0xffE7E8E9,
            0xff446D91,
            0xff3896D4);
    public static final ColorSet NIGHT = new ColorSet(
            0xff212D3B,
            0xff161E27,
            0xff1D2733,
            0xff101924,
            0xff2E3E52,
            0xff7BC4FB);
    public final int toolbar;
    public final int darkBackground;
    public final int lightBackground;
    public final int ruler;
    public final int pressedButton;

    public final int legendTitle;

    public ColorSet(int toolbar, int darkBackground, int lightBackground, int ruler, int pressedButton, int legendTitle) {
        this.toolbar = toolbar;
        this.darkBackground = darkBackground;
        this.lightBackground = lightBackground;
        this.ruler = ruler;
        this.pressedButton = pressedButton;
        this.legendTitle = legendTitle;
    }
}
