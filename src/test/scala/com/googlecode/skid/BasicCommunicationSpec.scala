package com.googlecode.skid

import com.googlecode.skid.communication._

import java.io.File

import java.net.InetSocketAddress

import java.util.UUID

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

import org.sgine.core.ProcessingMode

import org.sgine.event.EventHandler
import org.sgine.event.Recursion

import org.sgine.util.Time

class BasicCommunicationSpec extends FlatSpec with ShouldMatchers {
	private val serverAddress = new InetSocketAddress("localhost", 2600)
	private val serverDirectory = new File("temp/server")
	private val server = new CommunicationServer(serverAddress, serverDirectory)
	private val clientDirectory = new File("temp/client")
	private val client = new CommunicationClient(serverAddress, clientDirectory)
	
	private var clientConnected = false
	private var serverConnected = false
	
	private var serverNode: Communication = _
	
	private val clientUUID1 = UUID.randomUUID
	private var clientUUID1Received = false
	private var clientMessage1: Any = _
	
	private val clientUUID2 = UUID.randomUUID
	private var clientUUID2Received = false
	private var clientMessage2: Any = _
	
	private val serverUUID1 = UUID.randomUUID
	private var serverUUID1Received = false
	private var serverMessage1: Any = _
	
	private val serverUUID2 = UUID.randomUUID
	private var serverUUID2Received = false
	private var serverMessage2: Any = _
	
	private val testFile1 = new File("lib/sgine_2.8.0-1.0.jar")
	
//	org.sgine.log.Log.sourceLookup = true
	
	"Setup" should "initialize directories" in {
		serverDirectory.mkdirs()
		clientDirectory.mkdirs()
		
		serverDirectory.listFiles.foreach(_.delete)
		clientDirectory.listFiles.foreach(_.delete)
	}
	
	it should "add listeners" in {
		val evtEstablished = new java.util.concurrent.atomic.AtomicReference[ConnectionEstablished]
		val evtObject = new java.util.concurrent.atomic.AtomicReference[ObjectReceived]
		val evtFile = new java.util.concurrent.atomic.AtomicReference[FileReceived]
		
		server.onEvent[ConnectionEstablished](ProcessingMode.Blocking, Recursion.Children, container = evtEstablished) {
			serverNode = evtEstablished.get.communication
			serverConnected = true
		}
		server.onEvent[ObjectReceived](ProcessingMode.Blocking, Recursion.Children, container = evtObject) {
			val evt = evtObject.get
			if (evt.uuid == clientUUID1) {
				clientUUID1Received = true
				clientMessage1 = evt.obj
			} else {
				throw new RuntimeException("Unknown message: " + evt)
			}
		}
		server.onEvent[FileReceived](ProcessingMode.Blocking, Recursion.Children, container = evtFile) {
			val evt = evtFile.get
			if (evt.uuid == clientUUID2) {
				clientUUID2Received = true
				clientMessage2 = evt.file
			} else {
				throw new RuntimeException("Unknown message: " + evt)
			}
		}
		
		client.onEvent[ConnectionEstablished](ProcessingMode.Blocking) {
			clientConnected = true
		}
		client.onEvent[ObjectReceived](ProcessingMode.Blocking, container = evtObject) {
			val evt = evtObject.get
			if (evt.uuid == serverUUID1) {
				serverUUID1Received = true
				serverMessage1 = evt.obj
			} else {
				throw new RuntimeException("Unknown message: " + evt)
			}
		}
		client.onEvent[FileReceived](ProcessingMode.Blocking, container = evtFile) {
			val evt = evtFile.get
			if (evt.uuid == serverUUID2) {
				serverUUID2Received = true
				serverMessage2 = evt.file
			} else {
				throw new RuntimeException("Unknown message: " + evt)
			}
		}
	}
	
	"CommunicationServer" should "start" in {
		server.start()
	}
	
	"CommunicationClient" should "connect to server" in {
		client.connect()
	}
	
	it should "receive ConnectionEstablished event" in {
		Time.waitFor(5.0) {
			clientConnected
		}
		clientConnected should equal(true)
	}
	
	"CommunicationServer" should "receive ConnectionEstablished event" in {
		Time.waitFor(5.0) {
			serverConnected
		}
		serverConnected should equal(true)
	}
	
	"CommunicationClient" should "send a String object" in {
		client.send(clientUUID1, "Hello from Client")
	}
	
	"CommunicationServer" should "receive Hello from Client message" in {
		Time.waitFor(15.0) {
			clientUUID1Received
		}
		clientUUID1Received should equal(true)
		clientMessage1 should equal("Hello from Client")
	}
	
	"CommunicationClient" should "send a File object" in {
		client.send(clientUUID2, testFile1)
	}
	
	"CommunicationServer" should "receive File" in {
		Time.waitFor(15.0) {
			clientUUID2Received
		}
		clientUUID2Received should equal(true)
		clientMessage2.asInstanceOf[File].length should equal(testFile1.length)
	}
	
	"CommunicationServerNode" should "send a String object" in {
		serverNode.send(serverUUID1, "Hello from Server")
	}
	
	"CommunicationClient" should "receive Hello from Server message" in {
		Time.waitFor(15.0) {
			serverUUID1Received
		}
		serverUUID1Received should equal(true)
		serverMessage1 should equal("Hello from Server")
	}
	
	"CommunicationServerNode" should "send a File" in {
		serverNode.send(serverUUID2, testFile1)
	}
	
	"CommunicationClient" should "receive File" in {
		Time.waitFor(15.0) {
			serverUUID2Received
		}
		serverUUID2Received should equal(true)
		serverMessage2.asInstanceOf[File].length should equal(testFile1.length)
	}
	
	"CommunicationClient" should "shutdown" in {
		client.disconnect()
	}
	
	"CommunicationServer" should "shutdown" in {
		server.shutdown()
	}
}