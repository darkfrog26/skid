package com.googlecode.skid.communication

import java.io.File

import java.util.UUID

import org.sgine.event.BasicEvent
import org.sgine.event.Listenable

trait ReceivedEvent

trait SentEvent

trait FileEvent

trait ObjectEvent

class CommunicationEvent(val communication: Communication) extends BasicEvent(communication)

case class FileReceived(uuid: UUID, file: File, override val communication: Communication) extends CommunicationEvent(communication) with ReceivedEvent with FileEvent

case class ObjectReceived(uuid: UUID, obj: Any, override val communication: Communication) extends CommunicationEvent(communication) with ReceivedEvent with ObjectEvent

case class FileSent(uuid: UUID, file: File, override val communication: Communication) extends CommunicationEvent(communication) with SentEvent with FileEvent

case class ObjectSent(uuid: UUID, obj: Any, override val communication: Communication) extends CommunicationEvent(communication) with SentEvent with ObjectEvent

case class ConnectionEstablished(override val communication: Communication) extends CommunicationEvent(communication)