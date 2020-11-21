package com.soundcloud.maze

import java.io.{BufferedReader, BufferedWriter, InputStreamReader, OutputStreamWriter}
import java.net.{ServerSocket, Socket}

import scala.collection.JavaConverters.asScalaIteratorConverter
import scala.collection.concurrent.TrieMap
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source
import scala.util.Try

object EventsOrchestration {
  private val ClientPort = 9099

  def clientsAsync(clientPool: TrieMap[Long, Socket]) (implicit ctxt: ExecutionContext): Future[Unit] = {
    Future {
      println(s"Listening for client requests on $ClientPort")
      val serverSocket = new ServerSocket(ClientPort)
      var maybeClientSocket = Option(serverSocket.accept())

      while (maybeClientSocket.nonEmpty) {
        maybeClientSocket.foreach { clientSocket =>
          val bufferedSource = Source.fromInputStream(clientSocket.getInputStream())
          val userId = bufferedSource.bufferedReader().readLine()

          if (userId != null) {
            clientPool.put(userId.toLong, clientSocket)
            println(s"User connected: $userId (${clientPool.size} total)")
          }

          maybeClientSocket = Option(serverSocket.accept())
        }
      }
    }
  }
}
