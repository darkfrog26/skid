package com.googlecode.skid.communication

import java.io._

import java.net._

import java.util.concurrent.ConcurrentLinkedQueue

import org.sgine.util.FunctionRunnable
import org.sgine.util.IO._

class CommunicationClient(val address: InetSocketAddress) {
	private val connection: Socket = new Socket()
	private val readThread = new Thread(FunctionRunnable(readRunner))
	private val writeThread = new Thread(FunctionRunnable(writeRunner))
	private val queue = new ConcurrentLinkedQueue[Any]
	private lazy val input = new DataInputStream(connection.getInputStream)
	private lazy val output = new DataOutputStream(connection.getOutputStream)
	private lazy val objectInput = new ObjectInputStream(connection.getInputStream)
	private lazy val objectOutput = new ObjectOutputStream(connection.getOutputStream)
	
	private var keepAlive = true
	
	connect()
	
	private def connect() = {
		if (connection != null) {
			throw new ConnectException("Unable to establish connection as a connection already exists!")
		}
		connection.connect(address)
		
		if (!readThread.isAlive) {
			readThread.setDaemon(true)
			writeThread.setDaemon(true)
			
			readThread.start()
			writeThread.start()
		}
	}
	
	def disconnect() = {
		keepAlive = false
		connection.close()
	}
	
	def send(value: Any) = queue.add(value)
	
	private def readRunner() = {
		while (keepAlive) {
			
		}
	}
	
	private def writeRunner() = {
		while (keepAlive) {
			val value = queue.poll()
			if (value != null) {
				value match {
					case file: File => sendFile(file)
					case _ => sendObject(value)
				}
			} else {
				Thread.sleep(50)
			}
		}
	}
	
	private def sendFile(file: File) = {
		// Send file header
		output.writeInt(CommunicationHeader.File.ordinal)
		
		// Send file length
		output.writeLong(file.length)
		
		// Open input stream for file and transfer
		val input = new FileInputStream(file)
		try {
			stream(input, output)
		} finally {
			output.flush()
			input.close()
		}
	}
	
	private def sendObject(value: Any) = {
		// Send object header
		output.writeInt(CommunicationHeader.Object.ordinal)
		
		// Stream object
		objectOutput.writeObject(value)
		objectOutput.flush()
	}
}