package com.soundcloud.maze.events

import java.io.{BufferedWriter, OutputStreamWriter}
import java.net.Socket

import scala.util.Try

object EventHelper {

  def writeToSocket(socket: Socket, nextPayload: String): Unit = {
    Try {
      val writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))
      writer.write(s"$nextPayload\n")
      writer.flush()
    }
  }

  def malformedInput(input: String) = {
    s"Malformed Input: $input"
  }

  def unexistingClient(clientId: Long, input: String) = {
    s"Target User Disconnected: $clientId, original message: $input"
  }
}
