package com.github.rosjava_alphabot.driver;

import com.github.rosjava_alphabot.driver.dto.DistDto;
import com.github.rosjava_alphabot.driver.dto.TwistDto;

public class AlphabotDriver {

	public DistDto getDistances() {
		return new DistDto();
	}

	public void processTwistMessage(TwistDto twist) {
	}

}
