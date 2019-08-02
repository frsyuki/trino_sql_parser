require 'shellwords'

class PrestoSqlParser
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
        [PrestoSqlParser.java_cmd] +
        PrestoSqlParser.java_args.map {|arg| Shellwords.escape(arg) } +
        ["-jar", Shellwords.escape(PrestoSqlParser.jar_path)]
      ).join(' ')

      if @with_tokens
        cmd << " --with-tokens"
      end

      @pipe = IO.popen(PrestoSqlParser.java_env, cmd, "r+", external_encoding: 'UTF-8')
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

    def send_line(line)
      pipe = @mutex.synchronize do
        start! unless @pipe
        @pipe
      end
      pipe.puts line
      @last_used_pid = pipe.pid
      nil
    end

    def receive_line
      @pipe.gets
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
