require 'presto_sql_parser'

RSpec.describe PrestoSqlParser do
  let(:parser) do
    PrestoSqlParser.new
  end

  it "raises ParseError::ParseError" do
    expect(lambda { parser.parse("...") }).to raise_error(PrestoSqlParser::ParseError)
  end

  describe PrestoSqlParser::ParseError do
    it "includes detailed error messages" do
      e = nil
      begin
        parser.parse("xxx; yyy")
      rescue PrestoSqlParser::ParseError
        e = $!
      end
      expect(e.errors[0]['message']).to include("mismatched input 'xxx'")
      expect(e.errors[0]['line']).to eq(1)
      expect(e.errors[1]['message']).to include("mismatched input 'yyy'")
      expect(e.errors[1]['line']).to eq(1)
    end
  end

  context "with_tokens" do
    let(:parser) do
      PrestoSqlParser.new(with_tokens: true)
    end

    it "returns token list" do
      statements = parser.parse("select 1; select * from t")
      text, type, line, column = *statements[0]['tokens'].first
      expect(text).to eq("select")
      expect(line).to eq(1)
      expect(column).to eq(0)
    end
  end
end
