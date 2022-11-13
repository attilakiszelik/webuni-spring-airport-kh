package hu.webuni.airport.model;

import java.util.Set;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class AirportUser {

	@Id
	private String username;
	private String password;
	
	//a role (ebben a projektben most) nem külön entitás, így az ElementCollection annotáció szükséges rá
	//ezáltal nem jön létre saját táblája az adatbázisban, viszont user és role közötti kapcsolótábla igen
	@ElementCollection(fetch = FetchType.EAGER)
	private Set<String> roles;
		
}
