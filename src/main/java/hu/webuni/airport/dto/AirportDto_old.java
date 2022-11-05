//package hu.webuni.airport.dto;
//
//import java.util.List;
//
//import javax.validation.constraints.Size;
//
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.Setter;
//
//@Getter
//@Setter
//@Builder
//@AllArgsConstructor
//@NoArgsConstructor
//public class AirportDto_old {
//	
//	private long id;
//	@Size(min = 3, max = 20)
//	private String name;
//	private String iata;
//	
//	private AddressDto address;
//	
//	private List<FlightDto> departures;
//	private List<FlightDto> arrivals;
//	
//}