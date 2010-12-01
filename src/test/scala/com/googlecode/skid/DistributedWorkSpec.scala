package com.googlecode.skid

import com.googlecode.skid.communication._
import com.googlecode.skid.serialization._

import java.io.File

import java.net.InetSocketAddress

import org.scalatest.WordSpec
import org.scalatest.matchers.ShouldMatchers

class DistributedWorkSpec extends WordSpec with ShouldMatchers {
	org.sgine.log.Log.sourceLookup = true
	
	private val serverAddress = new InetSocketAddress("localhost", 2601)
	private val clientDirectory = new File("temp/client")
	private val serverDirectory = new File("temp/server")
	
	private val server = JobManager(serverAddress, serverDirectory)
	private val client = JobDispatcher(serverAddress, clientDirectory)
	
	"Server Directory" should {
		"delete directory" when {
			if (serverDirectory.exists) {
				"exists" in {
					JobPersistence.delete(serverDirectory)
				}
			}
		}
	}
	
	"Client Directory" when {
		if (clientDirectory.exists) {
			"exists" should {
				"delete properly" in {
					JobPersistence.delete(clientDirectory)
				}
			}
		}
	}
}