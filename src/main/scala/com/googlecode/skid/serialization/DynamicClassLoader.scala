package com.googlecode.skid.serialization

class DynamicClassLoader(parent: ClassLoader) extends ClassLoader(parent) {
	def loadClass(name: String, bytes: Array[Byte]) = {
		val n = name.substring(0, name.length - 6).replaceAll("/", ".")
		defineClass(n, bytes, 0, bytes.length)
	}
}