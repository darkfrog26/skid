package com.googlecode.skid

import com.googlecode.skid.communication._
import com.googlecode.skid.serialization._
import java.io.File

import java.net.InetSocketAddress

import org.sgine.event.Event
import org.sgine.event.EventHandler
import org.sgine.event.Recursion

import org.sgine.log._

/**
 * JobDispatcher is a client that connects remotely
 * to a centralized JobManager to send and receive
 * information about jobs.
 * 
 * @author Matt Hicks <mhicks@sgine.org>
 */
class JobDispatcher private(serverAddress: InetSocketAddress, storage: File) {
	private val client = new CommunicationClient(serverAddress, storage)
	private val persistence = JobPersistence(new File(storage, "temp"))
	
	def start() = {
		client.listeners += EventHandler(clientEvent, recursion = Recursion.Children)
		
		client.connect()
	}
	
	private def clientEvent(evt: Event) = {
//		info(evt)
	}
	
	def send(f: AnyRef, resources: JobResource*) = {
		// Persist the job for transfer
		val uuid = persistence.persist(f, resources: _*)
		
		// Transfer the job to the server
		for (file <- persistence.files(uuid)) {
			client.send(uuid, file)
		}
		
		// Send an object with a reference to the UUID to start working
		client.send(uuid, Work(uuid))
		
		// Return the UUID
		uuid
	}
}

object JobDispatcher {
	def apply(serverAddress: InetSocketAddress, storage: File) = {
		new JobDispatcher(serverAddress, storage)
	}
}