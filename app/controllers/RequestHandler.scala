package controllers

import play.api.Logger
import play.api.Configuration
import javax.inject.Inject
import play.api.http._
import play.api.mvc.RequestHeader
import play.api.routing.Router
import scala.annotation.implicitNotFound
import play.core.WebCommands
import play.api.OptionalDevContext


class RequestHandler @Inject() (webCommands: WebCommands, optionalDevContext: OptionalDevContext, config: Configuration, router: Router, errorHandler: HttpErrorHandler, configuration: HttpConfiguration, filters: HttpFilters )
     extends DefaultHttpRequestHandler( webCommands, optionalDevContext, router, errorHandler, configuration, filters ) {
   
    val logger: Logger = Logger(this.getClass())
    val prefix = config.getOptional[String]("betterplay.prefix").map(p => "/"+p).getOrElse("")  
  
  override def routeRequest(request: RequestHeader) = {
    val strippedPath = request.path.stripPrefix(prefix)
    val strippedUri = request.uri.stripPrefix(prefix)
    val target = request.target.withPath(strippedPath).withUri(new java.net.URI(strippedUri))
    val strippedRequest = request.withTarget(target)
    super.routeRequest(strippedRequest)
    
  }
  
}