package korniltsev.telegram.charts.ui;


public class ColorSet {
    //    public static final int COLOR_ACTION_BAR_LIGHT = 0xff517DA2;//todo make a theme object
    //    public static final int COLOR_ACTION_BAR_DARK = 0xff212D3B;//todo make a theme object
        public static final ColorSet DAY = new ColorSet(0xff517DA2, 0xffF0F0F0,0xffffffff, 0xffE7E8E9);
    public static final ColorSet NIGHT = new ColorSet(0xff212D3B, 0xff161E27,0xff1D2733, 0xff101924);
    public final int toolbar;
    public final int darkBackground;
    public final int lightBackground;
    public final int ruler;

    public ColorSet(int toolbar, int darkBackground, int lightBackground, int ruler) {
        this.toolbar = toolbar;
        this.darkBackground = darkBackground;
        this.lightBackground = lightBackground;
        this.ruler = ruler;
    }
}
