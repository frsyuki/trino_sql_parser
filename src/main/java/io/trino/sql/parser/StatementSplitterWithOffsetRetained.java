// This code is in this package following classes are package-private:
//   io.trino.sql.parser.DelimiterLexer
package io.trino.sql.parser;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenSource;
import org.antlr.v4.runtime.Vocabulary;

public class StatementSplitterWithOffsetRetained
{
    private static final Vocabulary vocabulary = new SqlBaseParser(null).getVocabulary();

    public static String getTokenName(Token token)
    {
        return vocabulary.getDisplayName(token.getType());
    }

    public static List<Fragment> split(String sql)
    {
        return new StatementSplitterWithOffsetRetained().run(sql);
    }

    private int currentLine;
    private int currentCharPositionInLine;
    private List<Token> currentFragmentTokens = new ArrayList<>();

    private StatementSplitterWithOffsetRetained()
    { }

    private void resetState(int nextLine, int nextCharPositionInLine)
    {
        this.currentLine = nextLine;
        this.currentCharPositionInLine = nextCharPositionInLine;
        this.currentFragmentTokens.clear();
    }

    private List<Fragment> run(String sql)
    {
        ImmutableList.Builder<Fragment> results = ImmutableList.builder();
        resetState(1, 0);

        TokenSource tokens = getLexer(sql, ImmutableSet.of(";"));
        while (true) {
            Token token = tokens.nextToken();
            if (token.getType() == Token.EOF) {
                completeFragmentTo(results, null);
                return results.build();
            }
            else if (token.getType() == SqlBaseParser.DELIMITER) {
                completeFragmentTo(results, token);
                resetState(token.getLine(), token.getCharPositionInLine() + token.getText().length());
            }
            else {
                currentFragmentTokens.add(token);
            }
        }
    }

    private void completeFragmentTo(ImmutableList.Builder<Fragment> results, Token deliminatorToken)
    {
        String statement = currentFragmentTokens.stream().map(Token::getText).collect(Collectors.joining(""));
        Token firstToken = findFirstNonWhitespaceToken(statement);
        if (firstToken != null) {  // skip empty statements
            if (deliminatorToken != null) {
                currentFragmentTokens.add(deliminatorToken);
            }
            int lineOffset = (currentLine - 1) + (firstToken.getLine() - 1);
            int firstLineColumnOffset = firstToken.getCharPositionInLine() + (firstToken.getLine() == 1 ? currentCharPositionInLine : 0);
            statement = removeLines(statement, firstToken.getLine() - 1);
            statement = statement.substring(firstToken.getCharPositionInLine());
            results.add(new Fragment(statement, lineOffset, firstLineColumnOffset, ImmutableList.copyOf(currentFragmentTokens)));
        }
    }

    private static String removeLines(String string, int count)
    {
        if (count > 0) {
            String[] lines = string.split("\\n");
            return Stream.of(lines).skip(count).collect(Collectors.joining("\n"));
        }
        else {
            return string;
        }
    }

    private static Token findFirstNonWhitespaceToken(String statement)
    {
        TokenSource tokens = getLexer(statement, ImmutableSet.of());
        while (true) {
            Token token = tokens.nextToken();
            if (token.getType() == Token.EOF) {
                return null;
            }
            if (token.getChannel() != Token.HIDDEN_CHANNEL) {
                return token;
            }
        }
    }

    private static TokenSource getLexer(String sql, Set<String> terminators)
    {
        CharStream stream = new CaseInsensitiveStream(new ANTLRInputStream(sql));
        return new DelimiterLexer(stream, terminators);
    }

    public static class Fragment
    {
        private final String statement;
        private final int lineOffset;
        private final int firstLineColumnOffset;
        private final List<Token> tokens;

        public Fragment(String statement, int lineOffset, int firstLineColumnOffset, List<Token> tokens)
        {
            this.statement = statement;
            this.lineOffset = lineOffset;
            this.firstLineColumnOffset = firstLineColumnOffset;
            this.tokens = tokens;
        }

        public String getStatement()
        {
            return statement;
        }

        public int getLineOffset()
        {
            return lineOffset;
        }

        public int getFirstLineColumnOffset()
        {
            return firstLineColumnOffset;
        }

        public List<Token> getTokens()
        {
            return tokens;
        }
    }
}
