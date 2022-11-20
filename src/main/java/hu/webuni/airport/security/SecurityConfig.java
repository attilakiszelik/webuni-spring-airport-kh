package hu.webuni.airport.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
//ahhoz, hogy a kontrollerekre tett metódus szintű védelmek működjenek engedélyezni kell azokat (azok így felülírják az antMatchers-ökkel beállított jogosultságokat)
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter{
	
	@Autowired
	UserDetailsService userDetailsService; 
	
	@Autowired
	JwtAuthFilter jwtAuthFilter;

	//Spring Security 5-től kötelező valamilyen jelszó titkosító használata
	//a BCrypt előnye, hogy megoldja a jelszó "sózását" (a megadott jelszóhoz hozzáad egy stringet (ez a salt), majd ezt követően hash-eli, így az nem lesz visszafejthető hash szótárból)
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
	
	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		
		auth
			.authenticationProvider(myAuthenticationProvider());
		
	}
	
	@Bean
	public AuthenticationProvider myAuthenticationProvider() {
		
		//a DaoAuthentication csak a UserDetailsService-szel működik, ezért kellett létrehozni az AirportUserDetailsService osztályt, ami implementálja a szükséges UserDetailsService-t
		DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
		daoAuthenticationProvider.setPasswordEncoder(passwordEncoder());
		daoAuthenticationProvider.setUserDetailsService(userDetailsService);
		return daoAuthenticationProvider;
		
	}
	
	//JWT generálásának és ellenőrzésének beállítása után a http basic helyett annak használata, tulajdonképpen úgy történik, hogy
	//azt be kell állítani a Spring Security által használt számos filter közül a UsernamePasswordAuthnticationFilter elé!
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http
			.csrf().disable()
			.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
			.and()
			.authorizeRequests()
			.antMatchers(HttpMethod.POST,"/api/login/**").permitAll()
			//középhaladó képzés soorán ezt a sort adtuk hozzá, hogy a /api/stomp/** végpontokra is bárki fel tudjon íratkozni bejelentkezés nélkül
			.antMatchers(HttpMethod.POST,"/api/stomp/**").permitAll()
			.antMatchers(HttpMethod.POST,"/api/airports/**").hasAuthority("ADMIN")
			.antMatchers(HttpMethod.PUT,"/api/airports/**").hasAnyAuthority("USER","ADMIN") 
			.anyRequest().authenticated()
			.and()
			.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class); 

	}
	

	@Override
	@Bean
	public AuthenticationManager authenticationManagerBean() throws Exception {
		return super.authenticationManagerBean();
	}
	
}
