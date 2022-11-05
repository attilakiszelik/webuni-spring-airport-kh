package hu.webuni.airport.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import hu.webuni.airport.model.Image;

public interface ImageRepository extends JpaRepository<Image, Long>{

}
