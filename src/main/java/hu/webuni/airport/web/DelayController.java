package hu.webuni.airport.web;

import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import hu.webuni.airport.service.DelayService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class DelayController {

	//ASZINKRON SZÁLKEZELÉS
	
	//alap esetben a webkonténer "nio" szálán érkezik minden kérés és az blokkolásra kerül, mindaddig, amíg a válasz visszaküldésre nem kerül,
	//ami egy időigényes kérés esetén problémás -> megoldás, hogy vagy a service metódust ("A"), vagy magát a controller metódust ("B") tesszük aszinkronná
	//"A" opciónak olyan üzleti logika mellet van létjogosultsága, ahol a kliens nem várja meg a kérés eredményét, mert pl.: az majd kiküldésre kerül emailben
	//ilyenkor a controller osztály hívása még a "nio" szállon történik, az blokkolódik, viszont a továbbhívás a service osztályba már a thread pool soron következő,
	//szabad task szálán történik meg és mivel a controller osztály vissza küld egy nullát válaszként így a "nio" szál blokkolódása sem jelentős
	//"B" opció esetében a kliens ugyan csak akkor kap választ, amikor feldolgozásra került a kérés, de ilyenkor már a controller osztály hívása sem a "nio" szálon történik
	
	private final DelayService delayService;
	
	@Async
	@GetMapping("/api/flights/{id}/delay")
	public CompletableFuture<Integer> getDelayForFlight(@PathVariable Long id) {
		
		System.out.println("DelayController.getDelayForFlight called at thread:" + Thread.currentThread().getName());
		
		//"A" opció:
		//delayService.getDelay(id);
		//return 0;
		
		return CompletableFuture.completedFuture(delayService.getDelay(id));
		
	}
	
}
