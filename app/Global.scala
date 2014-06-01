import play.api._
import play.api.mvc._
import play.filters.headers.SecurityHeadersFilter



//object Global extends WithFilters(SecurityHeadersFilter()) with GlobalSettings {
object Global extends GlobalSettings {

  override def onStart(app: Application) {
	  Logger.info("starting up")
      InitialData.insert()
  }
 
  override def onStop(app: Application){
	  Logger.info("shutdown")
  }
}
