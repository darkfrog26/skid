package com.googlecode.skid

case class Job(f: AnyRef, classLoader: ClassLoader, resources: JobResource*)

object Job {
	def invoke(job: Job) = job.f match {
		case f0: Function0[_] => f0()
		case _ => throw new RuntimeException("Unknown Job function: " + job.f.getClass)
	}
}