package hu.webuni.airport.web;

import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import hu.webuni.airport.service.UserService;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class FacebookLoginController {
	
	private final UserService userService;
	
	//sikeres facebook bejelentkezés után átdob erre az url-re (SecurityConfig-ban ezt adtuk meg)
	//(illetve a facebook arra a végpontra dob át, ahol a spring security lekezeli a választ és kinyeri a user infókat, majd az dob tovább erre a végpontra)
	
	@GetMapping("/fbLoginSucces")
	public String onFacebookLoginSuccess(Map<String, Object> model, OAuth2AuthenticationToken authenticationToken, @AuthenticationPrincipal OAuth2User principal) {
		
		//az authenticationToken getPrincipal metódusa egy OAuth2User-t ad, aminek az attribútumai között megtalálható a user neve 
		String fullname = authenticationToken.getPrincipal().getAttribute("name");
		
		//ha a token-ben rejlő további lehetőségeket nem szeretnénk kihasználni, akkor elegendő az OAuth2User-t injektálni 
		fullname = principal.getAttribute("name");
		model.put("fullName", fullname);
		
		//átadjuk az authenticationToken-t user adatbázisba mentéséhez, ha még nincs benne
		userService.registerNewUserIfNeeded(authenticationToken);
		
		return "home";
		
	}
}
