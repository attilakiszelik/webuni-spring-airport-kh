package hu.webuni.airport.xmlws;

import java.util.List;

import javax.jws.WebService;
import javax.xml.ws.ResponseWrapper;

import hu.webuni.airport.api.model.HistoryDataAirportDto;

@WebService
public interface AirportXmlWs {

	public List<HistoryDataAirportDto> getHistoryById(Long id);
	
	//az aszinkron metódus miatt nem tudja automatikusan, hogy mibe kell csomagolnia a választ, ezért az alábbiak szerint meg kell adni neki
	@ResponseWrapper(localName="getFlightDelayResponse", className="java.lang.Integer")
	public int getFlightDelay(Long id);
	
}