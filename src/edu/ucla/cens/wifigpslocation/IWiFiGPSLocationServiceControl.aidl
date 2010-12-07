package edu.ucla.cens.wifigpslocation;

interface IWiFiGPSLocationServiceControl
{

	/**
	 * Sets the current operational regime.
	 * REGIME_RELAXED (0x00000000) is the default regime where the service can
	 * take suggestions from its clients.
	 * Other integer values indicate next levels of power consumption limitations
	 *
	 * @param		regime		new power consumption regime
	 */ 
	void setOperationRegime(int regime);
	
	
	/**
	 * Increases the GPS sampling interval of the service. 
	 * The value after the modification is returned
	 *
	 * @return					current GPS sampling interval in milliseconds
	 */ 
	int increaseInterval();
	
	/**
	 * Decreases the GPS sampling interval of the service.
	 * The value after the modification is returned.
	 *
	 * @return					current GPS sampling interval in milliseconds
	 */
	int decreaseInterval();
	
	/**
	 * Sets the sampling interval of GPS and returns the current value to verify.
	 *
	 * @param		interval	new sampling interval in milliseconds
	 * @return					current sampling interval in milliseconds
	 */
	int setInterval(int interval);
	
	/**
	 * Retruns the current GPS sampling interval.
	 *
	 * @return					current GPS sampling interval in milliseconds
	 */
	 int getInterval();
	
	/**
	 * Sets teh power consumption level.
	 * 
	 * @param		power		new power consumption level
	 */
	void setPower(int power);
	
	/**
	 * Returns the current power consumption level.
	 * 
	 * @return					current power consumption level
	 */
	int getPower();
	
	

}
