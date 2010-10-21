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
	
	private val clientUUID1 = UUID.randomUUID
	private var clientUUID1Received = false
	private var clientMessage1: Any = _
	
//	org.sgine.log.Log.sourceLookup = true
	
	"Setup" should "initialize directories" in {
		serverDirectory.mkdirs()
		clientDirectory.mkdirs()
		
		serverDirectory.listFiles.foreach(_.delete)
		clientDirectory.listFiles.foreach(_.delete)
	}
	
	it should "add listeners" in {
		val f = (evt: ObjectReceived) => if (evt.uuid == clientUUID1) {
			clientUUID1Received = true
			clientMessage1 = evt.obj
		}
		server.listeners += EventHandler(f, ProcessingMode.Blocking, Recursion.Children)
	}
	
	"CommunicationServer" should "start" in {
		server.start()
	}
	
	"CommunicationClient" should "connect to server" in {
		client.connect()
	}
	
	it should "send a String object" in {
		client.send(clientUUID1, "Hello World")
	}
	
	"CommunicationServer" should "receive Hello World message" in {
		Time.waitFor(15.0) {
			clientUUID1Received == true
		}
		clientUUID1Received should equal(true)
		clientMessage1 should equal("Hello World")
	}
	
	"CommunicationClient" should "shutdown" in {
		client.disconnect()
	}
	
	"CommunicationServer" should "shutdown" in {
		server.shutdown()
	}
}