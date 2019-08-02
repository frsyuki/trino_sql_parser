require "bundler/gem_tasks"
require "rspec/core/rake_task"

RSpec::Core::RakeTask.new(:spec)

task :jar do
  sh "./gradlew shadowJar"
  cp "build/libs/presto-sql-parser-1.0.0-all.jar", "lib/presto_sql_parser/presto-sql-parser.jar"
end

task :default => [:jar, :spec, :build]
