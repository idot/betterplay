package controllers

object testEx {;import org.scalaide.worksheet.runtime.library.WorksheetSupport._; def main(args: Array[String])=$execute{;$skip(80); 
  println("Welcome to the Scala worksheet")




trait BetterException  {
  
  
}

object BetterException {
  
  def apply(message: String): BetterException =  new RuntimeException(message) with BetterException

  def apply(message: String, cause: Throwable = null): BetterException = new RuntimeException(message, cause) with BetterException
  
  def unapply(exception: BetterException with RuntimeException): (Option[String],Option[Throwable],Option[Seq[StackTraceElement]]) = {
      val stack = if(exception.getStackTrace != null) Some(exception.getStackTrace.toSeq) else None
      val message = if(exception.getMessage != null) Some(exception.getMessage) else None
      val cause = if(exception.getCause != null) Some(exception.getCause) else None
      (message, cause, stack)
  }
  
}

case class AccessViolationException(val message: String) extends RuntimeException(message) with BetterException
case class ItemNotFoundException(message: String) extends RuntimeException(message) with BetterException
case class ValidationException(message: String) extends RuntimeException(message) with BetterException;$skip(1121); 


  val ex = AccessViolationException("mymessage");System.out.println("""ex  : controllers.testEx.AccessViolationException = """ + $show(ex ));$skip(27); 
  println(ex.getMessage())}
  


}
