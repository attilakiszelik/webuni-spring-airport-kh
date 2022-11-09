package hu.webuni.airport.config;

import javax.xml.ws.Endpoint;

import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import hu.webuni.airport.xmlws.AirportXmlWs;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class WebServiceConfig {

	private final Bus bus;
	private final AirportXmlWs airportXmlWs;
	
	@Bean
	//itt a javax.xml.ws.endpoint került importálásra
	public Endpoint endpoint() {
		
		//itt az org.apache.cxf.jaxws került importálásra
		EndpointImpl endpoint = new EndpointImpl(bus, airportXmlWs);
		
		//ez által lesz elérhető a localhost:8080/services/airport url-en a webszolgáltatásunk
		//localhost:8080/services/airport?wsdl url-en elérhető maga a web szolgáltatást leíró dokumentum
		endpoint.publish("/airport");
		
		return endpoint;
	}
	
}
