package hu.webuni.airport.config;

import org.apache.activemq.broker.BrokerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class JmsConfig {

	//erre azért van szükség, hogy a jms tudja, hogy json-re/ről kell konvertálnia az üzenetet
	@Bean
	public MessageConverter jacksonJmsMessageConverter(ObjectMapper objectMapper) {
		
		MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
		//ha ezt nem állítjuk be, akkro nem tudja szerializni a OffsetDateTime-ot
		converter.setObjectMapper(objectMapper);
		//json típus beállítása szerializációkor
		converter.setTargetType(MessageType.TEXT);
		//json típus beállítása deszerializációkor
		converter.setTypeIdPropertyName("_type");
		return converter;
		
	}
	
	//az activemq függőség miatt magában az alkalmazásban, beágyazva fog futni az activemq
	//tehát nem kell egy önálló activemq szervert futtatni
	//viszont ahhoz, hoyg erre a kliensek fel tudjanak íratkozni, ki kell publikálni egy TCP porton
	@Bean
	public BrokerService brokerService() throws Exception {
		
		BrokerService brokerService = new BrokerService();
		brokerService.addConnector("tcp://localhost:9999");
		//by defaul perzisztensen tárolja az üzeneteket, amihez elvárja az activemq-kahadb-store függőséget,
		//ha nem szeretnénk az üzenetek perzisztens tárolását, akkor kikapcsolható
		//brokerService.setPersistent(false);
		return brokerService;
		
	}
}
