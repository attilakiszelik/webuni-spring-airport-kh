package hu.webuni.airport.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class PriceService {

	private final DiscountService discountService;

	public int getFinalPrice(int price) {
		return (int) (price / 100.0 * (100 - discountService.getDiscountPercent(price)));
	}

}
