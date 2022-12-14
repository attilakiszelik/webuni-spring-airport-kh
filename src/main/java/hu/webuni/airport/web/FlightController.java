package hu.webuni.airport.web;

import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import javax.validation.Valid;

import org.springframework.core.MethodParameter;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.data.web.querydsl.QuerydslPredicateArgumentResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.querydsl.core.types.Predicate;

import hu.webuni.airport.api.FlightControllerApi;
import hu.webuni.airport.api.model.FlightDto;
import hu.webuni.airport.mapper.FlightMapper;
import hu.webuni.airport.model.Flight;
import hu.webuni.airport.repository.FlightRepository;
import hu.webuni.airport.service.FlightService;
import hu.webuni.airport.websocket.DelayMessage;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class FlightController implements FlightControllerApi{

	private final FlightMapper flightMapper;
	private final FlightService flightService;
	private final FlightRepository flightRepository;
	
	private final NativeWebRequest nativeWebRequest;
	
	private final QuerydslPredicateArgumentResolver querydslResolver;
	
	private final SimpMessagingTemplate messagingTemplate;
	
	private final JmsTemplate jmsTemplate;
	
	@Override
	public Optional<NativeWebRequest> getRequest() {
		return Optional.of(nativeWebRequest);
	}

	@Override
	public ResponseEntity<FlightDto> createFlight(@Valid FlightDto flightDto) {
		Flight flight = flightService.save(flightMapper.dtoToFlight(flightDto));
		return ResponseEntity.ok(flightMapper.flightToDto(flight));
	}

	@Override
	public ResponseEntity<List<FlightDto>> searchFlights(@Valid FlightDto example) {
		return ResponseEntity.ok(flightMapper.flightsToDtos(flightService.findFlightsByExample(flightMapper.dtoToFlight(example))));
	}

	@Override
	public ResponseEntity<Void> startDelayPolling(Long flightId, Long rate) {
		flightService.startDelayPollingForFlight(flightId, rate);
			return ResponseEntity.ok().build();
	}

	@Override
	public ResponseEntity<Void> stopDelayPolling(Long flightId) {
		flightService.stopDelayPollingForFlight(flightId);
			return ResponseEntity.ok().build();
	}

	public void configPredicate(@QuerydslPredicate (root = Flight.class) Predicate predicate) {}
	
	//k??db??l k??sz??lt az openapi le??r?? f??jl, annak gener??l??sakor t??vesen ker??lt felimer??sre modellk??nt ennek a met??dusnak a "QuerydslPredicate predicate" param??tere
	//a predicate modellt ki kell t??r??lni Stoplight-ban, majd a searchFlights2 met??dusn??l fel kell venni request param??terk??nt az al??bbiakat:
		//id - Integer (other propertiesben a format int64, ami a jav??ban haszn??lt long)
		//flightNumber - String
		//takeoff.iata - String
		//takeoffTime - Array (majd k??d n??zetben a "type: array" al?? be kell ??rni, hogy "items: type: String")
	//a megold??s pedig nagyon hasonl?? az AirportController-ben haszn??lthoz
	@Override
	public ResponseEntity<List<FlightDto>> searchFlights2(@Valid Long id, @Valid String flightNumber,
			@Valid String takeoffIata, @Valid List<String> takeoffTime) {
		
		Predicate predicate = createPredicate("configPredicate");
		List<FlightDto> result = flightMapper.flightsToDtos(flightRepository.findAll(predicate));
		return ResponseEntity.ok(result);
	}

	private Predicate createPredicate(String configurePredicateMethodName) {
		
		try {
			
			Method method = this.getClass().getMethod(configurePredicateMethodName, Predicate.class);
			
			MethodParameter methodParameter = new MethodParameter(method, 0);
			ModelAndViewContainer mavContainer = null;
			WebDataBinderFactory binderFactory = null;
			
			return (Predicate) querydslResolver.resolveArgument(methodParameter, mavContainer, nativeWebRequest, binderFactory);
			
		} catch (Exception  e) {
			
			e.printStackTrace();
			throw new RuntimeException(e);
			
		}
		
	}

//	WEBSOCKET - openapi le??r??ban az al??bbiak lettek be??ll??tva:
//	
//	 '/api/flights/{id}/delay/{delay}':
//		    parameters:
//		      - schema:
//		          type: integer
//		          format: int64
//		        name: id
//		        in: path
//		        required: true
//		      - schema:
//		          type: integer
//		          format: int32
//		        name: delay
//		        in: path
//		        required: true
//		    put:
//		      summary: ''
//		      operationId: reportDelay
//		      responses:
//		        '200':
//		          description: OK
//		      tags:
//		        - flight-controller
	
	@Override
	public ResponseEntity<Void> reportDelay(Long id, Integer delay) {

		//ez a met??dus egy ??res ResponseEntity.ok()-val t??r vissza, de el??tte m??g sz??t broadcast-ol??sra ker??l a j??ratra felk??ld??tt k??s??s
		//param??terek:
		//1., a topic: amelyre feliratkozott kliensek fogj??k majd l??tni az ??zenetet
		//2., az ??zenet: ez egy saj??t oszt??ly, tetsz??leges tartalommal, most maga a k??s??s lesz benne ??s annak bek??ld??si ideje
		DelayMessage payload = new DelayMessage(delay, OffsetDateTime.now(), id);
		this.messagingTemplate.convertAndSend("/topic/delay/" + id, payload);
		
		//ALKALMAZ??S INTEGR??CI?? ASZINKRON ??ZENETSORON KERESZT??L
		
		//ha az activemq nincs k??l??n konfigolva, akkor by deafult queue-ba k??ldi ki az ??zenetet
		//queue (??zenetsor): destrukt??v kiolvas??s -> olvas?? kiolvas??s ut??n k??ld egy "acknowledges" ??zenetet a sornak, aminek hat??s??ra az ??zenet t??rl??sre ker??l,
		//                   ez??rt csak egy olvas?? tudja azt kiolvasni (k??ldeni t??bben is tudnak) //client1 - sends, client2 - consumes
		//topic: t??bben is kitudj??k olvasni ugyanazt az ??zenetet //client1 - publish, client2,3,n - subscribe
		
		//els?? param??ter a queue/topic neve, amire majd a kliensek feliratkoznak, m??sodik maga az ??zenet
		//a DelayMessage oszt??ly kieg??sz??t??sre ker??lt FlightId-val, mert itt nincs minden egyes flightnak saj??t topicja 
		this.jmsTemplate.convertAndSend("delays", payload);
		
		return ResponseEntity.ok().build();
		
	}
	

	
	

}
