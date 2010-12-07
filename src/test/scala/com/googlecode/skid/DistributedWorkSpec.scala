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

object DistributedWorkSpec extends WordSpec with ShouldMatchers {
	org.sgine.log.Log.sourceLookup = true
	
	private val serverAddress = new InetSocketAddress("localhost", 2602)
	private val clientDirectory = new File("temp/client")
	private val serverDirectory = new File("temp/server")
	private val worker1Directory = new File("temp/worker1")
	
	private val server = JobManager(serverAddress, serverDirectory)
	private val client = JobDispatcher(serverAddress, clientDirectory)
	
	private val worker1 = JobWorker(serverAddress, worker1Directory)
	
	private var workId: UUID = _
	private var workFinished = false
	
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
	
	"JobDispatcher" should {
		"start" in {
			client.start()
		}
		
		"listen for WorkFinished" in {
			client.onEvent[ObjectReceived](recursion = Recursion.Children, filter = (evt: ObjectReceived) => evt.obj match {
				case wf: WorkFinished => wf.work.uuid == workId
				case _ => false
			}) {
				workFinished = true
			}
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
			server.work should equal(1)
		}
	}
	
	"Worker1" should {
		var work: Work = null
		var response: Any = null
		
		"start" in {
			worker1.start(false)		// Don't auto-find new work
		}
		
		"get a job from server" in {
			val option = worker1.requestWork()
			work = option.get
			work.uuid should equal(workId)
		}
		
		"process the job" in {
			response = worker1.processWork(work)
			response should equal("Hello from client!")
		}
		
		"respond with response from job to server" in {
			worker1.finishedWork(work, response)
		}
	}
	
	"JobManager" should {
		"have no pending jobs" in {
			Time.waitFor(10.0) {
				server.work == 0
			}
			server.work should equal(0)
		}
	}
	
	"JobDispatch" should {
		"receive the WorkFinished message" in {
			Time.waitFor(10.0) {
				workFinished
			} should equal(true)
		}
		"request the response" in {
			val wr = client.status(workId).get
			wr.work.uuid should equal(workId)
			wr.status should equal(Status.Success)
			wr.response should equal("Hello from client!")
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