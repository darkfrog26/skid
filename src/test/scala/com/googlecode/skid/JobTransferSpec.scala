package com.googlecode.skid

import com.googlecode.skid.communication._
import com.googlecode.skid.serialization._

import java.io.File

import java.net.InetSocketAddress

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

import org.sgine.util.Time

class JobTransferSpec extends FlatSpec with ShouldMatchers {
	org.sgine.log.Log.sourceLookup = true
	
	private val serverAddress = new InetSocketAddress("localhost", 2601)
	private val clientDirectory = new File("temp/client")
	private val serverDirectory = new File("temp/server")
	
	private val server = JobManager(serverAddress, serverDirectory)
	private val client = JobDispatcher(serverAddress, clientDirectory)
	
	"Setup" should "initialize directories" in {
		JobPersistence.delete(serverDirectory)
		JobPersistence.delete(clientDirectory)
		
		serverDirectory.mkdirs()
		clientDirectory.mkdirs()
	}
	
	"Server" should "start successfully" in {
		server.start()
	}
	
	"Client" should "start successfully" in {
		client.start()
	}
	
	it should "create a job and send it to the JobDispatcher" in {
		val testFunction = () => println("Simple test function!")
		client.send(testFunction, JobResource(testFunction.getClass, distribute = true))
	}
	
	"Server" should "find persisted Work in database" in {
		var work: Work = null
		Time.waitFor(10.0) {
			server.requestWork() match {
				case Some(w) => work = w; true
				case None => false
			}
		}
		work should not equal(null)
		println("Found Work in Server!")
	}
}