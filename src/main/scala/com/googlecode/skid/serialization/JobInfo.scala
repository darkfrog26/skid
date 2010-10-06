package com.googlecode.skid.serialization

import com.googlecode.skid.JobResource

import java.util.UUID

case class JobInfo(uuid: UUID, resources: JobResource*)