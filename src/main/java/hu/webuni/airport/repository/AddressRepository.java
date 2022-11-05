package hu.webuni.airport.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import hu.webuni.airport.model.Address;

public interface AddressRepository extends JpaRepository<Address, Long>{
	
}