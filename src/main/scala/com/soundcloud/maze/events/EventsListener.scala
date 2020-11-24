package com.soundcloud.maze.events

import java.io.{BufferedReader, InputStreamReader}
import java.net.{ServerSocket, Socket}

import main.scala.com.soundcloud.maze.events.{Follow, Unfollow}

import scala.collection.JavaConverters.asScalaIteratorConverter
import scala.collection.concurrent.TrieMap
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class EventsListener(clientPool: TrieMap[Long, Socket], messagesBySeqNo: mutable.HashMap[Long, List[String]], followRegistry: mutable.HashMap[Long, Set[Long]])(implicit ctxt: ExecutionContext) {

  private var lastSeqNo = 0L

  private val EventPort = 9090

  def getEventsAsync(): Future[Unit] = {
    Future {
      println(s"Listening for events on $EventPort")
      val eventSocket = new ServerSocket(EventPort).accept()
      Try {
        val reader = new BufferedReader(new InputStreamReader(eventSocket.getInputStream()))

        val results =
          reader
          .lines()
          .iterator()
          .asScala
            .map(sendMessage)
            .toList
            .flatten

        results.map(DeadLetter.publish)

        if (reader != null) reader.close()
        if (eventSocket != null) eventSocket.close()
      }
    }
  }

  def sendMessage(payload: String): ListBuffer[String] = {
    println(s"Message received: $payload")
    val message = payload.split("\\|").toList

    messagesBySeqNo += message(0).toLong -> message

    var errorResults = ListBuffer[String]()

    while (messagesBySeqNo.get(lastSeqNo + 1L).isDefined) {
      val nextMessage = messagesBySeqNo(lastSeqNo + 1)
      messagesBySeqNo -= lastSeqNo + 1L

      val nextPayload = nextMessage.mkString("|")
      val seqNo = nextMessage(0).toLong
      val kind = nextMessage(1)

      val executableAction: EventAction = kind match {
        case "F" => new Follow(nextMessage, followRegistry, clientPool, nextPayload)
        case "U" => new Unfollow(nextMessage, followRegistry, clientPool, nextPayload)
        case "P" => new PrivateMessage(nextMessage, followRegistry, clientPool, nextPayload)
        case "B" => new Broadcast(nextMessage, followRegistry, clientPool, nextPayload)
        case "S" => new StatusUpdate(nextMessage, followRegistry, clientPool, nextPayload)
        case unsupported => new UnsupportedKind(nextMessage, followRegistry, clientPool, nextPayload, unsupported)
      }

      executableAction.execute().left.map(error => errorResults += error)
      lastSeqNo = seqNo
    }
    errorResults
  }

}
