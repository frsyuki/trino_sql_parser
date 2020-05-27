# presto_sql_parser

Presto SQL Parser for Ruby parses a SQL using Presto's native SQL parser precisely and reports syntax errors.

Optionally, it also returns a ANTLR tokens sequence which is useful to analyze and reformat SQL statements.

## Installation

Add this line to your application's Gemfile:

```ruby
gem 'presto_sql_parser'
```

And then execute:

    $ bundle

Or install it yourself as:

    $ gem install presto_sql_parser

## Runtime dependency

`java` command must be available because this PrestoSqlParser needs to use Presto's jar file.

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
require 'presto_sql_parser'

parser = PrestoSqlParser.new
begin
  parser.parse("syntax error!")
rescue PrestoSqlParser::ParseError => e
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
require 'presto_sql_parser'

sql = <<SQL
select 1;

-- this is another statement:
select profiles.id, count(*) as count from events
left join profiles on events.profile_id = profiles.id
group by 1
SQL

parser = PrestoSqlParser.new(with_tokens: true)
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
PrestoSqlParser.java_cmd = "java"  # java command (default: PRESTO_SQL_PARSER_JAVA env var or "java")
PrestoSqlParser.java_args = []     # command-line arguments of java_cmd
PrestoSqlParser.java_env = {}      # environment variables given to java_cmd
```

## Development

### Build

```
bundle
bundle exec rake jar    # builds jar to lib/presto_sql_parser/presto-sql-parser.jar
bundle exec rake spec   # runs tests
bundle exec rake build  # builds a gem file
```

### Release

```
gem push pkg/presto_sql_parser-<version>.gem
```

### Update version and dependencies

* Gem version: `VERSION` at `lib/presto_sql_parser/version.rb`
* Presto version: dependency version at `build.gradle`

