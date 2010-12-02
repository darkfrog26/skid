package com.googlecode.skid.communication

import java.io._

import java.net._

import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

import com.googlecode.skid._

import org.sgine.event.Event
import org.sgine.event.Listenable

import org.sgine.log._

import org.sgine.util.FunctionRunnable
import org.sgine.util.IO._

trait Communication extends Listenable {
	protected def connection: Socket
	protected def directory: File
	
	private var keepAlive = true
	private val readThread = new Thread(FunctionRunnable(readRunner))
	private val writeThread = new Thread(FunctionRunnable(writeRunner))
	private val queue = new ConcurrentLinkedQueue[(UUID, Any)]
	protected lazy val input = new DataInputStream(connection.getInputStream)
	protected lazy val output = new DataOutputStream(connection.getOutputStream)
	
	def connect() = {
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

	def sendWork(uuid: UUID, option: Option[Work]) = option match {
		case Some(work) => {
			// Transfer all files for job
			val workDirectory = new File(directory, work.uuid.toString)
			for (file <- workDirectory.listFiles) {
				send(work.uuid, file)
			}
			
			// Send the response
			send(uuid, work)
		}
		case None => send(uuid, null)
	}
	
	private def readRunner() = {
		try {
			while (keepAlive) {
				// Read UUID
				val uuid = readObject().asInstanceOf[UUID]
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
		} catch {
			case exc if (!keepAlive) =>	// Ignore
			case exc => {
				warn(exc.getClass.getName)
//				exc.printStackTrace()
				// TODO: notify of connection drop
				keepAlive = false
			}
		}
	}
	
	private def writeRunner() = {
		try {
			while (keepAlive) {
				queue.poll() match {
					case null => Thread.sleep(50)
					case (uuid: UUID, file: File) => sendFile(uuid, file)
					case (uuid: UUID, value: Any) => sendObject(uuid, value)
				}
			}
		} catch {
			case exc if (!keepAlive) =>	// Ignore
			case exc => {
				warn(exc.getClass.getName)
				exc.printStackTrace()
				// TODO: notify of connection drop
				keepAlive = false
			}
		}
	}
	
	private def readFile(uuid: UUID) = {
		// Read file name
		val name = readObject().asInstanceOf[String]
		
		// Read file length
		val length = input.readInt()
		
		// Write file
		val file = createFile(uuid, name)
		val output = new FileOutputStream(file)
		try {
			stream(input, output, length = length)
		} finally {
			output.flush()
			output.close()
		}
		
		// Throw event
		Event.enqueue(FileReceived(uuid, file, this))
	}
	
	private def readObject(uuid: UUID): Any = {
		// Read object
		val value = readObject()
		
		// Throw event
		Event.enqueue(ObjectReceived(uuid, value, this))
	}
	
	private def sendFile(uuid: UUID, file: File) = {
		// Send UUID
		writeObject(uuid)
		
		// Send file header
		output.writeInt(CommunicationHeader.File.ordinal)
		
		// Send filename
		val name = file.getName
		writeObject(name)
		
		// Send file length
		output.writeInt(file.length.toInt)
		
		// Open input stream for file and transfer
		val input = new FileInputStream(file)
		try {
			stream(input, output, length = file.length.toInt)
			
			Event.enqueue(FileSent(uuid, file, this))
		} finally {
			output.flush()
			input.close()
		}
	}
	
	private def sendObject(uuid: UUID, value: Any) = {
		// Send UUID
		writeObject(uuid)
		
		// Send object header
		output.writeInt(CommunicationHeader.Object.ordinal)
		
		// Stream object
		writeObject(value)
	}
	
	private def createFile(uuid: UUID, filename: String): File = {
		val uuidDirectory = new File(directory, uuid.toString)
		uuidDirectory.mkdirs()
		new File(uuidDirectory, filename)
	}
	
	def readObject(): Any = {
		val length = input.readInt()
		val data = new Array[Byte](length)
		input.readFully(data)
		fromBytes(data)
	}
	
	def writeObject(o: Any) = {
		val data = toBytes(o)
		output.writeInt(data.length)
		output.write(data)
		output.flush()
	}
	
	private def toBytes(o: Any) = {
		val bos = new ByteArrayOutputStream()
		val oos = new ObjectOutputStream(bos)
		oos.writeObject(o)
		oos.flush()
		bos.close()
		bos.toByteArray()
	}
	
	private def fromBytes(data: Array[Byte]) = {
		val bis = new ByteArrayInputStream(data)
		val ois = new ObjectInputStream(bis)
		try {
			ois.readObject()
		} finally {
			ois.close()
		}
	}
}