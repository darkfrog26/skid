package com.googlecode.skid.communication

import java.net._

import org.sgine.event.Event

import org.sgine.util.FunctionRunnable

import scala.collection.mutable.SynchronizedQueue

class CommunicationServer(val address: SocketAddress) {
	private val serverSocket = new ServerSocket()
	
	private var keepAlive = true
	private val thread = new Thread(FunctionRunnable(run))
	
	private val queue = new SynchronizedQueue[CommunicationServerNode]
	
	init()
	
	protected def init() = {
		serverSocket.bind(address)
		serverSocket.setSoTimeout(5000)
	}
	
	private def run() = {
		while (keepAlive) {
			try {
				val socket = serverSocket.accept()
				val node = new CommunicationServerNode(this, socket)
				queue += node
				
				Event.enqueue(ConnectionEstablished(node))
			} catch {
				case exc: SocketTimeoutException => // Ignore
			}
		}
		serverSocket.close()
	}
}