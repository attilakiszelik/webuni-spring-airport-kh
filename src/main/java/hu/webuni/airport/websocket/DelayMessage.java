package hu.webuni.airport.websocket;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DelayMessage {

	private int delay;
	private OffsetDateTime timestamp;
	private long flightId;
	
}