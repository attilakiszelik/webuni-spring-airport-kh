package hu.webuni.airport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;

import hu.webuni.airport.model.Airport;
import hu.webuni.airport.model.Flight;
import hu.webuni.airport.repository.AirportRepository;
import hu.webuni.airport.repository.FlightRepository;

@SpringBootTest
@AutoConfigureTestDatabase
public class FlightServiceIT {

	@Autowired
	FlightService flightService;
	
	@Autowired
	AirportRepository airportRepository;
	
	@Autowired
	FlightRepository flightRepository;
	
	@BeforeEach
	public void init() {
		flightRepository.deleteAll();
		airportRepository.deleteAll();
	}
	
	@Test
	void testCreateFlight() throws Exception {
		String flightNumber = "ABC123";
		long takeoff = createAirport("airport1", "iata1");
		long landing = createAirport("airport2", "iata2");
		LocalDateTime dateTime = LocalDateTime.now();
		long flightId = createFlight(flightNumber, takeoff, landing, dateTime);
		
		Optional<Flight> savedFlightOptional = flightRepository.findById(flightId);
		assertThat(savedFlightOptional).isNotEmpty();
		Flight savedFlight = savedFlightOptional.get();
		assertThat(savedFlight.getFlightNumber()).isEqualTo(flightNumber);
		assertThat(savedFlight.getTakeoffTime()).isCloseTo(dateTime, within(1, ChronoUnit.MICROS));
		assertThat(savedFlight.getTakeoff().getId()).isEqualTo(takeoff);
		assertThat(savedFlight.getLanding().getId()).isEqualTo(landing);
	}

	private long createAirport(String name, String iata) {
		return airportRepository.save(new Airport(name, iata)).getId();
	}
	
	@Test
	void testFindFlightsByExample() throws Exception {
		long airport1Id = createAirport("airport1", "iata1");
		long airport2Id = createAirport("airport2", "iata2");
		long airport3Id = createAirport("airport3", "2iata");
		createAirport("airport4", "3ata1");
		LocalDateTime takeoff = LocalDateTime.of(2021, 4, 23, 8, 0, 0);
		long flight1 = createFlight("ABC123", airport1Id, airport3Id, takeoff);
		long flight2 = createFlight("ABC1234", airport2Id, airport3Id, takeoff.plusHours(2));
		createFlight("BC123", airport1Id, airport3Id, takeoff);
		createFlight("ABC123", airport1Id, airport3Id, takeoff.plusDays(1));
		createFlight("ABC123", airport3Id, airport3Id, takeoff);
		
		Flight example = new Flight();
		example.setFlightNumber("ABC123");
		example.setTakeoffTime(takeoff);
		example.setTakeoff(new Airport("sasa", "iata"));
		List<Flight> foundFlights= this.flightService.findFlightsByExample(example);
		assertThat(foundFlights.stream().map(Flight::getId).collect(Collectors.toList())).containsExactly(flight1, flight2);
	}

	private long createFlight(String flightNumber, long takeoff, long landing, LocalDateTime dateTime) {
		Flight flight = new Flight();
		flight.setFlightNumber(flightNumber);
		flight.setTakeoffTime(dateTime);
		flight.setTakeoff(Airport.builder().id(takeoff).build());
		flight.setLanding(Airport.builder().id(landing).build());
		return flightService.save(flight).getId();
	}
}
