package app.gathererscrapping.card.doublecard

import akka.actor.{Actor, ActorLogging}
import app.beans.Language
import app.gathererscrapping.GathererScrappingFunctions._
import app.gathererscrapping.LanguageCardMessage

import scala.util.{Failure, Success, Try}

class LanguageDoubleCardScrapping extends Actor with ActorLogging with DoubleCardDetailsScrapping {
  override def receive: Receive = {
    case LanguageCardMessage(language, multiverseId) =>
      val languageDocument = getDocument(buildPrintedCardUrl(multiverseId))
      val (printedLeftSideDetails, printedRightSideDetails) = detailsFromDocument(languageDocument)
      val leftSide : Language = cardLanguageFromExtractedDetails(printedLeftSideDetails, language, multiverseId)
      val rightSide : Language = cardLanguageFromExtractedDetails(printedRightSideDetails, language, multiverseId)
      sender ! (leftSide, rightSide)
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

  def cardLanguageFromExtractedDetails(details : Map[String, String], language : String, multiverseId : String) : Language = {
    Language(
      multiverseId = multiverseId,
      name = details.getOrElse("name", ""),
      types = details.getOrElse("types", ""),
      cardText = details.getOrElse("cardText", ""),
      flavorText = details.getOrElse("flavorText", ""),
      language = language
    )
  }

}
