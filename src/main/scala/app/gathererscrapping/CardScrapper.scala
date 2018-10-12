package app.gathererscrapping

import akka.actor.{Actor, ActorLogging}
import app.gathererscrapping.Constants.gathererPageUrl
import app.gathererscrapping.GathererScrappingFunctions._
import org.jsoup.nodes.Document

class CardScrapper extends Actor with ActorLogging {
  override def receive: Receive = {
    case MultiverseIdMessage(multiverseId) =>
      val document = getDocument(buildCardUrl(multiverseId))
      if (!isDoubleCard(document)) {
        Actors.singleCardScrapping forward CardDetailsMessage(document, multiverseId)
      } else {
        Actors.doubleCardScrapping forward CardDetailsMessage(document, multiverseId)
      }
  }

  def buildCardUrl(multiverseId : String) : String =
    s"${gathererPageUrl}Card/Details.aspx?multiverseid=$multiverseId"


  def isDoubleCard(document: Document): Boolean = document.getElementsByClass("rightCol").size() >= 2

}


