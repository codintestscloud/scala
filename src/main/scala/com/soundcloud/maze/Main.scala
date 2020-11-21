package com.soundcloud.maze

import java.io._
import java.net.{ServerSocket, Socket}

import scala.collection.concurrent.TrieMap
import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.io.Source
import scala.util.{Failure, Success, Try}
import scala.collection.JavaConverters._

object Main {

  def main(args: Array[String]): Unit = {

    val clientPool = new TrieMap[Long, Socket]

    val messagesBySeqNo = new mutable.HashMap[Long, List[String]]
    val followRegistry = new mutable.HashMap[Long, Set[Long]]

    implicit val ec = ExecutionContext.global

      val events = new Events(clientPool, messagesBySeqNo, followRegistry).getEventsAsync()
      val clients = EventsOrchestration.clientsAsync(clientPool)

    Await.result(events, Duration.Inf)
    Await.result(clients, Duration.Inf)
  }
}
