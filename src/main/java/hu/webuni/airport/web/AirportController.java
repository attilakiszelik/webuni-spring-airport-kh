package hu.webuni.airport.web;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import javax.validation.Valid;

import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import hu.webuni.airport.api.AirportControllerApi;
import hu.webuni.airport.api.model.AirportDto;
import hu.webuni.airport.api.model.HistoryDataAirportDto;
import hu.webuni.airport.mapper.AirportMapper;
import hu.webuni.airport.mapper.HistoryDataMapper;
import hu.webuni.airport.model.Airport;
import hu.webuni.airport.model.HistoryData;
import hu.webuni.airport.model.Image;
import hu.webuni.airport.repository.AirportRepository;
import hu.webuni.airport.service.AirportService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AirportController implements AirportControllerApi {

	private final AirportService airportService;
	private final AirportRepository airportRepository;
	private final AirportMapper airportMapper;
	private final HistoryDataMapper historyDataMapper;
	
	private final NativeWebRequest nativeWebRequest;
	
	//ez a pageable legyártásához szükséges (springben by default bean-ként regisztrálva van)
	private final PageableHandlerMethodArgumentResolver pageableResolver; 
	
	@Override	
	public Optional<NativeWebRequest> getRequest() {
		
		//return AirportControllerApi.super.getRequest();
		
		//ha beinjektálunk egy NativeWebRequest-et és azt adjuk vissza (Optional.of()-ba kell csomagolni) az automatikusan generált kód helyett,
		//akkor bármilyen kliens oldali request-re kigenerálja dummy adatokkal a választ, ami a gyors prototipizálást segíti (frontend fejlesztés már elkezdhető az alapján)
		return Optional.of(nativeWebRequest);
		
	}

	@Override
	public ResponseEntity<AirportDto> createAirport(@Valid AirportDto airportDto) {
		
		Airport airport = airportService.save(airportMapper.dtoToAirport(airportDto));
		return ResponseEntity.ok(airportMapper.airportToDto(airport));
	
	}

	@Override
	public ResponseEntity<Void> deleteAirport(Long id) {
		
		airportService.delete(id);
		return ResponseEntity.ok().build();
		
	}

	@Override
	public ResponseEntity<AirportDto> getById(Long id) {
		
		Airport airport = airportService.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		return ResponseEntity.ok(airportMapper.airportSummaryToDto(airport));
		
	}

	@Override
	public ResponseEntity<List<HistoryDataAirportDto>> getHistoryById(Long id) {
		
		//openapi generált HistoryDataAirportDto osztályt is, amihez így már célszerűbb létrehozni egy mapper osztályt
		
		List<HistoryData<Airport>> airports = airportService.getAirportHistory(id);
		
		List<HistoryDataAirportDto> result = new ArrayList<>();
		
		airports.forEach(hd ->{
			result.add(historyDataMapper.airportHistoryDatatoDto(hd));
		});
		
		return ResponseEntity.ok(result);
		
	}

	@Override
	public ResponseEntity<AirportDto> modifyAirport(Long id, @Valid AirportDto airportDto) {
		
		Airport airport = airportMapper.dtoToAirport(airportDto);
		airport.setId(id);
		
		try {
			AirportDto savedAirportDto = airportMapper.airportToDto(airportService.update(airport));
			return ResponseEntity.ok(savedAirportDto);
		} catch (NoSuchElementException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		
	}

	//ezzel az üres metódussal tulajdonképpen csak beállítjuk, hogy a Pageable deafult sort-olási metódusa id alapján történik
	//bármi egyéb konfiguráció is beállítható lenne ezen keresztül, de az alap kódunkban is csak ez volt...
	public void configPageable (@SortDefault("id") Pageable pagebale) {}
	
	//kódból készült az openapi leíró fájl, annak generálásakor tévesen került felimerésre modellként ennek a metódusnak a "@SortDefault("id") Pageable pageable" paramétere
	//a pageable és sort modelleket ki kell törölni Stoplight-ban, majd a getAll metódusnál felvenni request paraméterként: page és size Integer típusú,
	//amíg a sort Array (majd kód nézetben a "type: array" alá be kell írni, hogy "items: type: String")
	@Override
	public ResponseEntity<List<AirportDto>> getAll(@Valid Boolean full, @Valid Integer page, @Valid Integer size,
			@Valid List<String> sort) {
		
		//a saját magunk írt kódban a full paraméter egy Optional<Boolean> volt,
		//itt meg sima Boolean-ként került legenerálásra, ezért az alábbiak szerint kell módosítani: 
		//boolean isFull = full.orElse(false);
		boolean isFull = full == null ? false : full;
		
		//pageable most nincs, viszont a page, a size és a sort tulajdonképpen leír egy pageable-t, úgyhogy le lehet gyártani!
		//ez a legyártás a createPageable metódusban történik, aminek paraméterként kerül átadásra a pageable konfigurálására szolgáló metódus neve
		Pageable pageable = createPageable("configPageable");
		
		List<Airport> airports = isFull

				? airportService.findAllWithRelationships(pageable)
		
				: airportRepository.findAll(pageable).getContent();
			
		List<AirportDto> result = isFull
				
				? airportMapper.airportsToDtos(airports)
						
				: airportMapper.airportSummariesToDtos(airports);
		
		return ResponseEntity.ok(result);
		
	}

	public Pageable createPageable(String pageableConfigureMethodName) {
		
		//ehhez injektálni kell egy PageableHandlerMethodArgumentResolver-t, ami négy paramétert vár el:
		// -> ezekből a NativeWebRequest már injektálva van, a ModelAndViewContainer és a WebDataBinderFactory pedig valójában nem szükséges ( = nulll )
		//így csak a MethodParametert kell ténylegesen előállítani!
		
		//ehhez fel kell venni egy method-ot
		Method method;
		try {
			//amire be kell állítani a létrehozott configPageable metódusunk nevét (ez érkezik paraméterként)
			//első paraméter a metódus neve, továbbiak pedig a metódus paramétereinek osztályai (jelen esetben csak egy Pageable van)
			method = this.getClass().getMethod(pageableConfigureMethodName, Pageable.class);
		} catch (NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		
		//ezt a method-ot kell átadni a MethodParamter-nek
		//a MethodParameter azt írja le, hogy a Pageable típusú metódus-argumentum -> melyik kontroller metódus -> hányadik argumentuma
		//ennek ismeretében vizsgálja meg, hogy azon az argumentumon van-e egyéb annotáció, pl. az általunk használt id szerinti default sortolás 
		//de ezt csak akkor tudja megtenni, ha tudja hogy a metódus hányadik argumontumán kell keresnie az egyéb annotációkat 
		MethodParameter methodParameter = new MethodParameter(method, 0);
		
		//ezeket elég null-ként inicializálni
		ModelAndViewContainer mavContainer = null;
		WebDataBinderFactory binderFactory = null;
		
		//és ezután összeállítható a pageable:
		Pageable pageable = pageableResolver.resolveArgument(methodParameter, mavContainer, nativeWebRequest, binderFactory);
		
		return pageable;
		
	}
	
//	FÁJL FELTÖLTÉS - openapi leíróban az alábbiak lettek beállítva:
//
//	'/api/airports/{id}/image':
//	    parameters:
//	      - schema:
//	          type: integer
//	          format: int64
//	        name: id
//	        in: path
//	        required: true
//	    post:
//	      tags:
//	        - airport-controller
//	      summary: ''
//	      operationId: uploadImageForAirport
//	      requestBody:
//	        content:
//	          multipart/form-data:
//	            schema:
//	              type: object
//	              properties:
//	                fileName:
//	                  type: string
//	                content:
//	                  type: string
//	                  format: binary
//	        description: ''
//	      responses:
//	        '200':
//	          description: OK
//	          headers: {}
//	          content:
//	            application/json:
//	              schema:
//	                type: string
	
	@Override
	public ResponseEntity<String> uploadImageForAirport(Long id, @Valid String fileName, MultipartFile content) {

		Image image;
		try {
			image = airportService.saveImageForAirport(id, fileName, content.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		
		return ResponseEntity.ok("/api/images/" + image.getId());
		
	}
	
}
