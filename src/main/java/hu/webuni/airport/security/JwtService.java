package hu.webuni.airport.security;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

@Service
public class JwtService {

	private static final String AUTH = "auth";
	private String issuer = "AirportApp";
	private Algorithm alg = Algorithm.HMAC256("mySecret");

	public String createJwtToken(UserDetails principal) {
		
		return JWT.create()
				//subject-be kerül a felhasználónév
				.withSubject(principal.getUsername())
				//claim-be "auth" néven kerülnek hozzáadásra a jogosultságok (a claim-hez bármit hozzá lehet adni kulcs-érték párként)
				.withArrayClaim(AUTH, principal.getAuthorities().stream().map(GrantedAuthority::getAuthority).toArray(String[]::new))
				//lejárati idő beállítása
				.withExpiresAt(new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(2)))
				//kibocsátó beállítása
				.withIssuer(issuer)
				//aláírás beállítása (paraméterként kell megadni a használni kívánt titkosítási algoritmust, aminek meg a használni kívánt salt-ot
				.sign(alg);
		
	}

	public UserDetails parseJwt(String jwtToken) {

		//dekódolt jwt létrehozása
		DecodedJWT decodedJWT = JWT
								//a JWT .decode nem ellenőrzi az aláírást, ezért kell a .require-t használni helyette, aminek meg kell adni a felhasznált algoritmust
								.require(alg)
								//az issuer azonosság ellenőrzésének bekapcsolása
								.withIssuer(issuer)
								.build()
								.verify(jwtToken);
		
		//visszatérési érték egy User (felhasználónév, jelszó, jogolsultságok)
		// - felhasználónevet generáláskor a token subject-jébe került
		// - jelszót nem kell visszaadni, most történik az authentikáció, később már nem lesz rá szükség
		// - jogosultságok: az AUTH nevű claim-ból String listaként lekérve, ami aztán egy SimpleGrantedAuthority-ket tartalmazó map-be kerül streamelésre
		return new User(decodedJWT.getSubject(), "dummy", decodedJWT.getClaim(AUTH).asList(String.class).stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList()));
	}

}
