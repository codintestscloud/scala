package com.soundcloud.maze.events

import java.io.{BufferedWriter, OutputStreamWriter}
import java.net.Socket

object EventWriter {

  def writeToSocket(socket: Socket, nextPayload: String): Unit = {
    val writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))
    writer.write(s"$nextPayload\n")
    writer.flush()
  }

}
