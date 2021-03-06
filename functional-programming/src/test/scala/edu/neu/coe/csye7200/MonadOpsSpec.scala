package edu.neu.coe.csye7200

import edu.neu.coe.csye7200.MonadOps._
import org.scalatest.{ FlatSpec, Matchers }
import org.scalatest.concurrent._
import java.net.URL
import scala.util._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import org.scalatest._

/**
  * @author scalaprof
  */
class MonadOpsSpec extends FlatSpec with Matchers with Futures with ScalaFutures {

  "lift(Future[Try[T]])" should "succeed for http://www.google.com" in {
    val uyf = Future(Try(new URL("http://www.google.com")))
    val uf = MonadOps.flatten(uyf)
    whenReady(uf) { u => u should matchPattern { case x: URL => } }
  }

  "lift(Try[Future[T]])" should "succeed for http://www.google.com" in {
    val ufy = Try(Future(new URL("http://www.google.com")))
    val uf = MonadOps.flatten(ufy)
    whenReady(uf) { u => u should matchPattern { case x: URL => } }
  }

  "sequence(Seq[Future[T]])" should "succeed for http://www.google.com, etc." in {
    val ws = List("http://www.google.com", "http://www.microsoft.com")
    val ufs = for {w <- ws; uf = Future(new URL(w))} yield uf
    whenReady(Future.sequence(ufs)) { us => Assertions.assert(us.length == 2) }
  }

  "sequence(Seq[Try[T]])" should "succeed for http://www.google.com, etc." in {
    val ws = List("http://www.google.com", "http://www.microsoft.com")
    val uys = for {w <- ws; url = Try(new URL(w))} yield url
    MonadOps.sequence(uys) match {
      case Success(us) => Assertions.assert(us.length == 2)
      case _ => Failed
    }
  }

  it should "fail for www.google.com, etc." in {
    val ws = List("www.google.com", "http://www.microsoft.com")
    val uys = for {w <- ws; uy = Try(new URL(w))} yield uy
    MonadOps.sequence(uys) match {
      case Failure(e) => Succeeded
      case _ => Failed
    }
  }

  it should "succeed for empty list" in {
    val uys = for { w <- List[String](); uy = Try(new URL(w))} yield uy
    sequence(uys) match {
      case Success(us) => Assertions.assert(us.isEmpty)
      case _ => Failed
    }
  }

  "flatten" should "succeed" in {
    val ifs: Seq[Future[Seq[Int]]] = Seq(Future(Seq(1,2)))
    whenReady(flatten(ifs)) {x => println(x); x should matchPattern { case Seq(1,2) => }}
  }

  it should "succeed for http://www.google.com, etc." in {
    val ws = List("http://www.google.com", "http://www.microsoft.com")
    val ufs = for {w <- ws; uf = Future(new URL(w))} yield uf
    val usfs = List(Future.sequence(ufs))
    whenReady(MonadOps.flatten(usfs)) { us => Assertions.assert(us.length == 2) }
  }

  it should "succeed for empty list" in {
    val ws = List[String]()
    val urls = for { w <- ws; uf = Future(new URL(w))} yield uf
    val usfs = List(Future.sequence(urls))
    whenReady(flatten(usfs)) { us => Assertions.assert(us.isEmpty) }
  }
  it should "succeed for option map" in {
    val map: Map[String,Option[String]] = Map("a"->Some("A"), "b"->None)
    val flat: Map[String,String] = flatten(map)
    flat.size shouldBe 1
  }
  "sequence" should "succeed for http://www.google.com, www.microsoft.com" in {
    val ws = Seq("http://www.google.com", "http://www.microsoft.com", "www.microsoft.com")
    val ufs = for {w <- ws; uf = Future(new URL(w))} yield uf
    val uefs = for {uf <- ufs} yield MonadOps.sequence(uf)
    val uesf = Future.sequence(uefs)
    whenReady(uesf) { ues => Assertions.assert(ues.length == 3) }
    whenReady(uesf) { ues => (ues.head, ues(1)) should matchPattern { case (Right(x), Right(y)) => } }
    whenReady(uesf) { ues => ues(2) should matchPattern { case Left(x) => } }
  }

  "sequence(Future=>Future(Either))" should "succeed for http://www.google.com, www.microsoft.com" in {
    val ws = Seq("http://www.google.com", "http://www.microsoft.com", "www.microsoft.com")
    val uefs = for {w <- ws; uf = Future(new URL(w))} yield MonadOps.sequence(uf)
    for {uef <- uefs} whenReady(uef) { case Right(u) => true; case Left(e) => true; case _ => Assertions.fail() }
  }

  "Sequence[Either]" should "succeed" in {
    val l: Either[Throwable, Int] = Left(new RuntimeException("bad"))
    val r: Either[Throwable, Int] = Right(99)
    MonadOps.sequence(l) should matchPattern { case None => }
    MonadOps.sequence(r) should matchPattern { case Some(99) => }
  }

  "zip(Option,Option)" should "succeed" in {
    val (one, two, none) = (Some(1), Some(2), None)
    zip(one,two) should matchPattern { case Some((1,2)) => }
    zip(none,two) should matchPattern { case None => }
    zip(one,none) should matchPattern { case None => }
  }
  "optionToTry" should "succeed for Map" in {
    val map = Map("a"->"A", "b"->"B")
    optionToTry(map.get("a")) should matchPattern { case Success("A") => }
    optionToTry(map.get("x")) should matchPattern { case Failure(_) => }
  }
  "lift" should "succeed" in {
    def double(x: Int) = 2*x
    Success(1) map double _ should matchPattern { case Success(2) => }
    Failure(new Exception("bad")) map double _ should matchPattern { case Failure(_) => }
  }

  "map2" should "succeed" in {
    val one = Success(1)
    val two = Success(2)
    def sum(x: Int,y: Int) = x+y
    map2(one,two)(sum _) should matchPattern { case Success(3) => }
    map2(one,Failure(new Exception("bad")))(sum _) should matchPattern { case Failure(_) => }
  }

  "asFuture" should "succeed" in {
    whenReady(asFuture(Success(1))) { x => x should matchPattern { case 1 => }}
    //    whenReady(toFuture(Failure[Int](new Exception("bad")))) { x => p shouldBe new Exception("bad")}
  }

}