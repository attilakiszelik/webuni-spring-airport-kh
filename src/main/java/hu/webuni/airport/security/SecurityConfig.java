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
	
	//ezek a Source -> Override/Implement methods-ból legenerálhatóak (sárga metódus protected)
	
	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {

//		//első körben egy inMemoryAuthentication került hozzáadásra a projekthez
//		auth
//			.inMemoryAuthentication() //első körben memóriában nyilvántartott user-ekkel használjuk
//			.passwordEncoder(passwordEncoder()) //saját password encoder hozzáadása
//			.withUser("user").authorities("user").password(passwordEncoder().encode("pass"))
//			.and()
//			.withUser("admin").authorities("user","admin").password(passwordEncoder().encode("pass"));
		
		//második körben ez lecserélésre került egy DaoAuthenticaton-re, ami már adatbázisban tárolt felhasználókkal dolgozik
		auth
			.authenticationProvider(myAuthenticationProvider());
		
	}

//	@Override
//	protected void configure(HttpSecurity http) throws Exception {
//		http
//			.httpBasic() //httpBasic authentikáció bekapcsolása (csak akkor elegendő, ha https protokoll alatt fut az alkalmazás, mert sima http alatt stringként látható a felküldött jelszó)
//				//ezek később lettek hozzáadva:
//				.and()
//				//csrf (cross site request forgery) támadás elleni védelem, hogy a szerver az oldallal együt kigenerál egy csrf tokent is, amit minden http request-nél visszavár, ha azt nem kapja vissza a kéréssel, akkor 401 unauthorized-ot dob
//				//csrf token generálása by default be van kapcsolva, de hogy tesztelhető legyen postman-ből (mivel ott nem kerül generálásra az oldal és így a csrf token sem), ennek kikapcsolása
//				.csrf().disable()
//				//authentikáció után a szerver generál egy JSESSIONID-t (egy cookie-t), amit visszaküld a kliensnek és a további request-ek esetén felhasználónév-jelszó helyett azt várja vissza
//				//ennek a JSESSIONID generálásnak a kikapcsolása (minden request esetén elvárja a felhasználónév-jelszó párost) -> rest api esetén ezt szokás így beállítani
//				.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
//			.and() // és :D
//			.authorizeRequests() //az alábbi request-ek authorizációhoz (szerepkörhöz) kötésének bekapcsolása
//				//ezt a JWTLoginController bevezetése után kellett felvenni, hogy bárki postolhasson erre a címre
//				.antMatchers(HttpMethod.POST,"/api/login/**").permitAll()
//			.antMatchers(HttpMethod.POST,"/api/airports/**").hasAuthority("ADMIN")
//			.antMatchers(HttpMethod.PUT,"/api/airports/**").hasAnyAuthority("USER","ADMIN") //több szerepkör esetén hasAnyAuthority-t kell használni
//			.anyRequest().authenticated(); // minden request authentikációhoz (bejelentkezéshez) kötött, emiatt fogja feldobni a beépített httpBasic authentikációs felületet
//			
//			//fentről lefelé értékelődnek ki, ha valamelyik szabály illeszkedik, akkor nem megy tovább a kiértékelés,
//			//ezért best practice, hogy a végén mindig legyen egy denyAll(), így véletlenül sem lehet elérni olyan oldalt, ami nem került szabályozásra
//	}

	
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
			.antMatchers(HttpMethod.POST,"/api/airports/**").hasAuthority("ADMIN")
			.antMatchers(HttpMethod.PUT,"/api/airports/**").hasAnyAuthority("USER","ADMIN") 
			.anyRequest().authenticated()
			.and()
			.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class); 

	}
	
	@Bean
	public AuthenticationProvider myAuthenticationProvider() {
		
		//a DaoAuthentication csak a UserDetailsService-szel működik, ezért kellett létrehozni az AirportUserDetailsService osztályt, ami implementálja a szükséges UserDetailsService-t
		DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
		daoAuthenticationProvider.setPasswordEncoder(passwordEncoder());
		daoAuthenticationProvider.setUserDetailsService(userDetailsService);
		return daoAuthenticationProvider;
		
	}

	//JwtService-ben injektáláshoz
	@Override
	@Bean
	public AuthenticationManager authenticationManagerBean() throws Exception {
		return super.authenticationManagerBean();
	}
	
}
