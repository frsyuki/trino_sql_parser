require 'shellwords'

class TrinoSqlParser
  class SupportProcess
    def initialize(idle_wait: 2, with_tokens:)
      @idle_wait = idle_wait
      @with_tokens = with_tokens
      @mutex = Mutex.new
      @last_used_pid = nil
      @pipe = nil
      @pid = nil
    end

    def start!
      return if @pipe

      cmd = (
        [TrinoSqlParser.java_cmd] +
        TrinoSqlParser.java_args.map {|arg| Shellwords.escape(arg) } +
        ["-jar", Shellwords.escape(TrinoSqlParser.jar_path)]
      ).join(' ')

      if @with_tokens
        cmd << " --with-tokens"
      end

      @pipe = IO.popen(TrinoSqlParser.java_env, cmd, "r+", external_encoding: 'UTF-8')
      @pid = @pipe.pid
      Thread.new(@pid, &method(:monitor_thread))
    end

    def kill!
      @mutex.synchronize do
        if @pid
          Process.kill("KILL", @pid)
          @pipe.close rescue nil
          @pipe = nil
          @pid = nil
        end
      end
    end

    def send_and_receive_line(line)
      @mutex.synchronize do  # block kill! during execution
        start! unless @pipe
        @pipe.puts line
        @last_used_pid = @pipe.pid
        @pipe.gets
      end
    end

    private

    def monitor_thread(pid)
      while true
        done = Process.waitpid2(pid, Process::WNOHANG) rescue true
        break if done

        sleep @idle_wait
        if @last_used_pid != pid
          kill! rescue nil
        end

        @last_used_pid = nil  # Next check kills the process
      end
    end
  end
end
