package com.soundcloud.maze.events

object DeadLetter {
  def publish(error: String) = {
    println(error)
  }
}
