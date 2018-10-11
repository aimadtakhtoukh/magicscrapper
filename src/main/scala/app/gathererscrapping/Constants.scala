package app.gathererscrapping

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.routing.RoundRobinPool
import app.gathererscrapping.Constants.gathererPageUrl
import app.gathererscrapping.doublecard.{DoubleCardScrapping, LanguageDoubleCardScrapping}
import app.gathererscrapping.singlecard.{LanguageSingleCardScrapping, SingleCardScrapping}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, TextNode}
import org.jsoup.select.Elements

import scala.util.matching.Regex
import scala.collection.JavaConverters._

object Constants {
  val gathererUrl = "http://gatherer.wizards.com"
  val gathererPageUrl = "http://gatherer.wizards.com/Pages/"
  val multiverseRegex: Regex = "^.*?multiverseid=(\\d+).*?$".r
  val outPath = "Gatherer/"
  val imageOutPath = s"${outPath}Images/"
}

object Actors {
  val system = ActorSystem("GathererScrapper")
  val master: ActorRef = system.actorOf(
    Props[Master], name = "master"
  )
  val singleCardScrapping: ActorRef = system.actorOf(
    Props[SingleCardScrapping].withRouter(RoundRobinPool(4))
  )
  val languageSingleCardScrapping: ActorRef = system.actorOf(
    Props[LanguageSingleCardScrapping].withRouter(RoundRobinPool(4))
  )
  val doubleCardScrapping: ActorRef = system.actorOf(
    Props[DoubleCardScrapping].withRouter(RoundRobinPool(4))
  )
  val languageDoubleCardScrapping: ActorRef = system.actorOf(
    Props[LanguageDoubleCardScrapping].withRouter(RoundRobinPool(4))
  )
  val cardWorkerRouter: ActorRef = system.actorOf(
    Props[CardScrapping].withRouter(RoundRobinPool(4))
  )
  val legalityScrapper: ActorRef = system.actorOf(
    Props[LegalityScrapper].withRouter(RoundRobinPool(4))
  )
  val saveInFile: ActorRef = system.actorOf(
    Props[SaveInFile].withRouter(RoundRobinPool(4))
  )
  val cardImageDownload: ActorRef = system.actorOf(
    Props[CardImageDownload].withRouter(RoundRobinPool(4))
  )
}

object GathererScrappingFunctions {

  def getDocument(url : String): Document = {
    val conn = Jsoup.connect(url)
      .header("Accept-Language","en")
      .cookie("CardDatabaseSettings", "0=1&1=28&2=0&14=1&3=13&4=0&5=0&6=15&7=1&8=0&9=1&10=16&11=7&12=8&15=1&16=0&13=")
      .ignoreHttpErrors(true)
      .timeout(30 * 1000)
      .maxBodySize(0)
    conn.get()
  }


  def extractDetails(tuple : (String, Elements)): Option[(String, String)] = {
    tuple match {
      case ("Card Name", el) => Some("name" -> el.text)
      case ("Mana Cost", el) => Some("manaCost" -> extractManaCost(el))
      case ("Converted Mana Cost", el) => Some("convertedManaCost" -> el.text)
      case ("Types", el) => Some("types" -> el.text)
      case ("Card Text", el) => Some("cardText" -> extractCardText(el))
      case ("P/T", el) => Some("pt" -> el.text)
      case ("Expansion", el) => Some("expansion" -> el.text)
      case ("Rarity", el) => Some("rarity" -> el.text)
      case ("Card Number", el) => Some("number" -> el.text)
      case ("Artist", el) => Some("artist" -> el.text)
      case ("Loyalty", el) => Some("loyalty" -> el.text)
      case ("Flavor Text", el) => Some("flavorText" -> el.text)
      case (_, _) => None
    }
  }

  def extractManaCost(el : Elements): String =
    el.select("img")
      .asScala
      .map(_.attr("alt"))
      .mkString("[[", "]][[", "]]")

  def extractCardText(el : Elements): String = {
    el.select("img").forEach(
      e => e.replaceWith(new TextNode(s"[[${e.attr("alt")}]]"))
    )
    el.select(".cardtextbox").asScala.map(_.text).mkString("||")
  }

  def otherLanguagesFromUrl(url : String) : List[(String, String)] =
    getDocument(url)
      .select("table.cardList tr.cardItem")
      .asScala
      .map(_.select("td"))
      .map(tr => (
        tr.get(1).text(),
        tr.get(0).select("a").attr("href") match {
          case Constants.multiverseRegex(multiverseId) => multiverseId
          case _ => ""
        }
      ))
      .toList


  def buildLanguageUrl(multiverseId : String) : String =
    s"${gathererPageUrl}Card/Languages.aspx?multiverseid=$multiverseId"

  def buildPrintedCardUrl(multiverseId : String) : String =
    s"${gathererPageUrl}Card/Details.aspx?printed=true&multiverseid=$multiverseId"


}