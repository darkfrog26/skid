package com.googlecode.skid

import com.googlecode.skid.communication._

import java.io.File

import java.net.InetSocketAddress

import org.sgine.db._

import org.sgine.event.Event
import org.sgine.event.EventHandler
import org.sgine.event.Recursion

import org.sgine.log._

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
class JobManager private(serverAddress: InetSocketAddress, storage: File) {
	private val server = new CommunicationServer(serverAddress, storage)
	private lazy val db = DB.open(new File(storage, "manager.db"))
	private lazy val transaction = db.transaction(true)
	
	def start() = {
		server.listeners += EventHandler(receivedObject, recursion = Recursion.Children)
		
		server.start()
	}
	
	private def receivedObject(evt: ObjectReceived) = {
		evt.obj match {
			case work: Work => transaction.store(work)
			case obj => warn("JobManager received unknown object: " + obj)
		}
	}
	
	def requestWork() = transaction.find[Work]()
}

object JobManager {
	def apply(serverAddress: InetSocketAddress, storage: File) = {
		new JobManager(serverAddress, storage)
	}
}