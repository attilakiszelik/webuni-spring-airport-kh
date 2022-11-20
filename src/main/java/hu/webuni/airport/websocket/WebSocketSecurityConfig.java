package hu.webuni.airport.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;

@Configuration
public class WebSocketSecurityConfig extends AbstractSecurityWebSocketMessageBrokerConfigurer{
	
	//ennek az absztrakt osztálynak a sorrendje:
	//@Order(Ordered.HIGHEST_PRECEDENCE + 100)
	//annak a WebSocketConfig osztálynak, amelyben vizsgáltuk az InboundChannel-t
	//hamarabb kell érvényre jutnia, ezért arra rá kell tenni egy:
	//@Order(Ordered.HIGHEST_PRECEDENCE + 99) annotációt

	@Override
	protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
		// TODO Auto-generated method stub
		super.configureInbound(messages);
	}

	
	
}
