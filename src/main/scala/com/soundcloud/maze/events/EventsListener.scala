package com.soundcloud.maze.events

import java.io.{BufferedReader, InputStreamReader}
import java.net.{ServerSocket, Socket}

import main.scala.com.soundcloud.maze.events.{Follow, Unfollow}

import scala.collection.JavaConverters.asScalaIteratorConverter
import scala.collection.concurrent.TrieMap
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class EventsListener(clientPool: TrieMap[Long, Socket], messagesBySeqNo: mutable.HashMap[Long, List[String]], followRegistry: mutable.HashMap[Long, Set[Long]])(implicit ctxt: ExecutionContext) {

  private var lastSeqNo = 0L

  private val EventPort = 9090

  def getEventsAsync(): Future[Unit] = {
    Future {
      println(s"Listening for events on $EventPort")
      val eventSocket = new ServerSocket(EventPort).accept()

      val exec = Try {
        val reader = new BufferedReader(new InputStreamReader(eventSocket.getInputStream()))

        reader.lines().iterator().asScala.foreach(sendMessage)

        if (reader != null) reader.close()
      }

      exec match {
        case Success(value) => println(s"Success event listening execution! value: $value")
        case Failure(value) => println(s"Failed event listening execution! value: $value")
      }
      if (eventSocket != null) eventSocket.close()
    }
  }

  def sendMessage(payload: String) = {
    println(s"Message received: $payload")
    val message = payload.split("\\|").toList

    messagesBySeqNo += message(0).toLong -> message

    while (messagesBySeqNo.get(lastSeqNo + 1L).isDefined) {
      val nextMessage = messagesBySeqNo(lastSeqNo + 1)
      messagesBySeqNo -= lastSeqNo + 1L

      val nextPayload = nextMessage.mkString("|")
      val seqNo = nextMessage(0).toLong
      val kind = nextMessage(1)

      val executableAction: EventAction = kind match {
        case "F" => Follow(nextMessage, followRegistry, clientPool, nextPayload)
        case "U" => Unfollow(nextMessage, followRegistry, clientPool, nextPayload)
        case "P" => PrivateMessage(nextMessage, followRegistry, clientPool, nextPayload)
        case "B" => Broadcast(nextMessage, followRegistry, clientPool, nextPayload)
        case "S" => StatusUpdate(nextMessage, followRegistry, clientPool, nextPayload)
      }

      lastSeqNo = seqNo

      executableAction.execute()

    }
  }

}
