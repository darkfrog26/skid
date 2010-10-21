package com.googlecode.skid.communication

import java.io.File

import java.net._

import org.sgine.event.Event
import org.sgine.event.Listenable

import org.sgine.log._

import org.sgine.util.FunctionRunnable

import scala.collection.mutable.SynchronizedQueue

class CommunicationServer(val address: SocketAddress, val directory: File) extends Listenable {
	private val serverSocket = new ServerSocket()
	
	private var keepAlive = true
	private val thread = new Thread(FunctionRunnable(run))
	
	private val queue = new SynchronizedQueue[CommunicationServerNode]
	
	def start() = {
		serverSocket.bind(address)
		serverSocket.setSoTimeout(5000)
		
		thread.start()
	}
	
	private def run() = {
		while (keepAlive) {
			try {
				val socket = serverSocket.accept()
				val node = new CommunicationServerNode(this, socket, directory)
				node.connect()
				queue += node
				
				info("Socket connection established to server: " + socket.getInetAddress)
				
				Event.enqueue(ConnectionEstablished(node))
			} catch {
				case exc: SocketTimeoutException => // Ignore
			}
		}
		serverSocket.close()
	}
	
	def shutdown() = keepAlive = false
}