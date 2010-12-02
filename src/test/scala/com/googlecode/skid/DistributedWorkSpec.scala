package com.googlecode.skid

import com.googlecode.skid.communication._
import com.googlecode.skid.serialization._

import java.io.File

import java.net.InetSocketAddress

import org.sgine.util.Time

import org.specs._

object DistributedWorkSpec extends Specification {
	org.sgine.log.Log.sourceLookup = true
	
	private val serverAddress = new InetSocketAddress("localhost", 2602)
	private val clientDirectory = new File("temp/client")
	private val serverDirectory = new File("temp/server")
	
	private val server = JobManager(serverAddress, serverDirectory)
	private val client = JobDispatcher(serverAddress, clientDirectory)
	
	org.specs.util.Configuration.config = new org.specs.util.Configuration {
		override def examplesWithoutExpectationsMustBePending = false
	}
	
	shareVariables()
	
	"Server Directory" should {
		"be deleted" in {
			if (serverDirectory.exists) JobPersistence.delete(serverDirectory)
			serverDirectory.exists must_== false
		}
		"be created" in {
			serverDirectory.mkdirs()
			serverDirectory.exists must_== true
		}
	}
	
	"Client Directory" should {
		"be deleted" in {
			if (clientDirectory.exists) JobPersistence.delete(clientDirectory)
			clientDirectory.exists must_== false
		}
		"be created" in {
			clientDirectory.mkdirs()
			clientDirectory.exists must_== true
		}
	}
	
	"JobManager" should {
		"start" in {
			println("Server: " + server.toString)
			server.start()
		}
	}
	
	"JobDispatcher" should {
		"start" in {
			client.start()
		}
		
		"dispatch function to server" in {
			val f = () => println("Hello from client!")
			client.send(f, JobResource(f.getClass, distribute = true))
		}
	}
	
	"JobManager" should {
		"find one pending job" in {
			println("Server: " + server.toString)
			Time.waitFor(10.0) {
				server.work == 1
			}
			server.work must_== 1
		}
	}
	
	"JobDispatcher" should {
		"shutdown" in {
			client.shutdown()
		}
	}
	
	"JobManager" should {
		"shutdown" in {
			server.shutdown()
		}
	}
}