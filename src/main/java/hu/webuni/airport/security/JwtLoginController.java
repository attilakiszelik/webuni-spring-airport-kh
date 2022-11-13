package hu.webuni.airport.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import hu.webuni.airport.dto.LoginDto;

@RestController
public class JwtLoginController {

	//az AuthenticationManager by default létezik a Spring Security miatt, de nem lehet csak úgy injektálni, először Override-olni kell az AuthenticationManagerBean metódust a SecurityConfig-ban
	@Autowired
	AuthenticationManager authenticationManager; 
	
	@Autowired
	JwtService jwtService;
	
	@PostMapping("/api/login")
	public String login(@RequestBody LoginDto loginDto) {
		
		Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginDto.getUsername(), loginDto.getPassword()));
		
		//ez így egy String-gel tér vissza, középhaladó képzésben ezt átalakítjuk JSON-né
		//return jwtService.createJwtToken((UserDetails)authentication.getPrincipal());
		return "\"" + jwtService.createJwtToken((UserDetails)authentication.getPrincipal()) + "\"";
		
	}
	
}
