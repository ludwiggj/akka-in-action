package aia.faulttolerance.dbstrategy_alt2

import java.io.File

@SerialVersionUID(1L)
class DiskError(msg: String) extends Error(msg) with Serializable

@SerialVersionUID(1L)
class CorruptedFileException(msg: String, val file: File) extends Exception(msg) with Serializable

@SerialVersionUID(1L)
class DbBrokenConnectionException(msg: String) extends Exception(msg) with Serializable
