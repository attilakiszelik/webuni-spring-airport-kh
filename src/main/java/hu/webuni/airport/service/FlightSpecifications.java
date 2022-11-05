package hu.webuni.airport.service;

import java.time.LocalDateTime;
import java.time.LocalTime;

import org.springframework.data.jpa.domain.Specification;

import hu.webuni.airport.model.Airport_;
import hu.webuni.airport.model.Flight;
import hu.webuni.airport.model.Flight_;

public class FlightSpecifications {
	
	public static Specification<Flight> hasId(long id){
		return (root, cq, cb) -> cb.equal(root.get(Flight_.id), id);
	}

	public static Specification<Flight> hasFlightNumber(String flightNumber) {
		return (root, cq, cb) -> cb.like(root.get(Flight_.flightNumber), flightNumber + "%");
	}
	
	public static Specification<Flight> hasTakoffTime(LocalDateTime takeoffTime) {
		LocalDateTime startOfDay = LocalDateTime.of(takeoffTime.toLocalDate(), LocalTime.MIDNIGHT);
		
		return (root, cq, cb) -> cb.between(
				root.get(Flight_.takeoffTime), 
				startOfDay, startOfDay.plusDays(1));
	}
	
	public static Specification<Flight> hasTakoffIata(String takeoffIata) {
		return (root, cq, cb) -> cb.like(
				root.get(Flight_.takeoff).get(Airport_.iata), 
				takeoffIata + "%");
	}
	
}