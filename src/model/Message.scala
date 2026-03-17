package model

import scalasql._
import upickle.default.{ReadWriter, macroRW}

type Id[T] = T

case class Message[T[_]](
  id: T[Long],
  content: T[String],
  timestamp: T[String]
)
object Message extends Table[Message] {
  implicit val rw: ReadWriter[Message[Id]] = macroRW
}
