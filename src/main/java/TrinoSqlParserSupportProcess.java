
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import io.trino.sql.parser.ParsingException;
import io.trino.sql.parser.ParsingOptions.DecimalLiteralTreatment;
import io.trino.sql.parser.ParsingOptions;
import io.trino.sql.parser.SqlParser;
import io.trino.sql.parser.StatementSplitterWithOffsetRetained.Fragment;
import io.trino.sql.parser.StatementSplitterWithOffsetRetained;
import io.trino.sql.tree.Statement;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.nio.charset.StandardCharsets.UTF_8;

public class TrinoSqlParserSupportProcess
{
    public static void main(String[] args) throws Exception
    {
        boolean withTokens = false;
        boolean withStatement = false;

        for (String arg : args) {
            switch (arg) {
            case "--with-tokens":
                withTokens = true;
                break;
            case "--with-statement":
                withStatement = true;
                break;
            default:
                System.err.println("Unknown argument: " + arg);
                System.exit(1);
            }
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new GuavaModule());
        mapper.registerModule(new Jdk8Module());
        mapper.setDefaultTyping(new TrinoTreeNodeTypeResolver());

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, UTF_8));
        while (true) {
            String line = in.readLine();
            if (line == null) {
                return;
            }

            JsonRequest request = mapper.readValue(line, JsonRequest.class);
            JsonResult result = parse(request.getSql(), withTokens, withStatement);
            System.out.println(mapper.writeValueAsString(result));
        }
    }

    public static JsonResult parse(String sql,
            boolean withTokens, boolean withStatement)
    {
        ImmutableList.Builder<JsonStatement> statements = ImmutableList.builder();
        ImmutableList.Builder<JsonErrorReport> errors = ImmutableList.builder();

        List<Fragment> fragments = StatementSplitterWithOffsetRetained.split(sql);

        SqlParser parser = new SqlParser();

        for (Fragment fragment : fragments) {
            try {
                Statement statement = parser.createStatement(
                        fragment.getStatement(),
                        new ParsingOptions(DecimalLiteralTreatment.AS_DOUBLE));
                statements.add(new JsonStatement(fragment, statement, withTokens, withStatement));
            }
            catch (ParsingException ex) {
                errors.add(
                        new JsonErrorReport(
                            ex.getErrorMessage(),
                            ex.getLineNumber() + fragment.getLineOffset(),
                            ex.getColumnNumber() + (ex.getLineNumber() == 1 ? fragment.getFirstLineColumnOffset() : 0)));
            }
        }

        return new JsonResult(statements.build(), errors.build());
    }

    // Use this type resolver to include class name as "class" field. The field
    // name of "class" is safe because no node classes of io.trino.sql.tree.*
    // define getClass() method.
    private static class TrinoTreeNodeTypeResolver
            extends ObjectMapper.DefaultTypeResolverBuilder
    {
        public TrinoTreeNodeTypeResolver() {
            super(ObjectMapper.DefaultTyping.NON_FINAL);
            init(JsonTypeInfo.Id.NAME, null);
            inclusion(JsonTypeInfo.As.PROPERTY);
            typeProperty("class");
        }

        @Override
        public boolean useForType(JavaType t)
        {
            return t.getRawClass().getName().startsWith("io.trino.sql.tree");
        }
    }
}
