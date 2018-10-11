package app.gathererscrapping.singlecard

import akka.actor.{Actor, ActorLogging}
import app.beans.Language
import app.gathererscrapping.GathererScrappingFunctions._
import app.gathererscrapping.LanguageCardMessage

import scala.util.{Failure, Success, Try}

class LanguageSingleCardScrapping extends Actor with ActorLogging with SingleCardDetailsScrapping {
  override def receive: Receive = {
    case LanguageCardMessage(language, multiverseId) =>
      val document = getDocument(buildPrintedCardUrl(multiverseId))
      val tryDetails = singleCardDetailsFromDocument(document, multiverseId)
      tryDetails match {
        case Success(details) => sender ! singleCardFromExtractedDetails(details, language)
        case Failure(exception) => log.error(exception.getMessage)
      }

  }

  def singleCardFromExtractedDetails(details : Map[String, String], langString : String) : Language = {
    Try(
      Language(
        multiverseId = details.getOrElse("multiverseId", ""),
        name = details.getOrElse("name", ""),
        types = details.getOrElse("types", ""),
        cardText = details.getOrElse("cardText", ""),
        flavorText = details.getOrElse("flavorText", ""),
        language = langString
      )
    ) match {
      case Success(language) => language
      case Failure(_) =>
        log.error(s"Error with language card ${details("multiverseId")}, data = $details")
        Language()
    }
  }

}
