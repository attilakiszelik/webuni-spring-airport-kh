package hu.webuni.airport.security;

import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import hu.webuni.airport.model.AirportUser;
import hu.webuni.airport.repository.UserRepository;

@Service
public class AirportUserDetailsService implements UserDetailsService{

	@Autowired
	UserRepository userRepository;
	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

		//a findById metódus visszatérési értéke Optional<AirportUser> lenne, ezért kell beépíteni, hogy ha nem sikerült megtalálni, akkor dobjpn egy UsernameNotFoundException-t
		AirportUser airportUser = userRepository.findById(username).orElseThrow(()->new UsernameNotFoundException(username));
				
		//a UserDetails visszatérési érték tulajdonképpen egy User minimum felhasználónévvel, jelszóval és jogosultsági körökkel
		return new User(username, airportUser.getPassword(),airportUser.getRoles().stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList()));
		
	}
	
}
