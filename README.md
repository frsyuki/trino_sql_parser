# trino_sql_parser

Trino SQL Parser for Ruby parses a SQL using Trino's native SQL parser precisely and reports syntax errors.

Optionally, it also returns a ANTLR tokens sequence which is useful to analyze and reformat SQL statements.

## Installation

Add this line to your application's Gemfile:

```ruby
gem 'trino_sql_parser'
```

And then execute:

    $ bundle

Or install it yourself as:

    $ gem install trino_sql_parser

## Runtime dependency

`java` command must be available because this TrinoSqlParser needs to use Trino's jar file.

If `java` is not available, such as in a pre-built Docker container, you would install java using following script:

```bash
if [[ ! -d ~/java/jre_11.0.4_11 ]]; then
    mkdir -p ~/java
    curl -L "https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk-11.0.4%2B11/OpenJDK11U-jre_x64_linux_hotspot_11.0.4_11.tar.gz" | tar zx -C ~/java
    mv ~/java/jdk-11.0.4+11-jre ~/java/jre_11.0.4_11
fi
echo 'export PATH=$HOME/java/jre_11.0.4_11/bin:$PATH' >> ~/.bashrc
```

On Circle CI 2.0, you can add following configuration to `steps` section:

```yaml
  - type: cache-restore
    key: jre_11.0.4_11
  - run:
      name: Install java
      command: |
        set -xe
        if [[ ! -d ~/java/jre_11.0.4_11 ]]; then
          mkdir -p ~/java
          curl -L "https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk-11.0.4%2B11/OpenJDK11U-jre_x64_linux_hotspot_11.0.4_11.tar.gz" | tar zx -C ~/java
          mv ~/java/jdk-11.0.4+11-jre ~/java/jre_11.0.4_11
        fi
        echo 'export PATH=$HOME/java/jre_11.0.4_11/bin:$PATH' >> $BASH_ENV
  - type: cache-save
    key: jre_11.04_11
    paths:
      - ~/java
```

## Usage

Most typical use case is checking syntax error as following:

```ruby
require 'trino_sql_parser'

parser = TrinoSqlParser.new
begin
  parser.parse("syntax error!")
rescue TrinoSqlParser::ParseError => e
  puts e.message

  # Detailed error information is available in ParseError#errors
  e.errors.each do |error|
    line = error['line']
    column = error['column']
    message = error['message']
    puts "Error at #{line}:#{column}: #{message}"
  end
end
```

Optionally, you can get ANTLR token list. It also supports multiple statements.

```ruby
require 'trino_sql_parser'

sql = <<SQL
select 1;

-- this is another statement:
select profiles.id, count(*) as count from events
left join profiles on events.profile_id = profiles.id
group by 1
SQL

parser = TrinoSqlParser.new(with_tokens: true)
statements = parser.parse(sql)

# First statement's tokens
p statements[0]['tokens']
#=> [
#     ["select", "'SELECT'", 1, 0],
#     [" ", "WS", 1, 6],
#     ["1", "INTEGER_VALUE", 1, 7], [";", "DELIMITER", 1, 8]
#   ]

# Second statement's tokens
p statements[1]['tokens']
```

## Options

```ruby
TrinoSqlParser.java_cmd = "java"  # java command (default: TRINO_SQL_PARSER_JAVA env var or "java")
TrinoSqlParser.java_args = []     # command-line arguments of java_cmd
TrinoSqlParser.java_env = {}      # environment variables given to java_cmd
```

## Development

### Build

```
bundle
bundle exec rake jar    # builds jar to lib/trino_sql_parser/trino-sql-parser.jar
bundle exec rake spec   # runs tests
bundle exec rake build  # builds a gem file
```

### Release

```
gem push pkg/trino_sql_parser-<version>.gem
```

### Update version and dependencies

* Gem version: `VERSION` at `lib/trino_sql_parser/version.rb`
* Trino version: dependency version at `build.gradle`

