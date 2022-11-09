package hu.webuni.airport.service;

import java.util.Random;
import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class DelayService {

	private Random random = new Random();
	
	public int getDelay(long flightId) {
		
		//System.out.println("getDelay called");
		//aszinkron szálkezelés demonstrálásakor ez ki lett egészítve:
		System.out.println("DelayService.getDelay called at thread:" + Thread.currentThread().getName());
		
		//vár 5 mp-et, hogy szimuláljuk, hogy ez egy hosszantartó lekérdezés
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		//...majd visszaad egy random számot 0 és 30 perc között
		return random.nextInt(0, 1800);
		
	}
	
//	//ASZINKRON SZÁLKEZELÉS
//	
//	//"A" opció:	
//	@Async
//	public CompletableFuture<Integer> getDelay(long flightId) {
//		
//		System.out.println("DelayService.getDelay called at thread:" + Thread.currentThread().getName());
//		
//		try {
//			Thread.sleep(5000);
//		} catch (InterruptedException e) {
//
//			e.printStackTrace();
//		}
//
//		return CompletableFuture.completedFuture(random.nextInt(0, 1800));
//		
//	}
	
	//webservice-ben használt aszinkron getDelay metódus
	@Async
	public CompletableFuture<Integer> getDelayAsync(long flightId) {
		
		System.out.println("DelayService.getDelayAsync called at thread:" + Thread.currentThread().getName());
		
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {

			e.printStackTrace();
		}

		return CompletableFuture.completedFuture(random.nextInt(0, 1800));
		
	}
	
}
