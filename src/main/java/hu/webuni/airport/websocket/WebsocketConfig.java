package hu.webuni.airport.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebsocketConfig implements WebSocketMessageBrokerConfigurer {

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		
		//itt adjuk meg, hogy milyen url-en kell becsatlakozniuk a klienseknek a websocketükkel
		registry.addEndpoint("/api/stomp");
		//hogy a SockJS-es endpoint-ú kliensek is tudjanak csatlakozni
		registry.addEndpoint("/api/stomp").withSockJS();
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {

		//ez állítja be, hogy a /topic kezdetű url-ekre érkezett üzeneteket azonnal továbbítja az arra felíratkozott klienseknek
		//itt elég a prefixet megadni, és onnantól kezdve az alatt már minden végpontra működni fog a broadcast-olás
		registry.enableSimpleBroker("/topic");
		//ha lenne a projektnek olyan része, amikor a kliensek küldhetnének fel lekezelendő websocket kéréseket, akkor lenne erre szükség
		registry.setApplicationDestinationPrefixes("/app");
			
	}
	
}
