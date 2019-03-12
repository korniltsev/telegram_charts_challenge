package korniltsev.telegram.charts.ui;


import android.util.Log;

import korniltsev.telegram.charts.BuildConfig;
import korniltsev.telegram.charts.MainActivity;

public class ColorSet {
    public static final ColorSet DAY = new ColorSet(0xff517DA2,
            0xffF0F0F0,
            0xffffffff,
            0x19182D3B,
            0xff446D91,
            0xff000000,
            0xbff1f5f7, 0x334f89b4,
            0xff426382,
            0xffE5EBEF,
            0xff000000, 0xffffffff,
            0xff96A2AA,
            0xff222222,
            0xffdedede,
            0xffE4E4E4,
            0x7f252529, 0x7f252529,
            true);
    public static final ColorSet NIGHT = new ColorSet(
            0xff212D3B,
            0xff161E27,
            0xff1D2733,
            0x19FFFFFF,
            0xff2E3E52,
            0xff7BC4FB,
            0xbf17212c, 0x3365abdf,
            0xff19242F,
            0xff131C26,
            0xffffffff,
            0xff202B38,
            0xff506372,
            0xffffffff,
            0xff253242,
            0xff1A232D,
            0x7fECF2F8, 0x99A3B1C2, false);
    public final int toolbar;
    public final int darkBackground;
    public final int lightBackground;
    public final int ruler;
    public final int pressedButton;

    public final int legendTitle;
    public final int scrollbarOverlay;
    public final int scrollbarBorder;
    public final int statusbar;
    public final int tooltipVerticalLine;
    public final int tooltipTitleColor;
    public final int tooltipBGColor;
    public final int rulerLabelColor;
    public final int textColor;
    public final int listButtonPressedColor;
    public final int tooltipFakeSHadowColor;

    public final int messagesYAxisText;
    public final int messagesXAxisText;
    public final boolean day;

    public ColorSet(int toolbar, int darkBackground, int lightBackground, int ruler, int pressedButton, int legendTitle, int scrollbarOverlay, int scrollbarBorder, int statusbar, int tooltipVerticalLine, int tooltipTitleColor, int tooltipBGColor, int rulerLabelColor, int textColor, int listButtonPressedColor, int tooltipFakeSHadowColor, int messagesYAxisText, int messagesXAxisText, boolean day) {
        this.toolbar = toolbar;
        this.darkBackground = darkBackground;
        this.lightBackground = lightBackground;
        this.ruler = ruler;
        this.pressedButton = pressedButton;
        this.legendTitle = legendTitle;
        this.scrollbarOverlay = scrollbarOverlay;
        this.scrollbarBorder = scrollbarBorder;
        this.statusbar = statusbar;
        this.tooltipVerticalLine = tooltipVerticalLine;
        this.tooltipTitleColor = tooltipTitleColor;
        this.tooltipBGColor = tooltipBGColor;
        this.rulerLabelColor = rulerLabelColor;
        this.textColor = textColor;
        this.listButtonPressedColor = listButtonPressedColor;
        this.tooltipFakeSHadowColor = tooltipFakeSHadowColor;
        this.messagesYAxisText = messagesYAxisText;
        this.messagesXAxisText = messagesXAxisText;
        this.day = day;
    }

    public int mapButtonColor(int color) {
        switch (color) {
            case 0xff4BD964: // green
                return day ? 0xff3CC23F : 0xff5AB34E;
            case 0xffFE3C30: // red
                return day ? 0xffF34C44 : 0xffCF5D57;
            case 0xff108BE3: // blue
                return day ? 0xff3497ED : 0xff4681BB;
            case 0xffE8AF14: // yellow
                return day ? 0xffF5BD26 : 0xffC9AF4F;
            case 0xff3497ED:
                return day ? 0xff3497ED : 0xff4681BB;
            case 0xff2373DB:
                return day ? 0xff3381E8 : 0xff466FB3;
            case 0xff9ED448:
                return day ? 0xff9ED448 : 0xff88BA52;
            case 0xff5FB641:
                return day ? 0xff5FB641 : 0xff3DA05A;
            case 0xffF5BD25:
                return day ? 0xffF5BD25 : 0xffF5BD25;
            case 0xffF79E39:
                return day ? 0xffF79E39 : 0xffD49548;
            case 0xffE65850:
                return day ? 0xffE65850 : 0xffCF5D57;

            case 0xff3896E8: return day? 0xff3896E8:0xff4082CE;
            case 0xff558DED: return day? 0xff558DED:0xff4461AB;
            case 0xff5CBCDF: return day? 0xff5CBCDF:0xff4697B3;
            // Blue 3497ED, Dark Blue 3381E8, Light Green 9ED448, Green 5FB641, Yellow F5BD25, Orange F79E39, Red E65850, Light Blue 35AADC
            default:
                if (BuildConfig.DEBUG) {
                    throw new AssertionError(Integer.toHexString(color));
                }
                return color;
        }
    }

    public int mapLineColor(int color) {
        switch (color) {
            case 0xff4BD964: // green
                return day ? 0xff3CC23F : 0xff5AB34E;
            case 0xffFE3C30: // red
                return day ? 0xffF34C44 : 0xffCF5D57;
            case 0xff108BE3: // blue
                return day ? 0xff108BE3 : 0xff4892DA;
            case 0xffE8AF14: // yellow
                return day ? 0xffE8AF13 : 0xffE3BD3F;
            case 0xff3497ED :               return day ? 0xff3497ED : 0xff4681BB;
            case 0xff2373DB :               return day ? 0xff2373DB : 0xff345B9C;
            case 0xff9ED448 :               return day ? 0xff9ED448 : 0xff88BA52;
            case 0xff5FB641 :               return day ? 0xff5FB641 : 0xff3DA05A;
            case 0xffF5BD25 :               return day ? 0xffF5BD25 : 0xffD9B856;
            case 0xffF79E39 :               return day ? 0xffF79E39 : 0xffD49548;
            case 0xffE65850 :               return day ? 0xffE65850 : 0xffCF5D57;

            case 0xff3896E8: return day? 0xff3896E8:0xff4082CE;
            case 0xff558DED: return day? 0xff558DED:0xff4461AB;
            case 0xff5CBCDF: return day? 0xff5CBCDF:0xff4697B3;
            default:
//                if (BuildConfig.DEBUG) {
//                    throw new AssertionError(Integer.toHexString(color));
//                }
                return color;
        }

    }
    public int mapLineText(int color) {
        switch (color) {
            case 0xff4BD964: // green
                return day ? 0xff3CC23F : 0xff5AB34E;
            case 0xffFE3C30: // red
                return day ? 0xffF34C44 : 0xffCF5D57;
            case 0xff108BE3: // blue
                return day ? 0xff108BE3 : 0xff1C8BE3;
            case 0xffE8AF14: // yellow
                return day ? 0xffE8AF13 : 0xffE9B219;
            case 0xff3896E8: return day? 0xff3896E8:0xff4082CE;
            case 0xff558DED: return day? 0xff558DED:0xff4461AB;
            case 0xff5CBCDF: return day? 0xff5CBCDF:0xff4697B3;
            default:
                if (BuildConfig.DEBUG) {
//                    AssertionError e = new AssertionError(Integer.toHexString(color));
//                    Log.e(MainActivity.TAG, "err", e);
//                    throw e;
                }
                return color;
        }

    }
}
