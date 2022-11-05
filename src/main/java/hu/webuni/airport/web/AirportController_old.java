package hu.webuni.airport.web;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import javax.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import hu.webuni.airport.api.model.AirportDto;
import hu.webuni.airport.mapper.AirportMapper;
import hu.webuni.airport.model.Airport;
import hu.webuni.airport.model.HistoryData;
import hu.webuni.airport.repository.AirportRepository;
import hu.webuni.airport.service.AirportService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
//@RestController
@RequestMapping("/api/airports")
public class AirportController_old {

	private final AirportService airportService;
	private final AirportRepository airportRepository;
	private final AirportMapper airportMapper;

	@GetMapping
	//ezt a @SortDefault("id")-t azért érdemes mindig beállítani a Pageable típusnál,
	//mert egyébként, ha a kliens oldalról nincs meghatározva a rendezés, akkor előfordulhat, hogy több oldalon is megjelenik majd ugyanaz a találat 
	public List<AirportDto> getAll(@RequestParam Optional<Boolean> full, @SortDefault("id") Pageable pageable) {
		
		//a findAll alapvetően csak egy SQL query, viszont a mappelés során még annyi SQL query fut le, ahány eleme van a listának,
		//mert az address egy ManyToOne típusú kapcsolat, ami by default EAGER típusú betöltés, ezt optimalizálni szükséges
		//erre az egyetlen megoldás egy custom SQL query az AirportRepositoryban (azt meg célszerű közvetlenül hívni, a Service osztály kihagyásával, mert ott semmi nem történne)
		
		//aztán bevezetésre került egy opcionális Boolean paraméter a kérésbe,
		//ha érkezik True értékkel, akkor betöltésre kerülnek a címek
		//ha nem érkezik, vagy False értékkel érkezik, akkor a címek nem kerülnek visszaadásra
		//ehhez az Airport osztályban a ManyToOne annotáció fetch paramétere át lett állítva LAZY-re,
		//hogy ha a findAll fut le, akkor ne kerüljenek betöltésre a címek automatikusan a by default EAGER betöltési típus miatt
		boolean isFull = full.orElse(false);
		List<Airport> airports = isFull
				//? airportRepository.findAllWithAddress()
				//: airportRepository.findAll();
				
				//departures és arrivals listák bevezetése után
				//? airportRepository.findAllWithAddressAndDeparturesAndArrivals()
				//: airportRepository.findAll();
				
				//viszont ennek az eredménye egy Descartes szorzat lesz: M*N sor, ha M departures és N arrivals van az adatbázisban
			    //ezért át kell térni erre:
				//? airportService.findAllWithRelationships()
				//: airportRepository.findAll();
		
				//pageable bevezetése után
				? airportService.findAllWithRelationships(pageable)
						//a Repository így egy Page<> típussal térne vissza, de most nem akarjuk kihasználni a típusban rejlő lehetőségeket,
						//ezért egyszerűen csak a getContent() metódusának azeredményét adjuk vissza
				: airportRepository.findAll(pageable).getContent();
			
		//így viszont a mappelés dob egy LazyInitializationExceptiont, mert az Address tábla lecsatolt állapotban van, mikor mappelni próbálja az airport-hoz az address-t
		//megoldás, hogy létrehozunk egy mappelést, amiben ignoráljuk az address-ek mappelését és itt az isFull függvényében kérjük az eredmény mappelését
		return isFull ? airportMapper.airportsToDtos(airports) : airportMapper.airportSummariesToDtos(airports);
	}

	@GetMapping("/{id}")
	public AirportDto getById(@PathVariable long id) {
		
		Airport airport = airportService.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		
		//a second level cache-elés teszteléséhez itt átállunk
		//return airportMapper.airportToDto(airport);
		return airportMapper.airportSummaryToDto(airport);
		
	}
	
	@GetMapping("/{id}/history")
	public List<HistoryData<AirportDto>> getHistoryById(@PathVariable long id) {
		
		List<HistoryData<Airport>> airports = airportService.getAirportHistory(id);
		
		List<HistoryData<AirportDto>> airportDtosWithHistory = new ArrayList<>();
		
		airports.forEach(a ->{
			airportDtosWithHistory.add(new HistoryData<>(
			   //ha az airport kapcsolatait is visszaadjuk, akkor ehelyett		
			   //airportMapper.airportSummaryToDto(a.getData()),
			   //ezt használjuk
			   airportMapper.airportToDto(a.getData()),
			   a.getRevType(),
			   a.getRevision(),
			   a.getDate()
			));
		});
		
		return airportDtosWithHistory;
		
	}


	@PostMapping
	public AirportDto createAirport(@RequestBody @Valid AirportDto airportDto) {
		
		Airport airport = airportService.save(airportMapper.dtoToAirport(airportDto));
		return airportMapper.airportToDto(airport);
		
	}

	@PutMapping("/{id}")
	public ResponseEntity<AirportDto> modifyAirport(@PathVariable long id, @RequestBody AirportDto airportDto) {
		
		Airport airport = airportMapper.dtoToAirport(airportDto);
		airport.setId(id);
		
		try {
			AirportDto savedAirportDto = airportMapper.airportToDto(airportService.update(airport));
			return ResponseEntity.ok(savedAirportDto);
		} catch (NoSuchElementException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		
	}

	@DeleteMapping("/{id}")
	public void deleteAirport(@PathVariable long id) {
		
		airportService.delete(id);
		
	}
	
}