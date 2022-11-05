package hu.webuni.airport.mapper;

import org.mapstruct.Mapper;

import hu.webuni.airport.api.model.HistoryDataAirportDto;
import hu.webuni.airport.model.Airport;
import hu.webuni.airport.model.HistoryData;

@Mapper(componentModel = "spring")
public interface HistoryDataMapper {

	HistoryDataAirportDto airportHistoryDatatoDto(HistoryData<Airport> hd);

}