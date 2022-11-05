package hu.webuni.airport.service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.google.common.collect.Lists;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;

import hu.webuni.airport.aspect.LogCall;
import hu.webuni.airport.model.Airport;
import hu.webuni.airport.model.Flight;
import hu.webuni.airport.model.QFlight;
import hu.webuni.airport.repository.AirportRepository;
import hu.webuni.airport.repository.FlightRepository;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

@RequiredArgsConstructor
@Service
@LogCall
public class FlightService {
	
	private final AirportRepository airportRepository;
	private final FlightRepository flightRepository;
	private final DelayService delayService;
	
	private final TaskScheduler taskScheduler;
	//konkurens HashMap annyival több a simánál, hogy ez akár több szálon is meghívódhat
	private Map<Long, ScheduledFuture<?>> delayPollerJobs = new ConcurrentHashMap();
	
	@Transactional
	public Flight save(Flight flight) {
		//a takeoff/landing airportból csak az id-t vesszük figyelembe, amiknek már létezniük kell
		flight.setTakeoff(airportRepository.findById(flight.getTakeoff().getId()).get());
		flight.setLanding(airportRepository.findById(flight.getLanding().getId()).get());
		return flightRepository.save(flight);
	}
	
	//alapképzésen megismert Criteria API megoldás 
//	public List<Flight> findFlightsByExample(Flight example) {
//
//		long id = example.getId();
//		String flightNumber = example.getFlightNumber();
//		String takeoffIata = null;
//		Airport takeoff = example.getTakeoff();
//		if (takeoff != null)
//			takeoffIata = takeoff.getIata();
//		LocalDateTime takeoffTime = example.getTakeoffTime();
//
//		Specification<Flight> spec = Specification.where(null);
//
//		if (id > 0) {
//			spec = spec.and(FlightSpecifications.hasId(id));
//		}
//
//		if (StringUtils.hasText(flightNumber))
//			spec = spec.and(FlightSpecifications.hasFlightNumber(flightNumber));
//
//		if (StringUtils.hasText(takeoffIata))
//			spec = spec.and(FlightSpecifications.hasTakoffIata(takeoffIata));
//
//		if (takeoffTime != null)
//			spec = spec.and(FlightSpecifications.hasTakoffTime(takeoffTime));
//
//		return flightRepository.findAll(spec, Sort.by("id"));
//	}
	
	//Query DSL megoldás
	public List<Flight> findFlightsByExample(Flight example) {

		long id = example.getId();
		String flightNumber = example.getFlightNumber();
		String takeoffIata = null;
		Airport takeoff = example.getTakeoff();
		if (takeoff != null)
			takeoffIata = takeoff.getIata();
		LocalDateTime takeoffTime = example.getTakeoffTime();

		//itt a QueryDSL-es Predicate-t kell importálni
		ArrayList<Predicate> predicates = new ArrayList<Predicate>();

		//ez a QFlight annotáció jött létre a QueryDSL plugin hozzáadása után	
		QFlight flight = QFlight.flight;
		
		if (id > 0)
			predicates.add(flight.id.eq(id));

		if (StringUtils.hasText(flightNumber)) //case: insensitive
			predicates.add(flight.flightNumber.startsWithIgnoreCase(flightNumber));
		
		if (StringUtils.hasText(takeoffIata)) //case: sensitive
			predicates.add(flight.takeoff.iata.startsWith(takeoffIata));

		if (takeoffTime != null) {
			LocalDateTime startOfDay = LocalDateTime.of(takeoffTime.toLocalDate(), LocalTime.MIDNIGHT);
			predicates.add(flight.takeoffTime.between(startOfDay, startOfDay.plusDays(1)));
		}

		//ehhez a flightRepository implement részét bővíteni kell QuerydslPredicateExecutor<Flight>-tal
		//ez egy Iterable<Flight>-ot ad vissza, amit a Guava library Lists.newArrayList metódusával alakítunk List<Flight>-tá
		return Lists.newArrayList(flightRepository.findAll(ExpressionUtils.allOf(predicates)));
		
	}
	
	//@Transactional
	
