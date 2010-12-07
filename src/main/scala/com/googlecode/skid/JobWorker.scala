package com.googlecode.skid

import com.googlecode.skid.communication._
import com.googlecode.skid.serialization._

import java.io.File

import java.net.InetSocketAddress

import java.util.UUID

import org.sgine.core._

import org.sgine.event._

import org.sgine.util.FunctionRunnable

class JobWorker private(serverAddress: InetSocketAddress, storage: File) extends Listenable {
	private val client = new CommunicationClient(this, serverAddress, storage)
	private val persistence = JobPersistence(storage)
	
	private val thread = new Thread(FunctionRunnable(run))
	private var keepAlive = true
	
	private var checkForWork = new java.util.concurrent.atomic.AtomicBoolean
	
	def start(findWork: Boolean = true) = {
		client.connect()
		
		if (findWork) {
			client.listeners += EventHandler(objectEvent, ProcessingMode.Asynchronous)
			
			thread.setDaemon(true)
			thread.start()
		}
	}
	
	private def objectEvent(evt: ObjectReceived) = {
		evt.obj match {
			case queued: WorkQueued => thread.synchronized {		// Notified of new work
				checkForWork.set(true)
				
				thread.notifyAll()
			}
			case _ =>
		}
	}
	
	private def run(): Unit = {
		while (keepAlive) {
			requestWork() match {
				case Some(work) => {
					try {
						val response = processWork(work)
						finishedWork(work, response)
					} catch {
						case t => finishedWork(work, t, Status.Error)
					}
				}
				case None => thread.synchronized {
					if (!checkForWork.getAndSet(false)) {
						thread.wait(60000)
					}
				}
			}
		}
	}
	
	def shutdown() = {
		keepAlive = false
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
	
	protected[skid] def finishedWork(work: Work, response: Any, status: Int = Status.Success) = {
		// Send message back to server for completion of work
		client.send(work.uuid, WorkResponse(work, response, status))
	}
}

object JobWorker {
	def apply(serverAddress: InetSocketAddress, storage: File) = {
		new JobWorker(serverAddress, storage)
	}
}