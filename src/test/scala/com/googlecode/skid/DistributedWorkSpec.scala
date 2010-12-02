package com.googlecode.skid

import com.googlecode.skid.communication._
import com.googlecode.skid.serialization._

import java.io.File

import java.net.InetSocketAddress

import java.util.UUID

import org.sgine.util.Time

import org.specs._

object DistributedWorkSpec extends Specification {
	org.sgine.log.Log.sourceLookup = true
	
	private val serverAddress = new InetSocketAddress("localhost", 2602)
	private val clientDirectory = new File("temp/client")
	private val serverDirectory = new File("temp/server")
	private val worker1Directory = new File("temp/worker1")
	
	private val server = JobManager(serverAddress, serverDirectory)
	private val client = JobDispatcher(serverAddress, clientDirectory)
	
	private val worker1 = JobWorker(serverAddress, worker1Directory)
	
	private var workId: UUID = _
	
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
	
	"JobDispatcher" should {
		"start" in {
			client.start()
		}
		
		"dispatch function to server" in {
			val f = () => "Hello from client!"
			workId = client.send(f, JobResource(f.getClass, distribute = true))
		}
	}
	
	"JobManager" should {
		"find one pending job" in {
			Time.waitFor(10.0) {
				server.work == 1
			}
			server.work must_== 1
		}
	}
	
	"Worker1" should {
		var work: Work = null
		var response: Any = null
		
		"start" in {
			worker1.start()
		}
		
		"get a job from server" in {
			val option = worker1.requestWork()
			work = option.get
			work.uuid must_== workId
		}
		
		"process the job" in {
			response = worker1.processWork(work)
			response must_== "Hello from client!"
		}
		
		"respond with response from job to server" in {
			worker1.finishedWork(work, response)
		}
	}
	
	"JobManager" should {
		"have no pending jobs" in {
			server.work must_== 0
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