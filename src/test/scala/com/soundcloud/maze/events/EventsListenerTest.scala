package com.soundcloud.maze.events

import java.net.{ServerSocket, Socket, SocketAddress}

import org.scalatest.{BeforeAndAfterEach, FunSuite}

import scala.collection.concurrent.TrieMap
import scala.collection.mutable
import scala.concurrent.{Await, ExecutionContext, Future}

class EventsListenerTest extends FunSuite with BeforeAndAfterEach {

  implicit var ec: ExecutionContext = null

  var clientPool: TrieMap[Long, Socket] = null

  var messagesBySeqNo: mutable.HashMap[Long, List[String]] = null
  var followRegistry: mutable.HashMap[Long, Set[Long]] = null

  var eventsListener: EventsListener = null

  override def beforeEach() {
    ec = ExecutionContext.global
    clientPool = new TrieMap[Long, Socket]
    messagesBySeqNo = new mutable.HashMap[Long, List[String]]
    followRegistry = new mutable.HashMap[Long, Set[Long]]
    eventsListener = new EventsListener(clientPool, messagesBySeqNo, followRegistry)
  }

  test("correct input is parsed and return Unit with users connected") {

    val socket = new Socket()
    clientPool.put(1L, socket)

    val msgSend = eventsListener.sendMessage("1|F|17|1")
    val msgSend1 = eventsListener.sendMessage("2|S|16")
    val msgSend2 = eventsListener.sendMessage("3|U|17|30")
    val msgSend3 = eventsListener.sendMessage("4|P|23|1")
    val msgSend4 = eventsListener.sendMessage("5|B|23")

    assert(msgSend == List())
    assert(msgSend1 == List())
    assert(msgSend2 == List())
    assert(msgSend3 == List())
    assert(msgSend4 == List())
  }

  test("Incorrect input for Follow return malformed input string") {
    val message = "1|F"

    val msgSend = eventsListener.sendMessage(message)

    assert(msgSend.toList == List("Malformed Input: 1|F"))
  }

  test("Incorrect input for Status Update return malformed input string") {
    val message = "1|S"

    val msgSend = eventsListener.sendMessage(message)

    assert(msgSend.toList == List("Malformed Input: 1|S"))
  }

  test("Incorrect input for Unfollow return malformed input string") {
    val message = "1|U"

    val msgSend = eventsListener.sendMessage(message)

    assert(msgSend.toList == List("Malformed Input: 1|U"))
  }

  test("Incorrect input for PrivateMessage return malformed input string") {
    val message = "1|P"

    val msgSend = eventsListener.sendMessage(message)

    assert(msgSend.toList == List("Malformed Input: 1|P"))
  }

  test("Unsupported kind return Unsupported kind string") {
    val message = "1|A"

    val msgSend = eventsListener.sendMessage(message)

    assert(msgSend.toList == List("Unsupported type: A, original message: List(1, A)"))
  }

  test("Target user not connected Private Message") {
    val message = "1|P|23|18"

    val msgSend = eventsListener.sendMessage(message)

    assert(msgSend.toList == List("Target User Disconnected: 18, original message: 1|P|23|18"))
  }
  test("Target user not connected Follow") {
    val message = "1|F|23|18"

    val msgSend = eventsListener.sendMessage(message)

    assert(msgSend.toList == List("Target User Disconnected: 18, original message: 1|F|23|18"))
  }

}
