package hu.webuni.airport.service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hu.webuni.airport.model.Address;
import hu.webuni.airport.model.Airport;
import hu.webuni.airport.model.HistoryData;
import hu.webuni.airport.model.Image;
import hu.webuni.airport.repository.AirportRepository;
import hu.webuni.airport.repository.ImageRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class AirportService {

	private final AirportRepository airportRepository;
	private final ImageRepository imageRepository;
	
	@PersistenceContext
	private EntityManager em;
	
	@Transactional
	public Airport save(Airport airport) {
		checkUniqueIata(airport.getIata(), null);
		return airportRepository.save(airport);
	}
	
	@Transactional
	public Airport update(Airport airport) {
		checkUniqueIata(airport.getIata(), airport.getId());
		if(airportRepository.existsById(airport.getId())) {
			return airportRepository.save(airport);
		}
		else
			throw new NoSuchElementException();
	}
	
	private void checkUniqueIata(String iata, Long id) {
		
		boolean forUpdate = id != null;
		Long count = forUpdate ?
				airportRepository.countByIataAndIdNot(iata, id)
				:airportRepository.countByIata(iata);
		
		if(count > 0)
			throw new NonUniqueIataException(iata);
	}
	
	public List<Airport> findAll(){
		return airportRepository.findAll();
	}
	
	public Optional<Airport> findById(long id){
		return airportRepository.findById(id);
	}
	
	@Transactional
	public void delete(long id) {
		airportRepository.deleteById(id);
	}
	

	@Transactional
	@Cacheable("pagedAirportsWithRelations")
	public List<Airport> findAllWithRelationships(Pageable pageable){
		
		//pageable bevezetése előtt
		
		//List<Airport> airports = airportRepository.findAllWithAddressAndArrivals();
		//airports = airportRepository.findAllWithDepartures();
		
		//pageable bevezetése után
		
		List<Airport> airports = airportRepository.findAllWithAddress(pageable);
		List<Long> airportIds = airports.stream().map(Airport::getId).toList();
		airports = airportRepository.findByIdWithArrivals(airportIds);
		//ha nem adjuk át a pageable-ben meghatározott rendezést, a kért sorrend helyett,
		//egy nem-definiált sorrendben kapjuk vissza a lapra eső entitásoka
		//(az nem tiszta, hogy ezt miért csak a departures esetébn adjuk át?!)
		airports = airportRepository.findByIdWithDepartures(airportIds, pageable.getSort());
		return airports;
	
	}
	
	@Transactional
	//ezzel lehet a warning-okat megszüntetni
	@SuppressWarnings({"rawtypes","unchecked"})
	public List<HistoryData<Airport>> getAirportHistory(long id){
		
		List resultList = AuditReaderFactory.get(em)
							.createQuery()
							//1. boolean: csak az entitásokat akarjuk-e látni (vagy a revision-ök minden adatát is) (selectEntitiesOnly)
							//2. boolean: a törölt sorokat akarjuk-e látni (selectDeletedEntities)
							.forRevisionsOfEntity(Airport.class, false, true)
							.add(AuditEntity.property("id").eq(id))
							//ha szeretnénk az entity kapcsolatait is visszaadni (pl. address vagy departures / arrivals)
							//egy lehetséges megoldás lenne a .traverseRelation("address", JoinType.LEFT),
							//de az csak .forEntitiesOfReversion esetén használható...
							.getResultList()
							.stream().map(o->{
								Object[] objArray = (Object[])o;
								DefaultRevisionEntity revisionEntity = (DefaultRevisionEntity) objArray[1];
								Airport airport = (Airport) objArray[0];
								//..ezért a megoldás, hogy miután megvan az Airport kényszerítjük a kapcsolatok betöltését
								//(egy getter még önmagában nem kényszeríti, kell még azon belül valamit hívni)
								//airport.getAddress().getId();
								//viszont, ha nincs címe az airportnak, ez hibára fog futni,
								//szóval kicsit érzékenyebben kell ezt a betöltést kényszeríteni :D								
								Address address = airport.getAddress();
								if (address != null)
									airport.getAddress().getId();
								airport.getArrivals().size();
								airport.getDepartures().size();
								return new HistoryData<Airport>(airport,
										 						(RevisionType) objArray[2],
																revisionEntity.getId(),
																revisionEntity.getRevisionDate());
							}).toList();
		
		return resultList;
		
	}
	
	@Transactional
	public Image saveImageForAirport(Long airportId, String fileName, byte[] bytes) {
		
		Airport airport = airportRepository.findById(airportId).get();
		
		Image image = Image.builder()
				.fileName(fileName)
				.data(bytes)
				.build();
		
		image = imageRepository.save(image);
		
		//ez itt adatforgalomi szempontból nagyon nem hatékony megoldás,
		//mert ilyenkor a repülőtérhez tartozó összes kép (byte tömb) betöltsére kerül, azért, hogy egy újabb kép hozzáadásra kerüljön
		//megoldás: fel kellene venni az image-ben egy @ManyToOne kapcsolatot, ami az airport-ra  mutat,
		//azon keresztül lehetséges lenne az image-ben beállítani, hogy melyik airport-hoz tartozik, szóval az image irányaból menedzselni a mentést
		airport.getImages().add(image); 
		
		return image;
		
	}
	
}