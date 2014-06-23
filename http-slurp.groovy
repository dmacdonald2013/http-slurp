/**
* An extremely simple Groovy script to capture HTTP requests and responses going to a target system.
*
* Modify the slurpPoints map below to specify endpoints to proxy, e.g. "google":"http://www.google.com"
* will proxy all requests to http://host:8080/google through to http://www.google.com, capturing the request/response
* body pair under the directory 'google'. This also handles complete URL patterns,
* e.g. http://host:8080/google/foo?baz=true will be proxied through to http://www.google.com/foo?baz=true.
*/

@Grab(group='org.apache.camel', module='camel-core', version='2.13.1')
@Grab(group='org.apache.camel', module='camel-jetty', version='2.13.1')
@Grab('org.slf4j:slf4j-simple:1.6.6')

import org.apache.camel.builder.RouteBuilder
import org.apache.camel.CamelContext
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.component.properties.PropertiesComponent

def slurpPoints = [ "google" : "http://www.google.com" ]
def port = 8080
def filePath = "."

CamelContext context = new DefaultCamelContext()
context.addRoutes(new RouteBuilder() {
	public void configure() {
		slurpPoints.each() { targetSystem,targetUrl ->
			from("jetty:http://0.0.0.0:$port/$targetSystem?matchOnUriPrefix=true")
				.convertBodyTo(String.class)
					.choice().when(body().isNull()).setBody(constant("")).end()
					.to("file:$filePath/$targetSystem?fileName=\${exchangeId}-request")
					.to("jetty:" + targetUrl + "?bridgeEndpoint=true")
					.to("file:$filePath/$targetSystem?fileName=\${exchangeId}-response")
		}
	}
})

context.start()
addShutdownHook{ context.stop() }
synchronized(this){ this.wait() }