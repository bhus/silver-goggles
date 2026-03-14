package model

import scalasql._

case class Message[T[_]](
  id: T[Long],
  content: T[String],
  timestamp: T[String]
)
object Message extends Table[Message]
