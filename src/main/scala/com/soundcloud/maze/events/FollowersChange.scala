package main.scala.com.soundcloud.maze.events

import java.net.Socket

import com.soundcloud.maze.events.{EventAction, EventWriter}

import scala.collection.concurrent.TrieMap
import scala.collection.mutable

trait FollowersChange extends EventAction {
  val fromUserId = nextMessage(2).toLong
  val toUserId = nextMessage(3).toLong
  val followers = followRegistry.getOrElse(toUserId, Set.empty)
  val newFollowers: Set[Long]

  def putNewFollowers(): Unit = {
    followRegistry.put(toUserId, newFollowers)
  }
}

case class Follow(override val nextMessage: List[String],
                  override val followRegistry: mutable.HashMap[Long, Set[Long]],
                  override val clientPool: TrieMap[Long, Socket],
                  override val nextPayload: String) extends FollowersChange {
  val newFollowers = followers + fromUserId

  override def execute() = {
    putNewFollowers()

    clientPool.get(toUserId).foreach(socket => EventWriter.writeToSocket(socket, nextPayload))
  }
}

case class Unfollow(override val nextMessage: List[String],
                    override val followRegistry: mutable.HashMap[Long, Set[Long]],
                    override val clientPool: TrieMap[Long, Socket],
                    override val nextPayload: String) extends FollowersChange {
  val newFollowers = followers - fromUserId

  override def execute() = {
    putNewFollowers()
  }
}
