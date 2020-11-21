package com.soundcloud.maze

import java.io.{BufferedReader, BufferedWriter, InputStreamReader, OutputStreamWriter}
import java.net.{ServerSocket, Socket}

import scala.collection.JavaConverters.asScalaIteratorConverter
import scala.collection.concurrent.TrieMap
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class Events(clientPool: TrieMap[Long, Socket], messagesBySeqNo: mutable.HashMap[Long, List[String]], followRegistry: mutable.HashMap[Long, Set[Long]])(implicit ctxt: ExecutionContext){

  private var lastSeqNo = 0L

  private val EventPort = 9090

  def getEventsAsync(): Future[Unit] = {
    Future {
      println(s"Listening for events on $EventPort")
      val eventSocket = new ServerSocket(EventPort).accept()

      Try {
        val reader = new BufferedReader(new InputStreamReader(eventSocket.getInputStream()))

        Try {
          reader.lines().iterator().asScala.foreach { payload =>
            println(s"Message received: $payload")
            val message = payload.split("\\|").toList

            messagesBySeqNo += message(0).toLong -> message

            while (messagesBySeqNo.get(lastSeqNo + 1L).isDefined) {
              val nextMessage = messagesBySeqNo(lastSeqNo + 1)

              messagesBySeqNo -= lastSeqNo + 1L

              val nextPayload = nextMessage.mkString("|")
              val seqNo = nextMessage(0).toLong
              val kind = nextMessage(1)

              kind match {
                case "F" =>
                  val fromUserId = nextMessage(2).toLong
                  val toUserId = nextMessage(3).toLong
                  val followers = followRegistry.getOrElse(toUserId, Set.empty)
                  val newFollowers = followers + fromUserId

                  followRegistry.put(toUserId, newFollowers)

                  writeAction(clientPool, nextPayload, Option(toUserId))

                case "U" =>
                  val fromUserId = nextMessage(2).toLong
                  val toUserId = nextMessage(3).toLong
                  val followers = followRegistry.getOrElse(toUserId, Set.empty)
                  val newFollowers = followers - fromUserId

                  followRegistry.put(toUserId, newFollowers)

                case "P" =>
                  val toUserId = nextMessage(3).toLong

                  writeAction(clientPool, nextPayload, Option(toUserId))

                case "B" =>
                  writeAction(clientPool, nextPayload, None)

                case "S" =>
                  val fromUserId = nextMessage(2).toLong
                  val followers = followRegistry.getOrElse(fromUserId, Set.empty)

                  followers.foreach { follower =>
                    writeAction(clientPool, nextPayload, Option(follower))
                  }
              }

              lastSeqNo = seqNo
            }
          }
        }
        if (reader != null) reader.close()
      }
      if (eventSocket != null) eventSocket.close()
    }
  }

  private def writeAction(clientPool: TrieMap[Long, Socket], nextPayload: String, toUserId: Option[Long]): Unit = {
    val sockets = toUserId match {
      case None => clientPool.values
      case Some(userId) => clientPool.get(userId).toIterable
    }

    sockets.foreach { socket =>
      val writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))
      writer.write(s"$nextPayload\n")
      writer.flush()
    }
  }

}
