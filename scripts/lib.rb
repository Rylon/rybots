require 'yaml'

# Takes a command to execute, streaming output in realtime.
def run_command cmd
  output = []
  begin
    PTY.spawn( cmd ) do |stdout, stdin, pid|
      begin
        stdout.each { |line| output << line ; print line }
      rescue Errno::EIO
        # This probably just means that the process has finished giving output."
      end
    end
    return output
  rescue PTY::ChildExited
    puts "Error during the following command: '#{cmd}'".red
    exit 1
  end
end

def play_match team_a, team_b, maps
  # TODO: Make this handle multiple maps...
  base_result = {
    'map' => maps.first,
    'team_a' => team_a,
    'team_b' => team_b,
    'time'   => '',
    'round'  => ''
  }
  begin
    output = run_command "cd ../ ; ./gradlew runQuiet --offline -PteamA=#{team_a} -PteamB=#{team_b} -Pmaps=#{maps.join(',')}"
    winner, reason, time = parse_output(output)

    return base_result.merge({
      'winner' => winner['bot'],
      'reason' => reason,
      'time'   => time,
      'round'  => winner['round']
    })
  rescue => e
    puts e
    puts e.backtrace
    return base_result.merge({
      'winner' => 'Error',
      'reason' => e,
    })
  end
end

# Clones/pulls all repos given in the bots.yml file, and setup appropriate symlinks
# then returns a list of all robots and maps.
def setup_environment
  puts "Updating other bots...\n".bold.yellow

  pwd    = '../other_bots'
  config = YAML.load(File.open('bots.yml'))
  Dir.mkdir(pwd) unless Dir.exist?(pwd)

  config['bots'].each do |key, value|
    name         = key
    url          = value['git_url']
    symlinks     = [value['src_dir']].flatten

    puts "#{key}:"

    puts "cd #{pwd} ; git clone #{url} #{name}"

    if File.exist? "#{pwd}/#{name}"
      run_command "cd #{pwd}/#{name} ; git pull -r"
    else
      run_command "cd #{pwd} ; git clone #{url} #{name}"
    end

    symlinks.each do |symlink|
      src, dest = nil, nil
      if symlink.class == Hash
        src  = symlink['src']
        dest = symlink['dest']
      else
        src, dest = symlink
      end

      run_command "cd ../src ; ln -sfv ../other_bots/#{name}/src/#{src} #{dest}"
    end
    puts ""
  end

  # Check all the symlinks contain a RobotPlayer.java file, as some of them are supporting classes, not actual robots.
  robots = Dir["../src/*"].collect do |robot|
    File.basename(robot) if File.exist? "#{robot}/RobotPlayer.java"
  end.compact

  return robots, config['maps']

end

def parse_output output
  # We're looking for output such as the following, then using regexes to extract the right data.
  # [server]                    ben.one (A) wins (round 525)
  # [server] Reason: The winning team won by destruction.
  winner = output.select{|line| line.match(/.*wins.*/) }.first.match(/.*\s+(?<bot>\S+)\s\(\S\) wins \(round (?<round>\d+)\)/)
  reason = output.select{|line| line.match(/.*Reason:.*/) }.first.match(/\[server\] Reason: (.*)/)[1].strip
  time   = output.select{|line| line.match(/.*Total time:.*/) }.first.match(/.*\s(\d+.\d+.*)/)[1].strip

  return winner, reason, time
end

def send_to_slack results
  attachments = []

  results.each do |result|

    color = nil
    if result['team_a'] == 'rybots' or result['team_b'] == 'rybots'
      color = result['winner'] == 'rybots' ? 'good' : 'danger'
    end

    attachments << {
      fallback: "#{result['team_a']} -vs- #{result['team_b']} on #{result['map']} - #{result['winner']} wins on round #{result['round']} after #{result['time']}! #{result['reason']}",
      title:    "#{result['team_a']} -vs- #{result['team_b']} on #{result['map']}",
      text:     "Winner: #{result['winner']} on round #{result['round']} after #{result['time']}.\n#{result['reason']}",
      color:    color,
    }

  end
  SLACK.post text: "Battlecode Results #{Time.now.strftime("%Y-%m-%d @ %H:%M")}", attachments: attachments
end
