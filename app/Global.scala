import play.api._
import play.api.mvc._
import play.filters.headers.SecurityHeadersFilter



//object Global extends WithFilters(SecurityHeadersFilter()) with GlobalSettings {
object Global extends WithFilters(SecurityHeadersFilter()) with GlobalSettings {

  override def onStart(app: Application) {
     InitialData.insert()
  }
 
  override def onStop(app: Application){
	  Logger.info("shutdown")
  }
}
