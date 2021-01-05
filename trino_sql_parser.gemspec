# encoding: utf-8
$:.push File.expand_path('../lib', __FILE__)
require "trino_sql_parser/version"

Gem::Specification.new do |gem|
  gem.name          = "trino_sql_parser"
  gem.summary       = "Trino SQL Parser for Ruby"
  gem.description   = "Trino SQL Parser for Ruby parses a SQL using Trino's native SQL parser precisely and reports error if given SQL has syntax errors. Optionally, it returns a ANTLR tokens sequence."
  gem.version       = TrinoSqlParser::VERSION
  gem.authors       = ["Sadayuki Furuhashi"]
  gem.email         = ["frsyuki@gmail.com"]
  gem.homepage      = "https://github.com/frsyuki/trino_sql_parser"
  gem.license       = "MIT"
  gem.files         = `git ls-files`.split("\n") + ["lib/trino_sql_parser/trino-sql-parser.jar"]
  gem.test_files    = `git ls-files -- {test,spec,features}/*`.split("\n")
  gem.executables   = `git ls-files -- bin/*`.split("\n").map{ |f| File.basename(f) }
  gem.require_paths = ["lib"]

  gem.add_development_dependency "bundler"
  gem.add_development_dependency "rake", "~> 13"
  gem.add_development_dependency "rspec", "~> 3"
end
