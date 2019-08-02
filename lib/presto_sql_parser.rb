require 'json'

class PrestoSqlParser
  module ClassMethods
    attr_accessor :java_cmd
    attr_accessor :java_args
    attr_accessor :java_env
    attr_accessor :jar_path
  end

  extend ClassMethods

  self.java_cmd = ENV['PRESTO_SQL_PARSER_JAVA']
  if self.java_cmd == nil || self.java_cmd.strip.empty?
    self.java_cmd = "java"
  end

  self.java_args = []

  self.java_env = {}

  self.jar_path = File.join(File.dirname(__FILE__), "presto_sql_parser/presto-sql-parser.jar")

  class ParseError < StandardError
    def initialize(errors)
      @errors = errors
      message = errors.map do |error|
        "[#{error['line']}:#{error['column']}] #{error['message']}"
      end.join(", ")
      super(message)
    end

    attr_reader :errors
  end

  require 'presto_sql_parser/support_process'

  def initialize(with_tokens: false)
    @support_process = SupportProcess.new(with_tokens: with_tokens)
  end

  def parse(sql)
    unless sql.is_a?(String)
      raise ArgumentError, "SQL must be a String but got #{sql.class}"
    end
    request_line = JSON.dump({"sql" => sql})

    success = false
    begin
      @support_process.send_line(request_line)
      response_line = @support_process.receive_line
      raise "Process crashed" unless response_line
      response = JSON.parse(response_line)
      statements = response['statements']
      errors = response['errors']
      success = true
    rescue => e
      raise "Support process failed with an error: #{e}"
    ensure
      @support_process.kill! unless success
    end

    if errors
      raise ParseError.new(errors)
    end

    statements.map do |h|
      h.reject! {|k, v| v == nil }
    end

    statements
  end
end
