package app.gathererscrapping

import java.net.URLEncoder

import akka.actor.{Actor, ActorLogging, Props}
import app.beans.Card
import app.gathererscrapping.Constants.gathererPageUrl
import app.gathererscrapping.GathererScrappingFunctions._
import org.jsoup.nodes.Document

import scala.collection.JavaConverters._

sealed trait GathererScrapperMessage
case object StartMessage extends GathererScrapperMessage
case class SetUrlMessage(setUrl : String) extends GathererScrapperMessage
case class MultiverseIdMessage(multiverseId : String) extends GathererScrapperMessage
case class CardDetailsMessage(document: Document, multiverseId : String) extends GathererScrapperMessage
case class CardMessage(card : Card) extends GathererScrapperMessage
case class JsonMessage(card : Card, json : String) extends GathererScrapperMessage
case class SaveInFileMessage(setName : String, list: List[String]) extends GathererScrapperMessage
case object StopMessage extends GathererScrapperMessage

object GathererScrapper extends App {
  scrap()

  def scrap(): Unit = {
    Actors.master ! StartMessage
  }
}

class Master extends Actor with ActorLogging {
  var setCount = 0
  var savedSetCount = 0
  override def receive: Receive = {
    case StartMessage =>
      log.info("Starting")
      val setList = magicSetList.par
      setCount = setList.length
      log.info(s"Set count : $setCount")
      setList
        .map(getFullSetUrl)
        .map(setUrl => {log.info(setUrl); setUrl})
        .foreach(context.actorOf(Props[SetToCard]) ! SetUrlMessage(_))
    case StopMessage =>
      savedSetCount += 1
      log.info(s"Saved set count : $savedSetCount")
      if (setCount == savedSetCount) {
        log.info("Done!")
        Actors.system.terminate()
      }
  }

  def getFullSetUrl(setName : String) : String = s"${gathererPageUrl}Search/Default.aspx?action=advanced&sort=cn+&set=[%22$setName%22]"

  def magicSetList : List[String] = {
    getDocument(s"${gathererPageUrl}Default.aspx")
      .getElementById("ctl00_ctl00_MainContent_Content_SearchControls_setAddText")
      .children
      .asScala
      .map(_.attr("value"))
      .filterNot(_.isEmpty)
      .map(URLEncoder.encode(_, "UTF-8"))
      .toList
  }
}

