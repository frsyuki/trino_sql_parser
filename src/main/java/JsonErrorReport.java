import com.fasterxml.jackson.annotation.JsonProperty;

public class JsonErrorReport
{
    private final String message;
    private final int line;
    private final int column;

    public JsonErrorReport(String message, int line, int column)
    {
        this.message = message;
        this.line= line;
        this.column= column;
    }

    @JsonProperty
    public String getMessage() { return message; }

    @JsonProperty
    public int getLine() { return line; }

    @JsonProperty
    public int getColumn() { return column; }
}
