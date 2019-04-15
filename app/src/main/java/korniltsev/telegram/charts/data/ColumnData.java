package korniltsev.telegram.charts.data;

public class ColumnData {
    public String id;
    public String name;
    public final long[] values;
    public final long max;
    public final long min;
    public final Type type;
    public int color;


    public ColumnData(String id, String name, long[] values, Type type, int color) {
        this.id = id;
        this.name = name;
        this.values = values;
        long min = values[0];
        long max = values[0];
        for (long value : values) {
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        this.max = max;
        this.min = min;
        this.type = type;
        this.color = color;
    }

//    "line",
//            "area”,
//            "bar”,

    public enum Type {
        line("line"),
        area("area"),
        bar("bar"),;
        final String name;

        Type(String name) {
            this.name = name;
        }
    }

}
