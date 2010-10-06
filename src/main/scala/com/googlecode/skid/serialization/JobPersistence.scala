package com.googlecode.skid.serialization

import com.googlecode.skid._

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.ObjectStreamClass
import java.io.OutputStream

import java.net.URL

import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object JobPersistence {
	var TemporaryStorage = new File("temp")
	
	def load(uuid: UUID) = {
		val directory = new File(TemporaryStorage, uuid.toString)
		
		// Load job object
		val info = loadJobInfo(directory)
		
		// Create ClassLoader context
		val classLoader = new DynamicClassLoader(Thread.currentThread.getContextClassLoader)
		
		// Load distributed resources
		val resources = new Array[JobResource](info.resources.length)
		for (index <- 0 until info.resources.length) {
			val r = info.resources(index)
			val value = r.value match {
				case pr: PersistedResource => pr.resourceType match {
					case "class" => {
						val input = new FileInputStream(new File(directory, "resource" + index + ".bin"))
						val output = new ByteArrayOutputStream()
						try {
							stream(input, output)
							output.flush()
							
							val bytes = output.toByteArray
							classLoader.loadClass(pr.stored.asInstanceOf[String], bytes)
						} finally {
							output.close()
							input.close()
						}
					}
					case "file" => {
						new File(directory, pr.stored.asInstanceOf[String])
					}
					case "url" => {
						new File(directory, "resource" + index + ".bin").toURI.toURL
					}
				}
				case value => value
			}
			resources(index) = JobResource(value, r.argument, r.distribute)
		}
		
		// Load function
		val f = loadFunction(directory, classLoader)
		
		Job(f, classLoader, resources: _*)
	}
	
	private def loadJobInfo(directory: File) = {
		val oos = new ObjectInputStream(new FileInputStream(new File(directory, "info.bin")))
		try {
			oos.readObject().asInstanceOf[JobInfo]
		} finally {
			oos.close()
		}
	}
	
	private def loadFunction(directory: File, classLoader: ClassLoader) = {
		val oos = new ObjectInputStream(new FileInputStream(new File(directory, "job.bin"))) {
			override def resolveClass(desc: ObjectStreamClass) = {
				Class.forName(desc.getName, false, classLoader)
			}
		}
		try {
			oos.readObject()
		} finally {
			oos.close()
		}
	}
	
	def persist(f: AnyRef, resources: JobResource*) = {
		// Make sure storage directory exists
		val uuid = UUID.randomUUID()
		val directory = new File(TemporaryStorage, uuid.toString)
		directory.mkdirs()
		
		// Convert distributed resources into files
		val array = new Array[JobResource](resources.length)
		val files = new Array[File](resources.length)
		for ((r, index) <- resources.zipWithIndex) {
			if (r.distribute) {
				// TODO: modularly handle
				r.value match {
					case c: Class[_] => {
						val name = c.getName.replaceAll("[.]", "/") + ".class"
						val input = getClass.getClassLoader.getResourceAsStream(name)
						val file = store(input, new File(directory, "resource" + index + ".bin"))
						array(index) = JobResource(PersistedResource("class", name), r.argument, r.distribute)
						files(index) = file
					}
					case f: File => {
						array(index) = JobResource(PersistedResource("file", f), r.argument, r.distribute)
						files(index) = f
					}
					case u: URL => {
						val input = u.openStream
						val file = store(input, new File(directory, "resource" + index + ".bin"))
						array(index) = JobResource(PersistedResource("url", u), r.argument, r.distribute)
						files(index) = file
					}
					case _ => throw new RuntimeException("Unable to distribute: " + r.value.asInstanceOf[AnyRef].getClass)
				}
			}
		}
		
		def storeJobInfo() = {
			// Store persisted job information
			val info = JobInfo(uuid, array: _*)
			val file = new File(directory, "info.bin")
			val output = new ObjectOutputStream(new FileOutputStream(file))
			try {
				output.writeObject(info)
			} finally {
				output.flush()
				output.close()
			}
		}
		
		def storeJob() = {
			// Store persisted function
			val file = new File(directory, "job.bin")
			val output = new ObjectOutputStream(new FileOutputStream(file))
			try {
				output.writeObject(f)
			} finally {
				output.flush()
				output.close()
			}
		}
		
		storeJobInfo()
		storeJob()
		
		// Return the UUID
		uuid
	}
	
	def store(input: InputStream, file: File) = {
		val output = new FileOutputStream(file)
		try {
			stream(input, output)
		} finally {
			output.flush()
			output.close()
			input.close()
		}
		
		file
	}
	
	@scala.annotation.tailrec
	def stream(input: InputStream, output: OutputStream, buf: Array[Byte] = new Array[Byte](512)): Unit = {
		val len = input.read(buf)
		if (len != -1) {
			output.write(buf, 0, len)
			stream(input, output, buf)
		}
	}
	
	def main(args: Array[String]): Unit = {
		val f = () => println("This is my persistent function!")
		println(persist(f, JobResource(f.getClass, distribute = true)))
		
//		val f = load(UUID.fromString("4dceecd2-2758-4cc4-8c43-496f9e9f69e5"))
	}
}