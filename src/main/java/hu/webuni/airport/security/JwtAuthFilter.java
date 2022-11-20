package hu.webuni.airport.security;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthFilter extends OncePerRequestFilter{

	//ez a filter felelős azért, hogy a visszakapott JWT-t authentikálja és beállítsa a SecurityContextHolder-be
	
	private static final String AUTHORIZATION = "Authorization";
	private static final String BEARER = "Bearer ";
	
	@Autowired
	JwtService jwtService;
	
//	//ALAP KÉPZÉS
//	@Override
//	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
//			throws ServletException, IOException {
//		
//		//a request headerjének lekérése
//		String authHeader = request.getHeader(AUTHORIZATION);
//		
//		//csak akkor történik vizsgálat, ha az nem null és "Bearer "-rel kezdődik 
//		if(authHeader != null && authHeader.startsWith(BEARER)) {
//			
//			//az authHeader elejéről le kell vágni a "Bearer " előtagot
//			String jwtToken = authHeader.substring(BEARER.length());
//			
//			//az így kapott token visszafejtése UserDetails-é
//			UserDetails principal = jwtService.parseJwt(jwtToken);
//			
//			//majd annak beállítása a SecurityContextHolder-be (ehhez először át kell adni egy authentication-nek)
//			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
//			
//				//ha érdekes, hogy milyen IP-ről érkezik vissza a token, akkor ezt is be kell állítani a SecurityContextHolder-nek történő átadás előtt
//				authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
//				
//			SecurityContextHolder.getContext().setAuthentication(authentication);
//		}
//		
//		//a kérés és a válasz tovább engedése
//		filterChain.doFilter(request, response);
//		
//	}

	//KÖZÉPHALADÓ KÉPZÉS
	//során kiszerveztük a UsernamePasswordAuthenticationToken előállítását egy újra felhasználható metódusba,
	//hogy azt fel lehessen használni a WebSocketConfig -> configureClientInboundChannel metódusában is 
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		
		//a request headerjének lekérése
		String authHeader = request.getHeader(AUTHORIZATION);
		
		//a metódus hívása az authHeader átadásával (visszatérési értékét eltároljuk egy változóban)
		UsernamePasswordAuthenticationToken authentication = createUserDetailsFromAuthHeader(authHeader, jwtService);
		
		//vizsgálni kell, hogy nem null értékkel tért vissza a metódus (különben hibára fut a kód)
		if(authentication != null) {
			
			//ha érdekes, hogy milyen IP-ről érkezik vissza a token, akkor ezt is be kell állítani a SecurityContextHolder-nek történő átadás előtt
			authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
			//authentikáció átadása a SecurityContextHolder-nek
			SecurityContextHolder.getContext().setAuthentication(authentication);
		
		}
		
		//a kérés és a válasz tovább engedése
		filterChain.doFilter(request, response);
		
	}

	//static által válik újrafelhasználhatóvá a metódus
	public static UsernamePasswordAuthenticationToken createUserDetailsFromAuthHeader(String authHeader, JwtService jwtService) {
		
		//csak akkor történik vizsgálat, ha az authHeader nem null és "Bearer "-rel kezdődik 
		if(authHeader != null && authHeader.startsWith(BEARER)) {
			
			//az authHeader elejéről le kell vágni a "Bearer " előtagot
			String jwtToken = authHeader.substring(BEARER.length());
			
			//az így kapott token visszafejtése UserDetails-é
			UserDetails principal = jwtService.parseJwt(jwtToken);
			
			//majd a UserDetails adatainak átadása egy UsernamePasswordAuthenticationToken típusú változónak
			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
			
			//visszatérés
			return authentication;
		}
		
		//ha nem sikerült a token előállítása
		return null;
	}
	
}
