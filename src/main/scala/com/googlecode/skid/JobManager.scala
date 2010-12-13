package com.googlecode.skid

import com.googlecode.skid.communication._

import java.io._

import java.net.InetSocketAddress

import java.util.Calendar
import java.util.UUID

import org.sgine.db._

import org.sgine.event._

import org.sgine.log._

import org.sgine.util.IO._

/**
 * JobManager is a server that receives jobs
 * from remote clients (JobDispatcher) and
 * requests from workers (JobWorker) to send
 * available jobs for processing. This is the
 * centralized server that will receive all
 * work but does no work itself.
 * 
 * @author Matt Hicks <mhicks@sgine.org>
 */
class JobManager private(serverAddress: InetSocketAddress, storage: File) extends Listenable {
	private val server = new CommunicationServer(serverAddress, storage)
	private lazy val db = DB.open(new File(storage, "manager.db"))
	private lazy val transaction = db.transaction(true)
	
	// TODO: auto-delete old completed jobs
	// TODO: auto-timeout jobs associated with workers when they disconnect
	
	def start() = {
		server.listeners += EventHandler(receivedObject, recursion = Recursion.Children)
		
		server.start()
	}
	
	private def receivedObject(evt: ObjectReceived) = {
		evt.obj match {
			case work: Work => {
				transaction.store(work)
				info("Stored work")
				broadcast(work.uuid, WorkQueued(work.uuid))
			}
			case request: WorkRequest => remoteWorkRequest(evt)
			case response: WorkResponse => remoteWorkResponse(evt)
			case statusRequest: WorkStatusRequest => remoteStatusRequest(evt)
			case obj => warn("JobManager received unknown object: " + obj)
		}
	}
	
	// Received when a JobWorker (or other remote client) is requesting work to do
	private def remoteWorkRequest(evt: ObjectReceived) = {
		evt.communication.sendWork(evt.uuid, requestWork())
		info("Send work to requster!")
	}
	
	// Received when a JobWorker (or other remote client) has finished a job and is responding with the result
	private def remoteWorkResponse(evt: ObjectReceived) = {
		val wr = evt.obj.asInstanceOf[WorkResponse]
		
		// Write file to disk
		val bytes = Communication.toBytes(evt.obj)
		val directory = new File(storage, evt.uuid.toString)
		val file = new File(directory, "response.bin")
		val output = new ObjectOutputStream(new FileOutputStream(file))
		try {
			output.writeObject(wr.response)
		} finally {
			output.flush()
			output.close()
		}
		
		// Update job in database
		transaction.find((w: Work) => w.uuid == wr.work.uuid) match {
			case Some(work) => {
				work.finished = Calendar.getInstance()
				transaction.store(work)
			}
			case None => warn("Received response of finished work that does not exist in the database: " + evt.uuid)
		}
		
		// Tell all the clients
		val finished = WorkFinished(wr.work)
		broadcast(evt.uuid, finished)
		
		info("Work response!")
	}
	
	def broadcast(uuid: UUID, value: Any) = {
		for (client <- server.clients) {
			client.send(uuid, value)
		}
	}
	
	private def remoteStatusRequest(evt: ObjectReceived) = {
		// Lookup response
		val directory = new File(storage, evt.uuid.toString)
		if (directory.exists) {		// Found directory
			val file = new File(directory, "response.bin")
			transaction.find((w: Work) => w.uuid == evt.uuid) match {
				case Some(work) => {
					var response: Any = null
					val status = work.finished match {
						case null => Status.Working
						case _ if (!file.exists) => Status.Queued
						case _ => {
							val input = new ObjectInputStream(new FileInputStream(file))
							try {
								response = input.readObject()
								response match {
									case t: Throwable => Status.Error
									case _ => Status.Success
								}
							} finally {
								input.close()
							}
						}
					}
					evt.communication.send(evt.uuid, WorkResponse(work, response, status))
				}
				case None => evt.communication.send(evt.uuid, WorkResponse(Work(evt.uuid), null, Status.NotFound))
			}
		} else {		// Couldn't find directory for UUID
			evt.communication.send(evt.uuid, WorkResponse(Work(evt.uuid), null, Status.NotFound))
		}
	}
	
	def requestWork() = synchronized {		// Must be synchronized so the same work doesn't get assigned twice
		transaction.find[Work](predicate, sort) map workMapper
	}
	
	def work = transaction.query[Work]((w: Work) => w.finished == null).size
	
	private val workMapper = (w: Work) => {
		w.processing = true
		w
	}
	
	// Make sure it's not already being processed
	private val predicate = (w: Work) => !w.processing && w.finished == null
	
	// Make sure they come out in the right order
	private val sort = (w1: Work, w2: Work) => w1.created.compareTo(w2.created)
	
	def shutdown() = {
		server.shutdown()
		db.close()
	}
}

object JobManager {
	def apply(serverAddress: InetSocketAddress, storage: File) = {
		new JobManager(serverAddress, storage)
	}
}