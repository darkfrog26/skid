package com.googlecode.skid

import com.googlecode.skid.communication._
import com.googlecode.skid.serialization._

import java.io.File

import java.net.InetSocketAddress

import java.util.UUID

import org.sgine.event._

import org.sgine.util.Time

import org.specs._

class WorkAbstractionSpec extends Specification {
	org.sgine.log.Log.sourceLookup = true
	
	org.specs.util.Configuration.config = new org.specs.util.Configuration {
		override def examplesWithoutExpectationsMustBePending = false
	}
	
	shareVariables()
	
	private val serverAddress = new InetSocketAddress("localhost", 2603)
	private val clientDirectory = new File("temp/client")
	private val serverDirectory = new File("temp/server")
	private val worker1Directory = new File("temp/worker1")
	
	private val server = JobManager(serverAddress, serverDirectory)
	private val client = JobDispatcher(serverAddress, clientDirectory)
	
	private val worker1 = JobWorker(serverAddress, worker1Directory)

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
	
	"Worker1 Directory" should {
		"be deleted" in {
			if (worker1Directory.exists) JobPersistence.delete(worker1Directory)
			worker1Directory.exists must_== false
		}
		"be created" in {
			worker1Directory.mkdirs()
			worker1Directory.exists must_== true
		}
	}
	
	"JobManager" should {
		"start" in {
			server.start()
		}
	}
	
	"Worker1" should {
		"start" in {
			worker1.start()
		}
	}
	
	"JobDispatcher" should {
		"start" in {
			client.start()
		}
		
		"invoke work remotely" in {
			val s = client {
				"Hello World!"
			}.getOrElse(null)
			s must_== "Hello World!"
		}
	}
	
	"Worker1" should {
		"shutdown" in {
			worker1.shutdown()
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