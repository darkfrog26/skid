package com.googlecode.skid

import java.util.Calendar
import java.util.UUID

case class Work(uuid: UUID, created: Calendar = Calendar.getInstance()) {
	override def toString() = "Work(" + uuid + ")"
}