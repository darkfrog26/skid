package com.googlecode.skid.communication

import java.io._

import java.net._

import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

import org.sgine.event.Event
import org.sgine.event.Listenable

import org.sgine.util.FunctionRunnable
import org.sgine.util.IO._

trait Communication extends Listenable {
	protected def connection: Socket
	
	private var keepAlive = true
	private val readThread = new Thread(FunctionRunnable(readRunner))
	private val writeThread = new Thread(FunctionRunnable(writeRunner))
	private val queue = new ConcurrentLinkedQueue[(UUID, Any)]
	protected lazy val input = new DataInputStream(connection.getInputStream)
	protected lazy val output = new DataOutputStream(connection.getOutputStream)
	protected lazy val objectInput = new ObjectInputStream(connection.getInputStream)
	protected lazy val objectOutput = new ObjectOutputStream(connection.getOutputStream)
	
	protected def init() = {
		if (!readThread.isAlive) {
			readThread.setDaemon(true)
			writeThread.setDaemon(true)
			
			readThread.start()
			writeThread.start()
		}
	}
	
	def isAlive = keepAlive
	
	def disconnect() = {
		keepAlive = false
		connection.close()
	}
	
	def send(uuid: UUID, value: Any) = queue.add(uuid -> value)
	
	private def readRunner() = {
		while (keepAlive) {
			// Read UUID
			val uuid = objectInput.readObject().asInstanceOf[UUID]
			if (uuid != null) {
				// Read header
				val header = CommunicationHeader(input.readInt())
				header match {
					case CommunicationHeader.File => readFile(uuid)
					case CommunicationHeader.Object => readObject(uuid)
					case _ => throw new RuntimeException("Unknown CommunicationHeader: " + header)
				}
			}
		}
	}
	
	private def writeRunner() = {
		while (keepAlive) {
			queue.poll() match {
				case null => Thread.sleep(50)
				case (uuid: UUID, file: File) => sendFile(uuid, file)
				case (uuid: UUID, value: Any) => sendObject(uuid, value)
			}
		}
	}
	
	private def readFile(uuid: UUID) = {
		// Read file length
		val length = input.readInt()
		
		// Write file
		
		// Throw event
	}
	
	private def readObject(uuid: UUID) = {
		// Read object
		val value = objectInput.readObject()
		
		// Throw event
		Event.enqueue(ObjectReceived(uuid, value, this))
	}
	
	private def sendFile(uuid: UUID, file: File) = {
		// Send UUID
		objectOutput.writeObject(uuid)
		objectOutput.flush()
		
		// Send file header
		output.writeInt(CommunicationHeader.File.ordinal)
		
		// Send file length
		output.writeLong(file.length)
		
		// Open input stream for file and transfer
		val input = new FileInputStream(file)
		try {
			stream(input, output)
			
			Event.enqueue(FileSent(uuid, file, this))
		} finally {
			output.flush()
			input.close()
		}
	}
	
	private def sendObject(uuid: UUID, value: Any) = {
		// Send UUID
		objectOutput.writeObject(uuid)
		objectOutput.flush()
		
		// Send object header
		output.writeInt(CommunicationHeader.Object.ordinal)
		
		// Stream object
		objectOutput.writeObject(value)
		objectOutput.flush()
	}
}