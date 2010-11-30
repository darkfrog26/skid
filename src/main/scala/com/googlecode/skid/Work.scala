package com.googlecode.skid

import java.util.Calendar
import java.util.UUID

case class Work(uuid: UUID, created: Calendar = Calendar.getInstance()) {
	@volatile @transient protected[skid] var processing = false
	
	override def toString() = "Work(" + uuid + ")"
}