package com.googlecode.skid

import com.googlecode.skid.communication._

import java.io.File

import java.net.InetSocketAddress

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

class BasicCommunicationSpec extends FlatSpec with ShouldMatchers {
	private val serverAddress = new InetSocketAddress("localhost", 2600)
	private val serverDirectory = new File("temp/server")
	private val server = new CommunicationServer(serverAddress, serverDirectory)
	private val clientDirectory = new File("temp/client")
	private val client = new CommunicationClient(serverAddress, clientDirectory)
	
	"Setup" should "initialize directories" in {
		serverDirectory.mkdirs()
		clientDirectory.mkdirs()
		
		serverDirectory.listFiles.foreach(_.delete)
		clientDirectory.listFiles.foreach(_.delete)
	}
	
	"CommunicationServer" should "start" in {
		server.start()
	}
	
	"CommunicationClient" should "connect to server" in {
		client.connect()
	}
	
	it should "shutdown" in {
		client.disconnect()
	}
	
	"CommunicationServer" should "shutdown" in {
		server.shutdown()
	}
}