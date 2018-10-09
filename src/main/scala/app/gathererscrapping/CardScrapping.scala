package app.gathererscrapping

import akka.actor.{Actor, ActorLogging, Props}
import app.gathererscrapping.Constants.gathererPageUrl
import app.gathererscrapping.GathererScrappingFunctions._
import org.jsoup.nodes.Document

class CardScrapping extends Actor with ActorLogging {
  override def receive: Receive = {
    case MultiverseIdMessage(multiverseId) =>
      val document = getDocument(buildCardUrl(multiverseId))
      if (!isDoubleCard(document)) {
        val singleCardScrapping = Actors.system.actorOf(Props[SingleCardScrapping])
        singleCardScrapping forward CardDetailsMessage(document, multiverseId)
      } else {
        val doubleCardScrapping = Actors.system.actorOf(Props[DoubleCardScrapping])
        doubleCardScrapping forward CardDetailsMessage(document, multiverseId)
      }
  }

  def buildCardUrl(multiverseId : String) : String =
    s"${gathererPageUrl}Card/Details.aspx?multiverseid=$multiverseId"


  def isDoubleCard(document: Document): Boolean = document.getElementsByClass("rightCol").size() >= 2
}


