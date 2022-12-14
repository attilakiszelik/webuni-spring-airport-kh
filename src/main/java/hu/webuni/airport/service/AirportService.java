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
		
		//pageable bevezet??se el??tt
		
		//List<Airport> airports = airportRepository.findAllWithAddressAndArrivals();
		//airports = airportRepository.findAllWithDepartures();
		
		//pageable bevezet??se ut??n
		
		List<Airport> airports = airportRepository.findAllWithAddress(pageable);
		List<Long> airportIds = airports.stream().map(Airport::getId).toList();
		airports = airportRepository.findByIdWithArrivals(airportIds);
		//ha nem adjuk ??t a pageable-ben meghat??rozott rendez??st, a k??rt sorrend helyett,
		//egy nem-defini??lt sorrendben kapjuk vissza a lapra es?? entit??soka
		//(az nem tiszta, hogy ezt mi??rt csak a departures eset??bn adjuk ??t?!)
		airports = airportRepository.findByIdWithDepartures(airportIds, pageable.getSort());
		return airports;
	
	}
	
	@Transactional
	//ezzel lehet a warning-okat megsz??ntetni
	@SuppressWarnings({"rawtypes","unchecked"})
	public List<HistoryData<Airport>> getAirportHistory(long id){
		
		List resultList = AuditReaderFactory.get(em)
							.createQuery()
							//1. boolean: csak az entit??sokat akarjuk-e l??tni (vagy a revision-??k minden adat??t is) (selectEntitiesOnly)
							//2. boolean: a t??r??lt sorokat akarjuk-e l??tni (selectDeletedEntities)
							.forRevisionsOfEntity(Airport.class, false, true)
							.add(AuditEntity.property("id").eq(id))
							//ha szeretn??nk az entity kapcsolatait is visszaadni (pl. address vagy departures / arrivals)
							//egy lehets??ges megold??s lenne a .traverseRelation("address", JoinType.LEFT),
							//de az csak .forEntitiesOfReversion eset??n haszn??lhat??...
							.getResultList()
							.stream().map(o->{
								Object[] objArray = (Object[])o;
								DefaultRevisionEntity revisionEntity = (DefaultRevisionEntity) objArray[1];
								Airport airport = (Airport) objArray[0];
								//..ez??rt a megold??s, hogy miut??n megvan az Airport k??nyszer??tj??k a kapcsolatok bet??lt??s??t
								//(egy getter m??g ??nmag??ban nem k??nyszer??ti, kell m??g azon bel??l valamit h??vni)
								//airport.getAddress().getId();
								//viszont, ha nincs c??me az airportnak, ez hib??ra fog futni,
								//sz??val kicsit ??rz??kenyebben kell ezt a bet??lt??st k??nyszer??teni :D								
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
		
		//ez itt adatforgalomi szempontb??l nagyon nem hat??kony megold??s,
		//mert ilyenkor a rep??l??t??rhez tartoz?? ??sszes k??p (byte t??mb) bet??lts??re ker??l, az??rt, hogy egy ??jabb k??p hozz??ad??sra ker??lj??n
		//megold??s: fel kellene venni az image-ben egy @ManyToOne kapcsolatot, ami az airport-ra  mutat,
		//azon kereszt??l lehets??ges lenne az image-ben be??ll??tani, hogy melyik airport-hoz tartozik, sz??val az image ir??nyab??l menedzselni a ment??st
		airport.getImages().add(image); 
		
		return image;
		
	}
	
}