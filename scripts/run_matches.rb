#!/usr/bin/env ruby

require 'bundler' ; Bundler.require
require 'pty'
require_relative 'lib'

if ENV['SLACK_WEBHOOK_URL']
  SLACK = Slack::Notifier.new ENV['SLACK_WEBHOOK_URL'] do
    defaults channel:  '#battlecode-results',
             username: 'Battlecode',
             icon_url: 'https://cl.ly/2T3T1D46460A/cylon.png'
  end
else
  SLACK=false
end

robots, maps = setup_environment

puts "Playing matches...".bold.yellow

results = []

robots.each do |robot_a|
  robots.each do |robot_b|
    next if robot_a == robot_b
    next if robot_a == 'examplefuncsplayer'
    next if robot_b == 'examplefuncsplayer'
    puts "#{robot_a} -vs- #{robot_b}\n".yellow
    maps.each do |map|
      results << play_match(robot_a, robot_b, map)
    end
  end
end

table = Terminal::Table.new :title => "Battles", :headings => [results.first.keys]

results.each do |result|
  table.add_row(result.values)
end

puts ""
puts table

send_to_slack(results) if SLACK
