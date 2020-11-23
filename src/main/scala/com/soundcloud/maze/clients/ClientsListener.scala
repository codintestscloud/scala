package com.soundcloud.maze.clients

import java.net.{ServerSocket, Socket}

import scala.collection.concurrent.TrieMap
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source

class ClientsListener(clientPool: TrieMap[Long, Socket])(implicit ctxt: ExecutionContext) {
  private val ClientPort = 9099

  def getClientsAsync(): Future[Unit] = {
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
