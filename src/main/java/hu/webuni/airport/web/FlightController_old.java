package hu.webuni.airport.web;

import java.util.List;

import javax.validation.Valid;

import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.querydsl.core.types.Predicate;

import hu.webuni.airport.api.model.FlightDto;
import hu.webuni.airport.mapper.FlightMapper;
import hu.webuni.airport.model.Flight;
import hu.webuni.airport.repository.FlightRepository;
import hu.webuni.airport.service.FlightService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
//@RestController
@RequestMapping("/api/flights")
public class FlightController_old {

	private final FlightService flightService;
	private final FlightMapper flightMapper;
	//@QuerydslPredicate lehetőségeinek kihasználásához
	private final FlightRepository flightRepository;

	@PostMapping
	public FlightDto createFlight(@RequestBody @Valid FlightDto flightDto) {
		Flight flight = flightService.save(flightMapper.dtoToFlight(flightDto));
		return flightMapper.flightToDto(flight);
	}

	//az alapkézésen megismert Criteria API megoldás és
	//a középhaladó képzésen megismert Query DSL megoldás is ezzel hívható
	//viszont a legjobb megoldás, ha egy predicate paramétertel közvetlenül a repository-t hívjuk
	@PostMapping("/search")
	public List<FlightDto> searchFlights(@RequestBody FlightDto example) {
		return flightMapper.flightsToDtos(flightService.findFlightsByExample(flightMapper.dtoToFlight(example)));
	}
	
	//nem szükséges a FlightService osztály findFlightsByExample metódusát megírni (vizsgálni minden egyes paramétert, hogy fel lett-e küldve és ha igen hozzá adni a predicates-hez, stb...) 
	//viszont a visszatérési típus így is Iterable<>, amit kezelni kell: a FlightMapper-ben létre kell hozni egy metódust, amely Iterable<>-ből List<>-et ad vissza (ilyet a mapstruct by default tud)
	//by default pontos egyezést vár el minden felküldött paraméter esetében, viszont ez custom-izálható a flightRepository-ban
	@GetMapping("/search")
	public List<FlightDto> searchFlights_2(@QuerydslPredicate (root = Flight.class) Predicate predicate) {
		return flightMapper.flightsToDtos(flightRepository.findAll(predicate));
	}
	
	@PostMapping("/{flightId}/pollDelay/{rate}")
	public void startDelayPolling(@PathVariable long flightId, @PathVariable long rate) {
		flightService.startDelayPollingForFlight(flightId, rate);
	}
	
	@DeleteMapping("/{flightId}/pollDelay")
	public void startDelayPolling(@PathVariable long flightId) {
		flightService.stopDelayPollingForFlight(flightId);
	}
		
}