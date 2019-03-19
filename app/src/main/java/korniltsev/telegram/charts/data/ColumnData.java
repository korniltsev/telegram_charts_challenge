package korniltsev.telegram.charts.data;

public class ColumnData {
    public final String id;
    public final String name;
    public final long[] values;
    public final long maxValue;
    public final long minValue;
    public final String type;
    public final int color;


    public ColumnData(String id, String name, long[] values, long maxValue, long minValue, String type, int color) {
        this.id = id;
        this.name = name;
        this.values = values;
        this.maxValue = maxValue;
        this.minValue = minValue;
        this.type = type;
        this.color = color;
    }


}
