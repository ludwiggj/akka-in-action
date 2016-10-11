package aia.faulttolerance.dbstrategy

import java.io.File

import scala.language.postfixOps

// Unrecoverable
@SerialVersionUID(1L)
class DiskError(msg: String)
  extends Error(msg) with Serializable

// Log file is corrupt, cannot be processed
@SerialVersionUID(1L)
class CorruptedFileException(msg: String, val file: File)
  extends Exception(msg) with Serializable

@SerialVersionUID(1L)
class DbBrokenConnectionException(msg: String)
  extends Exception(msg) with Serializable

// Database node has fatally crashed
@SerialVersionUID(1L)
class DbNodeDownException(msg: String)
  extends Exception(msg) with Serializable