package com.googlecode.skid

import java.util.Calendar
import java.util.UUID

case class Work(uuid: UUID) {
	@volatile @transient protected[skid] var processing = false
	
	// Work-around for thread-safety bug in Calendar persistence
	private var createdMS = System.currentTimeMillis
	private var finishedMS = -1L
	
	@transient private var _created: Calendar = calendar(createdMS)
	@transient private var _finished: Calendar = calendar(finishedMS)
	
	def created = _created
	
	def finished = _finished
	def finished_=(c: Calendar) = {
		finishedMS = c.getTimeInMillis()
		_finished = c
	}
	
	private def calendar(time: Long) = {
		if (time > 0) {
			val c = Calendar.getInstance
			c.setTimeInMillis(time)
			c
		} else {
			null
		}
	}
	
	override def toString() = "Work(" + uuid + ")"
	
	override def equals(obj: Any) = obj match {
		case work: Work => work.uuid == uuid
		case _ => false
	}
}