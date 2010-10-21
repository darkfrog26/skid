package com.googlecode.skid.communication

import java.io.File

import java.net._

import org.sgine.event.Event

import org.sgine.log._
class CommunicationClient(val address: SocketAddress, val directory: File) extends Communication {
	protected val connection: Socket = new Socket()
	
	override def connect() = {
		if (connection.isConnected) {
			throw new ConnectException("Unable to establish connection as a connection already exists!")
		}
		info("Connecting to server: %1s", args = List(address))
		connection.connect(address)
		info("Connection to server established. Initializing.")
		
		super.connect()
		
		Event.enqueue(ConnectionEstablished(this))
	}
}