	//ahhoz, hogy ez működésbe lépjen kell a projektben létezzen egy konfigurációs osztály, amin engedélyezve van az időzítés
	//ez gyakorlatilag egy üres osztály, amin 2 annotáció van: @Configuration és @EnableScheduling
	//de az is megfelelő, ha ez utóbbit csak "prosztó módon" :D rátesszük az AirportApplication-re
	//minden 30 mp-ben:
	//@Scheduled(cron = "*/15 * * * * *")
	
	//ha egy ütemezett metódus futási ideje hosszabb, mint amilyen gyakran indulnia kell
	//(és az üzleti logika megkövetli, hogy ennek ellenére induljon el a következő futtatás)
	//akkor a megolds az aszinkronitás engedélyezése, az az @EnableAsync -> a konfigurációs osztályra
	//@Async
	
	//ha egyszer több példánya fut a programnak, akkor ez megvéd attól, hogy mindegyik külön-külön futtassa az updateDelays-t 
	@SchedulerLock(name = "updateDelays")
	public void updateDelays() {
		
		System.out.println("updateDelays called");
		
		//ez így egy hosszú tranzakció és a hosszan nyitva tartott tranzakció rontja a teljesítményfokot, ezért ezt nem érdemes tranzakcionálássi tenni!
		//szóval a @Transactional kikommentelésra került, így viszont ezt a metódust refaktorálni kell a lentiek szerint
		//flightRepository.findAll().forEach(f->f.setDelayInSec(delayService.getDelay(f.getId())));
		
		flightRepository.findAll().forEach(f->{
			updateFlightWithDelay(f);
		});
		
	}

	public void updateFlightWithDelay(Flight f) {
		f.setDelayInSec(delayService.getDelay(f.getId()));
		flightRepository.save(f);
	}
	
	//ahhoz, hogy egy második metódust is ütemezetten lehessen futtatni
	//az application.properties-ben be kell állítani a spring.task.scheduling.pool.size-t
	//@Scheduled(cron = "*/10 * * * * *")
	public void dummy() {
		
		try {
			Thread.sleep(8000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		System.out.println("dummy called");
		
	}
	
	//dinamikus időzítési megoldás: az alábbi két metódussal kliens oldalról (controller osztályon keresztül)
	//elindítható (paraméterként átadott gyakorisággal) és leállítható egy ütemezett feladat
	
	public void startDelayPollingForFlight(long flightId, long rate) {
		
		//ebben segít a TaskScheduler, aminek egy Runnable-t kell átadni (vagy egy lamdát) és egy gyakoriságot
		//visszatérési értéke pedig egy ScheduledFuture		
		ScheduledFuture<?> scheduledFuture = taskScheduler.scheduleAtFixedRate(()->{
			
												//flight létezésének ellenőrzése
												Optional<Flight> optionalFlight = flightRepository.findById(flightId);
												
												//ha létezik a flight
												if(optionalFlight.isPresent())
													updateFlightWithDelay(optionalFlight.get());
												
											}, rate);
		
		//ha van ScheduledFuture ehhez a flightId-hoz kapcsolódóan a HashMap-ben
		//azaz újra meghívták az indítást anélkül, hogy leállították volna azt
		//akkor itt felülírnánk az új ütemezéssel -> a korábbihoz már nem lenne referenciánk,
		//így soha nem tudnánk már leállítani azt
		stopDelayPollingForFlight(flightId);
		
		//az így kapott ScheduledFuter-t eltároljuk egy HashMap-ben
		delayPollerJobs.put(flightId, scheduledFuture);
		
	}
	
	public void stopDelayPollingForFlight(long flightId) {

		//flightId alapján lekérjük a scheduledFuture-t a HashMap-ből
		ScheduledFuture<?> scheduledFuture = delayPollerJobs.get(flightId);
		
		//ha szerepel a HashMap-ben (tehát adott flight-nak van elindított pollozás) egy cancel metódushívással leállítható
		//a metódus elfogad egy boolean paramétert, amivel szabályozható, hogy ha épp folyamatban van a futtatás,
		//akkor szakítsa-e meg azt is, vagy annak a lefutását már várja meg és csak utánna szakítsa meg az ütemezést
		if(scheduledFuture!= null)
			scheduledFuture.cancel(false);
		
	}
	
}