package hu.webuni.airport.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import hu.webuni.airport.model.AirportUser;

public interface UserRepository extends JpaRepository<AirportUser, String>{

}
