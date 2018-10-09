package app.gathererscrapping

import java.io.{File, PrintWriter}

import akka.actor.{Actor, ActorLogging}
import app.gathererscrapping.Constants.outPath

class SaveInFile extends Actor with ActorLogging {
  override def receive: Receive = {
    case SaveInFileMessage(setName, list) =>
      val file = new File(s"$outPath")
      file.mkdirs()
      val json = list.mkString("[", ",\n", "]")
      val fileName = setName.replaceAll("[ :.',\"]", "")
      new PrintWriter(s"$file/$fileName.json") {
        write(json)
        close()
        Actors.master ! StopMessage
      }
  }
}