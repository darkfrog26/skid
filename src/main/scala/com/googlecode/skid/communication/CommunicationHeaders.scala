package com.googlecode.skid.communication

import org.sgine.core.Enum
import org.sgine.core.Enumerated

sealed class CommunicationHeader extends Enum

object CommunicationHeader extends Enumerated[CommunicationHeader] {
	case object File extends CommunicationHeader
	case object Object extends CommunicationHeader
}