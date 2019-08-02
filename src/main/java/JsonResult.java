import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class JsonResult
{
    private final List<JsonStatement> statements;
    private final List<JsonErrorReport> errors;

    public JsonResult(List<JsonStatement> statements, List<JsonErrorReport> errors)
    {
        if (errors.isEmpty()) {
            this.statements = statements;
            this.errors = null;
        }
        else {
            this.statements = null;
            this.errors = errors;
        }
    }

    @JsonProperty
    public List<JsonStatement> getStatements() { return statements; }

    @JsonProperty
    public List<JsonErrorReport> getErrors() { return errors; }
}
