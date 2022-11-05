package hu.webuni.airport.service;

import java.util.Random;

import org.springframework.stereotype.Service;

@Service
public class DelayService {

	private Random random = new Random();
	
	public int getDelay(long flightId) {
		
		System.out.println("getDelay called");
		
		//vár 5 mp-et, hogy szimuláljuk, hogy ez egy hosszantartó lekérdezés
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//...majd visszaad egy random számot 0 és 30 perc között
		return random.nextInt(0, 1800);
		
	}
	
}
