package hu.webuni.airport.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import hu.webuni.airport.model.Airport;

public interface AirportRepository extends JpaRepository<Airport, Long>{
	
	Long countByIata(String iata);
	
	Long countByIataAndIdNot(String iata, long id);
	
	//-----ManyToOne-----
	
		//a join-olásra két lehetőség van, az egyik, hogy magában az query-ben join-olunk:
		//@Query("SELECT a FROM Airport a LEFT JOIN FETCH a.address")
		//a másik pedig, hogy Entity gráfban kérjük az eredményt, aminek az attributePaths-jába beállítjuk az address-t
		@EntityGraph(attributePaths= {"address"})
		@Query("SELECT a FROM Airport a")
		List<Airport> findAllWithAddress();
	
	//-----OneToMany-----
	
		//2. probléma ha egynél több OneToMany kapcsolat van az osztályban, hogy így 1 SQL query fog csak lefutni,
		//de az eredmény egy Descartes szorzat lesz: M*N sor, ha M departures és N arrivals van az adatbázisban
		@EntityGraph(attributePaths= {"address", "departures", "arrivals"})
		@Query("SELECT a FROM Airport a")
		List<Airport> findAllWithAddressAndDeparturesAndArrivals();
		
		//ezért el kell enegdni a "1 http lekérdezés 1 SQL queryben kerüljön lekérdezésre" megközelítést,
		//mert ez a Descrates szorzat eredmény károsabb erőforrás szempontból, mint több SQL query
		//megoldás Controller-ből áthívás Service osztályba, ahonnan az alábbi SQL query-k meghívása egy tranzakción belül
		
			//az address és a departures együtes lekérdezése még nem okoz Descrates szorzat eredményt, mert az address ManyToOne típusú (egy címe van)
			@EntityGraph(attributePaths= {"address", "departures"})
			@Query("SELECT a FROM Airport a")
			List<Airport> findAllWithAddressAndDepartures();
			
			@EntityGraph(attributePaths= {"arrivals"})
			@Query("SELECT a FROM Airport a")
			List<Airport> findAllWithArrivals();
			
			//pageable bevezetése esetén célszerű ezeket is átszervezni, hogy elkerüljük az in memory lapozást
			//(a collection fetch-elése miatt nem tud a pageable limitje és offsetje belegenerálódni az SQL querybe ->
			//betöltésre kerül az összes DB sor és memóriában kerül meghatározásra, hogy hány sor kerül ténylegesen visszaadásra, amivel így elveszítjük a pageable előnyét)
			
			@EntityGraph(attributePaths= {"address"})
			@Query("SELECT a FROM Airport a")
			List<Airport> findAllWithAddress(Pageable pageable);

			@EntityGraph(attributePaths= {"arrivals"})
			@Query("SELECT a FROM Airport a WHERE a.id IN :ids")
			List<Airport> findByIdWithArrivals(List<Long> ids);
			
			@EntityGraph(attributePaths= {"departures"})
			@Query("SELECT a FROM Airport a WHERE a.id IN :ids")
			List<Airport> findByIdWithDepartures(List<Long> ids, Sort sort);		

}