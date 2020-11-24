package com.soundcloud.maze.events

import java.net.Socket

import scala.collection.concurrent.TrieMap
import scala.collection.mutable
import scala.util.{Failure, Success, Try}

trait EventAction {
  val clientPool: TrieMap[Long, Socket]
  val nextPayload: String
  val nextMessage: List[String]
  val followRegistry: mutable.HashMap[Long, Set[Long]]

  def execute(): Either[String, Unit]
}

class PrivateMessage(override val nextMessage: List[String],
                     override val followRegistry: mutable.HashMap[Long, Set[Long]],
                     override val clientPool: TrieMap[Long, Socket],
                     override val nextPayload: String) extends EventAction {
  override def execute(): Either[String, Unit] = {
    val parseUserId = Try {
      nextMessage(3).toLong
    }

    parseUserId match {
      case Success(toUserId) => clientPool.get(toUserId) match {
        case Some(socket) => Right(EventHelper.writeToSocket(socket, nextPayload))
        case None =>Left(EventHelper.unexistingClient(toUserId, nextPayload))
      }
      case Failure(_) => Left(EventHelper.malformedInput(nextPayload))
    }
  }
}

class Broadcast(override val nextMessage: List[String],
                override val followRegistry: mutable.HashMap[Long, Set[Long]],
                override val clientPool: TrieMap[Long, Socket],
                override val nextPayload: String) extends EventAction {

  override def execute(): Either[String, Unit] = {
    Right(clientPool.values.foreach(socket => EventHelper.writeToSocket(socket, nextPayload)))
  }
}

class StatusUpdate(override val nextMessage: List[String],
                   override val followRegistry: mutable.HashMap[Long, Set[Long]],
                   override val clientPool: TrieMap[Long, Socket],
                   override val nextPayload: String) extends EventAction {
  override def execute(): Either[String, Unit] = {
    val parseUserId = Try {
      nextMessage(2).toLong
    }

    parseUserId match {
      case Success(fromUserId) => {
        val followers = followRegistry.getOrElse(fromUserId, Set.empty)
        Right(followers.foreach { follower =>
          clientPool.get(follower).foreach(socket => EventHelper.writeToSocket(socket, nextPayload))
        })
      }
      case Failure(_) => Left(EventHelper.malformedInput(nextPayload))
    }
  }
}

class UnsupportedKind(override val nextMessage: List[String],
                      override val followRegistry: mutable.HashMap[Long, Set[Long]],
                      override val clientPool: TrieMap[Long, Socket],
                      override val nextPayload: String,
                      val kind: String) extends EventAction {

  def execute() = {
    Left(s"Unsupported type: $kind, original message: $nextMessage")
  }

}