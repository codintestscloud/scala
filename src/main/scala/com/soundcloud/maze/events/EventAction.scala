package com.soundcloud.maze.events

import java.io.{BufferedWriter, OutputStreamWriter}
import java.net.Socket

import scala.collection.concurrent.TrieMap
import scala.collection.mutable

trait EventAction {
  val clientPool: TrieMap[Long, Socket]
  val nextPayload: String
  val nextMessage: List[String]
  val followRegistry: mutable.HashMap[Long, Set[Long]]

  def execute(): Unit

  lazy val success = Right("Success")

}

case class PrivateMessage(override val nextMessage: List[String],
                          override val followRegistry: mutable.HashMap[Long, Set[Long]],
                          override val clientPool: TrieMap[Long, Socket],
                          override val nextPayload: String) extends EventAction {
  val toUserId = nextMessage(3).toLong

  def execute() {
    clientPool.get(toUserId).foreach ( socket => EventWriter.writeToSocket(socket, nextPayload))
  }
}

case class Broadcast(override val nextMessage: List[String],
                     override val followRegistry: mutable.HashMap[Long, Set[Long]],
                     override val clientPool: TrieMap[Long, Socket],
                     override val nextPayload: String) extends EventAction {

  def execute() {
    clientPool.values.foreach (socket => EventWriter.writeToSocket(socket, nextPayload))
  }
}

case class StatusUpdate(override val nextMessage: List[String],
                        override val followRegistry: mutable.HashMap[Long, Set[Long]],
                        override val clientPool: TrieMap[Long, Socket],
                        override val nextPayload: String) extends EventAction {
  val fromUserId = nextMessage(2).toLong
  val followers = followRegistry.getOrElse(fromUserId, Set.empty)

  def execute() {
    followers.foreach { follower =>
      clientPool.get(follower).foreach (socket => EventWriter.writeToSocket(socket, nextPayload))
    }
  }

}