package hu.webuni.airport.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.web.reactive.server.WebTestClient;

import hu.webuni.airport.api.model.AirportDto;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase
public class AirportControllerIT {
	
	private static final String BASE_URI="/api/airports";
	
	@Autowired
	WebTestClient webTestClient;
	
	@Test
	void testThatCreatedAirportIsListed() throws Exception {
		
		List<AirportDto> airportsBefore = getAllAirports();
		
		//ez a konstruktor "eltört" miután az AirportDtohoz hozzá lett adva az AdressDto
		//AirportDto newAirport = new AirportDto(5, "asdasd", "IGH");
		
		//mivel az AirportDton van egy NoArgs- és egy AllArgsConstructor annotáció
		//ilyenkor hasznos a @Builder annotáció, ami biztosít egy tetszőleges kostruktor lehetőséget
		//AirportDto newAirport = AirportDto.builder()
		//		.id(5)
		//		.name("asdasd")
		//		.iata("IGH")
		//		.build();
		
		//openapi beveztése után
		AirportDto newAirport = new AirportDto()
				.id(5L)
				.name("asdasd")
				.iata("IGH");
		
		AirportDto savedAirport = createAirport(newAirport);
		newAirport.setId(savedAirport.getId());
		
		List<AirportDto> airportsAfter = getAllAirports();
		
		assertThat(airportsAfter.subList(0, airportsBefore.size()))
			.usingRecursiveFieldByFieldElementComparator()
			.containsExactlyElementsOf(airportsBefore);
		
		assertThat(airportsAfter.get(airportsAfter.size()-1))
			.usingRecursiveComparison()
			.isEqualTo(newAirport);
		
	}

	private AirportDto createAirport(AirportDto newAirport) {
		
		return webTestClient
			.post()
			.uri(BASE_URI)
			.bodyValue(newAirport)
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody(AirportDto.class)
			.returnResult().getResponseBody();
		
	}

	private List<AirportDto> getAllAirports() {
		
		List<AirportDto> responseList = webTestClient
			.get()
			.uri(BASE_URI)
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(AirportDto.class)
			.returnResult().getResponseBody();
		
		Collections.sort(responseList, (a1, a2) -> Long.compare(a1.getId(), a2.getId()));
		
		return responseList;
		
	}

}
