package hu.webuni.airport.xmlws;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import javax.xml.ws.AsyncHandler;

import org.apache.cxf.annotations.UseAsyncMethod;
import org.apache.cxf.jaxws.ServerAsyncResponse;
import org.springframework.stereotype.Service;

import hu.webuni.airport.api.model.HistoryDataAirportDto;
import hu.webuni.airport.mapper.HistoryDataMapper;
import hu.webuni.airport.model.Airport;
import hu.webuni.airport.model.HistoryData;
import hu.webuni.airport.service.AirportService;
import hu.webuni.airport.service.DelayService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AirportXmlWsImpl implements AirportXmlWs {

	private final AirportService airportService;
	private final HistoryDataMapper historyDataMapper;
	private final DelayService delayService;
	
	@Override
	public List<HistoryDataAirportDto> getHistoryById(Long id) {
		
		List<HistoryData<Airport>> airports = airportService.getAirportHistory(id);
		
		List<HistoryDataAirportDto> result = new ArrayList<>();
		
		airports.forEach(hd ->{
			result.add(historyDataMapper.airportHistoryDatatoDto(hd));
		});
		
		return result;
		
	}

	@Override
	//ez az annotáció okozza, hogy nem ez a metódus, hanem ennek az async párja hívódik meg
	@UseAsyncMethod
	public int getFlightDelay(Long id) {
		return 0;
	}
	
	//aszinkron metódus pár
	public Future<Integer> getFlightDelayAsync(Long id, AsyncHandler<Integer> asyncHandler) {

		System.out.println("getFlightDelayAsync called at thread:" +Thread.currentThread().getName());
		
		ServerAsyncResponse<Integer> serverAsyncResponse = new ServerAsyncResponse<>();
		
		delayService.getDelayAsync(id).thenAccept(result ->{
			
			System.out.println("getFlightDelayAsync get the result at thread:" +Thread.currentThread().getName());
			
			serverAsyncResponse.set(result);
			asyncHandler.handleResponse(serverAsyncResponse);
		
		});
		
		return serverAsyncResponse;
	}

}
