package controllers

import play.api.Configuration
import javax.inject.Inject
import play.api.http._
import play.api.mvc.RequestHeader
import play.api.routing.Router
import scala.annotation.implicitNotFound

class RequestHandler @Inject() (config: Configuration, router: Router, errorHandler: HttpErrorHandler, configuration: HttpConfiguration, filters: HttpFilters )
     extends DefaultHttpRequestHandler( router, errorHandler, configuration, filters ) {

  val prefix = config.getOptional[String]("betterplay.prefix").map(p => "/"+p).getOrElse("")  
  
  override def routeRequest(request: RequestHeader) = {
    val strippedPath = request.path.stripPrefix(prefix)
    val strippedUri = request.uri.stripPrefix(prefix)
    val strippedRequest = request.copy(uri = strippedUri, path = strippedPath )
    super.routeRequest(strippedRequest)
    
  }
  
}