package com.googlecode.skid

import com.googlecode.skid.communication._
import com.googlecode.skid.serialization._

import java.io.File

import java.net.InetSocketAddress

import java.util.UUID

import org.sgine.event._

import org.sgine.util.Time

import org.scalatest.WordSpec
import org.scalatest.matchers.ShouldMatchers

class WorkAbstractionSpec extends WordSpec with ShouldMatchers {
	org.sgine.log.Log.sourceLookup = true
	
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
			serverDirectory.exists should equal(false)
		}
		"be created" in {
			serverDirectory.mkdirs()
			serverDirectory.exists should equal(true)
		}
	}
	
	"Client Directory" should {
		"be deleted" in {
			if (clientDirectory.exists) JobPersistence.delete(clientDirectory)
			clientDirectory.exists should equal(false)
		}
		"be created" in {
			clientDirectory.mkdirs()
			clientDirectory.exists should equal(true)
		}
	}
	
	"Worker1 Directory" should {
		"be deleted" in {
			if (worker1Directory.exists) JobPersistence.delete(worker1Directory)
			worker1Directory.exists should equal(false)
		}
		"be created" in {
			worker1Directory.mkdirs()
			worker1Directory.exists should equal(true)
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
		
		"invoke work remotely returning a String" in {
			val s = client {
				"Hello World!"
			}.getOrElse(null)
			s should equal("Hello World!")
		}
		
		"invoke work remotely returning nothing" in {
			val o = client {
				println("Done something!")
			}
			o should not equal(None)
		}
		
		"invoke work remotely throwing exception" in {
			evaluating {
				client {
					throw new RuntimeException("Blargh!")
				}
			} should produce [RuntimeException]
		}
		
		// TODO: reference a library
		
		// TODO: reference a capability
		
		// TODO: schedule something for the future
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