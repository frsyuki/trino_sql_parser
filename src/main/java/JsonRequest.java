import com.fasterxml.jackson.annotation.JsonProperty;

public class JsonRequest
{
    private final String sql;

    public JsonRequest(@JsonProperty("sql") String sql)
    {
        this.sql = sql;
    }

    @JsonProperty
    public String getSql() { return sql; }
}
