package hu.webuni.airport.websocket;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import hu.webuni.airport.security.JwtAuthFilter;
import hu.webuni.airport.security.JwtService;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebsocketConfig implements WebSocketMessageBrokerConfigurer {
	
	private final JwtService jwtService;

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
	
	//JWC autentikáció hozzáadása a websocketen keresztül történő kommunikációhoz
	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {

		//ez a metódus bemenő paraméterként kap egy csatorna regisztrációt,
		//aminek az interceptorai közé felveszünk egy új csatorna interceptort
		registration.interceptors(
			
				new ChannelInterceptor() {

					//amiben felüldefiniáljuk az alábbi metódust, annak érdekében, hogy
					//beavatkozhassunk a bejövő üzenetek esetében, mielőtt a springen belül tovább küldésre kerülnének
					@Override
					public Message<?> preSend(Message<?> message, MessageChannel channel) {
	
						//header kinyerése a MessageHeaderAccessor osztályon keresztül történik						
						//annak át kell adni magát az üzenetet és meg kell adni, hogy a (projektünkben használt) üzenet fejléc típusa: stomp
						StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
						
						//ha az accessor (azaz a fejléc) parancsai között van CONNECT típusú stomp parancs
						if (StompCommand.CONNECT.equals(accessor.getCommand())) {
							
							//akkor kinyerjük az "X-Authorization" nevű (CONNECT típúsú) headert
							//(a frontendben az lett beállítva, hogy egy ilyen nevű headerben küldj fel a JWT tokent)
							List<String> authHeaders = accessor.getNativeHeader("X-Authorization");
							
							//innentől a JWT tokent már ugyanúgy kezeljük, mint egy http requestnél -> létrehozunk belőle egy UserDetailst
							//(a JwtAuthFilter osztályban létrehoztunk erre egy újrafelhasználható metódust (JwtAuthFilter-t nem injektáljuk tagváltozóként!)
							UsernamePasswordAuthenticationToken authentication = JwtAuthFilter.createUserDetailsFromAuthHeader(authHeaders.get(0), jwtService);
							//ezt pedig az accessor objektum user változóján keresztül adjuk át a spring security részére későbbi feldolgozásra 
							accessor.setUser(authentication);
							
							//ahhoz, hogy a spring security érzékelje ezt szükséges egy WebSocketSecurityConfig osztály...
							
						}
						
						//a végén (módosítás nélkül) tovább engedjük az üzenetet (csak a header kinyerése volt a cél)
						return message;
					}
				
				
				
			}	
		);
	}

	
}
