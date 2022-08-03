require 'trino_sql_parser'

RSpec.describe TrinoSqlParser do
  let(:parser) do
    TrinoSqlParser.new
  end

  it "raises ParseError::ParseError" do
    expect { parser.parse("...") }.to raise_error(TrinoSqlParser::ParseError)
  end

  describe TrinoSqlParser::ParseError do
    it "includes detailed error messages" do
      e = nil
      begin
        parser.parse("xxx; yyy")
      rescue TrinoSqlParser::ParseError
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
      TrinoSqlParser.new(with_tokens: true)
    end

    it "returns token list" do
      statements = parser.parse("select 1; select func(time, '%S%S'), * from t")
      text, type, line, column = *statements[0]['tokens'].first
      expect(text).to eq("select")
      expect(line).to eq(1)
      expect(column).to eq(0)
      expect(statements[0].has_key?('statements')).to eq(false)
    end
  end

  context "with_statement" do
    let(:parser) do
      TrinoSqlParser.new(with_statement: true)
    end

    it "returns token list" do
      statements = parser.parse("select 1; select func(time, '%Y%m%d') as f, * from t")

      select_items = statements[1]['statement']['children'][0]['children'][0]['selectItems']

      select_item_classes = select_items.map {|n| n['class'] }
      expect(select_item_classes).to eq(['SingleColumn', 'AllColumns'])

      func_alias = select_items[0]['alias']['value']
      expect(func_alias).to eq('f')

      func_arg_classes = select_items[0]['expression']['arguments'].map {|n| n['class'] }
      func_arg_values = select_items[0]['expression']['arguments'].map {|n| n['value'] }
      expect(func_arg_classes).to eq(['Identifier', 'StringLiteral'])
      expect(func_arg_values).to eq(['time', '%Y%m%d'])

      expect(statements[0].has_key?('tokens')).to eq(false)
    end
  end
end
