package hu.webuni.airport.web;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import javax.validation.Valid;

import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.data.web.querydsl.QuerydslPredicateArgumentResolver;
import org.springframework.http.ResponseEntity;
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
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class FlightController implements FlightControllerApi{

	private final NativeWebRequest nativeWebRequest;
	private final FlightService flightService;
	private final FlightMapper flightMapper;
	private final FlightRepository flightRepository;
	
	private final QuerydslPredicateArgumentResolver querydslResolver;
	
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
	
	//kódból készült az openapi leíró fájl, annak generálásakor tévesen került felimerésre modellként ennek a metódusnak a "QuerydslPredicate predicate" paramétere
	//a predicate modellt ki kell törölni Stoplight-ban, majd a searchFlights2 metódusnál fel kell venni request paraméterként az alábbiakat:
		//id - Integer (other propertiesben a format int64, ami a javában használt long)
		//flightNumber - String
		//takeoff.iata - String
		//takeoffTime - Array (majd kód nézetben a "type: array" alá be kell írni, hogy "items: type: String")
	//a megoldás pedig nagyon hasonló az AirportController-ben használthoz
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

}
