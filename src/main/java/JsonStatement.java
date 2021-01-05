
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.trino.sql.parser.StatementSplitterWithOffsetRetained.Fragment;
import io.trino.sql.parser.StatementSplitterWithOffsetRetained;
import io.trino.sql.tree.Statement;
import java.util.List;
import org.antlr.v4.runtime.Token;
import static com.google.common.collect.ImmutableList.toImmutableList;

public class JsonStatement
{
    @JsonFormat(shape=JsonFormat.Shape.ARRAY)
    @JsonPropertyOrder({"text", "type", "line", "column"})
    public static class JsonToken
    {
        private final String text;
        private final String type;
        private final int line;
        private final int column;

        public JsonToken(Token token)
        {
            this.text = token.getText();
            this.type = StatementSplitterWithOffsetRetained.getTokenName(token);
            this.line= token.getLine();
            this.column= token.getCharPositionInLine();
        }

        public String getText() { return text; }

        public String getType() { return type; }

        public int getLine() { return line; }

        public int getColumn() { return column; }
    }

    private final int line;
    private final int column;
    private final List<JsonToken> tokens;

    public JsonStatement(Fragment fragment, Statement statement, boolean withTokens)
    {
        this.line= fragment.getLineOffset() + 1;
        this.column= fragment.getFirstLineColumnOffset();
        if (withTokens) {
            this.tokens = fragment.getTokens().stream().map(JsonToken::new).collect(toImmutableList());
        }
        else {
            this.tokens = null;
        }
    }

    @JsonProperty
    public int getLine() { return line; }

    @JsonProperty
    public int getColumn() { return column; }

    @JsonProperty
    public List<JsonToken> getTokens() { return tokens; }
}
