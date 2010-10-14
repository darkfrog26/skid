package com.googlecode.skid.communication

import java.net._

import org.sgine.event.Event
class CommunicationClient(val address: SocketAddress) extends Communication {
	protected val connection: Socket = new Socket()
	
	connect()
	
	private def connect() = {
		if (connection != null) {
			throw new ConnectException("Unable to establish connection as a connection already exists!")
		}
		connection.connect(address)
		
		init()
		
		Event.enqueue(ConnectionEstablished(this))
	}
}