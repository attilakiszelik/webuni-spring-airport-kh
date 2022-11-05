package hu.webuni.airport.model;

import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.validation.constraints.Size;

import org.hibernate.envers.Audited;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Audited
public class Airport {
	
	@Id
	@GeneratedValue
	@EqualsAndHashCode.Include()
	private long id;
	@Size(min = 3, max = 20)
	private String name;
	private String iata;
	
		//a kapcsolatok betöltése statikusan meghatározott, by default:
		// - OneToMany és ManyToMany esetén -> LAZY (laza) 
		// - OneToOne és ManyToOne esetén -> EAGER (mohó)
		//ezek a (fetch=FetchType.LAZY) és (fetch=FetchType.EAGER) paraméterekkel felülírhatóak
		//preferált megoldás: lekérdezésenként dinamikusan kezelni
		
		//egy reptérnek egy címe van, de előfordulhat, hogy egy címen akár több reptér van (Ferihegy1, Ferihegy2)
		@ManyToOne(fetch = FetchType.LAZY)
		private Address address;
		
		//a Flight-nál a takeoff és a landing is egy ManyToOne típusú kapcsolaton keresztül egy reptérre mutat
		//ezeket húrozzuk be ide, hogy minden repülőtérnél elérhető legyen a felszálló és leszálló járatok listája
		// - a takeoff-ot -> departures néven
		// - a landing-et -> arrivals néven
		//egy OneToMany típusú kapcsolat még könnyen kezelhető, viszont egynél több esetében már másként kell kezelni a problémát
		
		@OneToMany(mappedBy = "takeoff")
		//private List<Flight> departures;
		private Set<Flight> departures;
		
		//1. probléma, hogy ha egynél több OneToMany típusú kapcsolat van az osztályban, akkor Set<> típust kell használni,
		//mert két List<> esetén MultipleBagFetchException-t fog dobni az SQL query (1. List<> + 2. Set<> még egyébként oké)
		//alapvetően is célszerűbb Set<>-ben gondolkodni, kivéve, ha sorba kell rendezni a találatokat -> akkor marad a List<>!
		
		@OneToMany(mappedBy = "landing")
		//private List<Flight> arrivals;
		private Set<Flight> arrivals;
		
		@OneToMany
		//ha az image-ből nem hivatkozunk vissza az ariport-ra egy ManyToOne-nal
		//akkor a spring by deafult létrehoz egy kapcsolótáblát kettőjük között
		//ilyenkor alapvetően elegendő lenne egy idegen kulcs oszlop az image-ben
		//ahhoz, hogy kapcsoló tábla helyett csak egy idegen kulcs oszlop jöjjön létre az image-ben:
		@JoinColumn(name = "airport_id")
		private Set<Image> images;
		
		//constructor
		public Airport(String name, String iata) {
			this.name = name;
			this.iata = iata;
		}
		
}
