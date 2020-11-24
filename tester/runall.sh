#!/bin/bash -ex

this_dir=$(dirname $0)
export concurrencyLevel=3
time java -server -Xmx1G -jar "${this_dir}/follower-maze-2.0.jar"
