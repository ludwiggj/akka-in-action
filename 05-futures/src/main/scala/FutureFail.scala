import scala.concurrent._
import scala.util._
import ExecutionContext.Implicits.global

object FutureFail extends App {


  def futureFail = {
    val futureFail = Future {
      throw new Exception("error!")
    }
    futureFail.foreach(value => println(value))
  }

  def futureFailCorrect = {
    val futureFail = Future {
      throw new Exception("error!")
    }
    futureFail.onComplete {
      case Success(value) => println(value)
      case Failure(e) => println(e)
    }
  }

  futureFailCorrect
  futureFail
}
