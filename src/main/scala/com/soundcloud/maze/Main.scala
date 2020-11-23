package com.soundcloud.maze

import java.net.Socket
import java.util.concurrent.Executors

import com.soundcloud.maze.clients.ClientsListener
import com.soundcloud.maze.events.EventsListener

import scala.collection.concurrent.TrieMap
import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

object Main {

  val eventsThreadPoolSize = 2
  val clientsThreadPoolSize = 2

  def main(args: Array[String]): Unit = {

    val clientPool = new TrieMap[Long, Socket]

    val messagesBySeqNo = new mutable.HashMap[Long, List[String]]
    val followRegistry = new mutable.HashMap[Long, Set[Long]]

    val eventsEc = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(eventsThreadPoolSize))
    val clientsEc = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(clientsThreadPoolSize))

    val events = new EventsListener(clientPool, messagesBySeqNo, followRegistry)(eventsEc).getEventsAsync()
    val clients = new ClientsListener(clientPool)(clientsEc).getClientsAsync()

    Await.ready(events, Duration.Inf)
    Await.result(clients, Duration.Inf)
  }
}
