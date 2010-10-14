package com.googlecode.skid.communication

import java.net.Socket

class CommunicationServerNode(val server: CommunicationServer, val connection: Socket) extends Communication {
	init()
}