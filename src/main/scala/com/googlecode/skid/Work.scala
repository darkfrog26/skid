package com.googlecode.skid

import java.util.Calendar
import java.util.UUID

case class Work(uuid: UUID, created: Calendar = Calendar.getInstance()) {
	@volatile @transient protected[skid] var processing = false
	var finished: Calendar = null
	
	override def toString() = "Work(" + uuid + ")"
	
	override def equals(obj: Any) = obj match {
		case work: Work => work.uuid == uuid
		case _ => false
	}
}