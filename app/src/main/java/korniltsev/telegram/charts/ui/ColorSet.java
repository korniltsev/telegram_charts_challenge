package korniltsev.telegram.charts.ui;


public class ColorSet {
        public static final ColorSet DAY = new ColorSet(0xff517DA2,
            0xffF0F0F0,
            0xffffffff,
            0xffE7E8E9,
            0xff446D91,
            0xff3896D4,
                0xbff1f5f7, 0x334f89b4);
    public static final ColorSet NIGHT = new ColorSet(
            0xff212D3B,
            0xff161E27,
            0xff1D2733,
            0xff101924,
            0xff2E3E52,
            0xff7BC4FB,
            0xbf17212c, 0x3365abdf);
    public final int toolbar;
    public final int darkBackground;
    public final int lightBackground;
    public final int ruler;
    public final int pressedButton;

    public final int legendTitle;
    public final int scrollbarOverlay;
    public final int scrollbarBorder;

    public ColorSet(int toolbar, int darkBackground, int lightBackground, int ruler, int pressedButton, int legendTitle, int scrollbarOverlay, int scrollbarBorder) {
        this.toolbar = toolbar;
        this.darkBackground = darkBackground;
        this.lightBackground = lightBackground;
        this.ruler = ruler;
        this.pressedButton = pressedButton;
        this.legendTitle = legendTitle;
        this.scrollbarOverlay = scrollbarOverlay;
        this.scrollbarBorder = scrollbarBorder;
    }
}
