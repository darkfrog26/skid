package com.googlecode.skid

import com.googlecode.skid.communication._
import com.googlecode.skid.serialization._
import java.io.File

import java.net.InetSocketAddress

import java.util.UUID

import org.sgine.event._

import org.sgine.log._

import org.sgine.util.Time

/**
 * JobDispatcher is a client that connects remotely
 * to a centralized JobManager to send and receive
 * information about jobs.
 * 
 * @author Matt Hicks <mhicks@sgine.org>
 */
class JobDispatcher private(serverAddress: InetSocketAddress, storage: File) extends Listenable {
	private val client = new CommunicationClient(this, serverAddress, storage)
	private val persistence = JobPersistence(new File(storage, "temp"))
	
	def start() = {
		client.listeners += EventHandler(clientEvent)
		
		client.connect()
	}
	
	private def clientEvent(evt: Event) = {
//		println("JobDispatcher.clientEvent: " + evt)
	}
	
	def send(f: AnyRef, resources: JobResource*) = {
		// Persist the job for transfer
		val uuid = persistence.persist(f, resources: _*)
		
		// Transfer the job to the server
		for (file <- persistence.files(uuid)) {
			client.send(uuid, file)
		}
		
		// Send an object with a reference to the UUID to persist the work
		client.send(uuid, Work(uuid))
		
		// Return the UUID
		uuid
	}
	
	def status(uuid: UUID, time: Double = 10.0) = {
		// Send request for status
		client.send(uuid, WorkStatusRequest(uuid))
		
		// Wait for WorkResponse
		val evt = client.waitForEvent[ObjectReceived](time, filter = (evt: ObjectReceived) => evt.obj match {
			case wr: WorkResponse => evt.uuid == uuid
			case _ => false
		})
		if (evt != null) {
			Some(evt.obj.asInstanceOf[WorkResponse])
		} else {
			None
		}
	}
	
	def waitForFinish(uuid: UUID, time: Double = 10.0) = {
		var option: Option[WorkResponse] = None
		Time.waitFor(time) {
			option = status(uuid, time)
			option != None && (option.get.status == Status.Success || option.get.status == Status.Error)
		}
		option
	}
	
	def apply[T](f: => T) = {
		val uuid = send(f _, JobResource((f _).asInstanceOf[AnyRef].getClass, distribute = true))
		waitForFinish(uuid, 10.0) match {
			case Some(wr) => {
				wr.status match {
					case Status.Success => Some(wr.response.asInstanceOf[T])
					case Status.Error => throw wr.response.asInstanceOf[Throwable]
					case _ => None
				}
			}
			case None => None
		}
	}
	
	def shutdown() = {
		client.disconnect()
	}
}

object JobDispatcher {
	def apply(serverAddress: InetSocketAddress, storage: File) = {
		new JobDispatcher(serverAddress, storage)
	}
}