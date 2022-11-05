package hu.webuni.airport.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles({"prod", "test"})
public class PriceServiceIT {
	
	@Autowired
	PriceService priceService; 

	@Test
	void testGetFinalPrice() throws Exception {
		
		int newPrice = priceService.getFinalPrice(100);
		assertThat(newPrice).isEqualTo(90);
	}
	
	@Test
	void testGetFinalPriceWithHighPrice() throws Exception {
		
		int newPrice = priceService.getFinalPrice(11000);
		assertThat(newPrice).isEqualTo(9350);
	}
	
}
