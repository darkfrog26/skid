package com.googlecode.skid.communication

import java.io.File

import java.net.Socket

class CommunicationServerNode(val server: CommunicationServer, val connection: Socket, val directory: File) extends Communication {
	override def parent = server
}