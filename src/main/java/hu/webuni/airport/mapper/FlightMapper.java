package hu.webuni.airport.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import hu.webuni.airport.api.model.AirportDto;
import hu.webuni.airport.api.model.FlightDto;
import hu.webuni.airport.model.Airport;
import hu.webuni.airport.model.Flight;

@Mapper(componentModel = "spring")
public interface FlightMapper {

	Flight dtoToFlight(FlightDto flightDto);

	FlightDto flightToDto(Flight flight);
	
	List<FlightDto> flightsToDtos(List<Flight> flight);

	//FlightController QuerydslPredicates-je használja
	List<FlightDto> flightsToDtos(Iterable<Flight> findAll);
	
	@Mapping(target="address", ignore = true)
	@Mapping(target="departures", ignore = true)
	@Mapping(target="arrivals", ignore = true)
	AirportDto airportSummaryToDto(Airport airport);

}