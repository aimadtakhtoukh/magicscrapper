package app.gathererscrapping

import akka.actor.Actor
import app.gathererscrapping.Constants.{gathererUrl, imageOutPath}

import sys.process._
import java.net.URL
import java.io.File

class CardImageDownload extends Actor {
  override def receive: Receive = {
    case MultiverseIdMessage(multiverseId) =>
      new URL(buildImageUrl(multiverseId)) #> new File(s"$imageOutPath$multiverseId.jpg") !!;
      sender ! "Ok"
  }

  def buildImageUrl(multiverseId : String) : String =
    s"$gathererUrl/Handlers/Image.ashx?multiverseid=$multiverseId&type=card"
}
