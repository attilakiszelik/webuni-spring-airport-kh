package hu.webuni.airport.mapper;

import java.util.List;

import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import hu.webuni.airport.api.model.AirportDto;
import hu.webuni.airport.api.model.FlightDto;
import hu.webuni.airport.model.Airport;
import hu.webuni.airport.model.Flight;

@Mapper(componentModel = "spring")
public interface AirportMapper {

	List<AirportDto> airportsToDtos(List<Airport> airports);

	AirportDto airportToDto(Airport airport);
	
	Airport dtoToAirport(AirportDto airportDto);
	
	//az AirportController-ben bevezetett isFull elágaztatás miatt az alábbi két mappelés került bevezetésre:
		
		@IterableMapping(qualifiedByName = "summary")
		List<AirportDto> airportSummariesToDtos(List<Airport> airports);
	
		@Named("summary")
		@Mapping(target="address", ignore = true)
		//departures és arrivals hozzáadása után ezek mappaelését is ignorálni kell
		@Mapping(target="departures", ignore = true)
		@Mapping(target="arrivals", ignore = true)
		AirportDto airportSummaryToDto(Airport airport);
	
	//az AirpotDto-ban List<FlightDto> departures és List<FlightDto> arrivals
	//a FlightDto-ban pedig AirportDto airport, így kialakul egy kör körös hivatkozás
	//megoldás: az AirportMapper által használt Flight-ból FlightDto konvertálásakor ignorálni kell a Flight osztály Airport típusú változóit
	@Mapping(target="takeoff", ignore = true)
	@Mapping(target="landing", ignore = true)
	FlightDto flightDto(Flight flight);

}