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
	
	def requestWork() = synchronized {		// Must be synchronized so the same work doesn't get assigned twice
		transaction.find[Work](predicate, sort) map workMapper
	}
	
	private val workMapper = (w: Work) => {
		w.processing = true
		w
	}
	
	// Make sure it's not already being processed
	private val predicate = (w: Work) => !w.processing
	
	// Make sure they come out in the right order
	private val sort = (w1: Work, w2: Work) => w1.created.compareTo(w2.created)
}

object JobManager {
	def apply(serverAddress: InetSocketAddress, storage: File) = {
		new JobManager(serverAddress, storage)
	}
}