package com.googlecode.skid

// Response sent from JobWorker when finished processing and from JobManager when status requested
case class WorkResponse(work: Work, response: Any, status: Int)

object Status {
	val NotFound = 0
	val Queued = 1
	val Working = 2
	val Success = 3
	val Error = 4
}