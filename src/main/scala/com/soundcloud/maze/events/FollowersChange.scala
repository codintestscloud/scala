package main.scala.com.soundcloud.maze.events

import java.net.Socket

import com.soundcloud.maze.events.{EventAction, EventHelper}

import scala.collection.concurrent.TrieMap
import scala.collection.mutable
import scala.util.{Failure, Success, Try}

trait FollowersChange extends EventAction {
  val tryFromUserId = Try {
    nextMessage(2).toLong
  }
  val tryToUserId = Try {
    nextMessage(3).toLong
  }
  val tryFollowers = tryToUserId.map(toUserId => {
    followRegistry.getOrElse(toUserId, Set.empty)
  })

  def putNewFollowers(toUserId: Long, newFollowers: Set[Long]): Unit = {
    followRegistry.put(toUserId, newFollowers)
  }
}

class Follow(override val nextMessage: List[String],
             override val followRegistry: mutable.HashMap[Long, Set[Long]],
             override val clientPool: TrieMap[Long, Socket],
             override val nextPayload: String) extends FollowersChange {
  override def execute(): Either[String, Unit] = {
    val tryResolution = for {
      followers <- tryFollowers
      fromUserId <- tryFromUserId
      toUserId <- tryToUserId
    } yield (toUserId, followers + fromUserId)

    tryResolution match {
      case Failure(_) => Left(EventHelper.malformedInput(nextPayload))
      case Success((toUserId, newFollowers)) => {
        putNewFollowers(toUserId, newFollowers)
        clientPool.get(toUserId) match {
          case Some(socket) => Right(EventHelper.writeToSocket(socket, nextPayload))
          case None => Left(EventHelper.unexistingClient(toUserId, nextPayload))
        }
      }
    }
  }
}

class Unfollow(override val nextMessage: List[String],
               override val followRegistry: mutable.HashMap[Long, Set[Long]],
               override val clientPool: TrieMap[Long, Socket],
               override val nextPayload: String) extends FollowersChange {
  override def execute(): Either[String, Unit] = {
        val tryResolution = for {
          followers <- tryFollowers
          fromUserId <- tryFromUserId
          toUserId <- tryToUserId
        } yield (toUserId, followers - fromUserId)

        tryResolution match {
          case Failure(_) => Left(EventHelper.malformedInput(nextPayload))
          case Success((toUserId, newFollowers)) => {
            Right(putNewFollowers(toUserId, newFollowers))
          }
        }
  }
}
