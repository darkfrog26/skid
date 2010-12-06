package com.googlecode.skid

import com.googlecode.skid.communication._
import com.googlecode.skid.serialization._

import java.io.File

import java.net.InetSocketAddress

import java.util.UUID

import org.sgine.event._

class JobWorker private(serverAddress: InetSocketAddress, storage: File) extends Listenable {
	private val client = new CommunicationClient(this, serverAddress, storage)
	private val persistence = JobPersistence(storage)
	
	def start() = {
		client.connect()
	}
	
	def shutdown() = {
		client.disconnect()
	}
	
	protected[skid] def requestWork(timeout: Double = 10.0): Option[Work] = {
		// Send request to server for work
		val requestId = UUID.randomUUID
		client.send(requestId, WorkRequest())
		
		// Wait for response
		val or = client.waitForEvent[ObjectReceived](timeout, filter = (or: ObjectReceived) => or.uuid == requestId)
		or.obj match {
			case null => None
			case work: Work => Some(work)
		}
	}
	
	protected[skid] def processWork(work: Work) = {
		// Load work from system
		val job = persistence.load(work.uuid)
		Job.invoke(job)
	}
	
	protected[skid] def finishedWork(work: Work, response: Any) = {
		// Send message back to server for completion of work
		client.send(work.uuid, WorkResponse(work, response, Status.Success))
	}
}

object JobWorker {
	def apply(serverAddress: InetSocketAddress, storage: File) = {
		new JobWorker(serverAddress, storage)
	}
}