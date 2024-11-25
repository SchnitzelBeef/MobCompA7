package layer2_802Algorithms;

import plot.JEMultiPlotter;
import layer1_802Phy.JE802PhyMode;
import layer2_80211Mac.JE802_11BackoffEntity;
import layer2_80211Mac.JE802_11Mac;
import layer2_80211Mac.JE802_11MacAlgorithm;

public class MobComp_Assignment07 extends JE802_11MacAlgorithm {
	
	private JE802_11BackoffEntity theBackoffEntityAC01;
	
	private double theSamplingTime_sec;

	private PIDController pid_controller;

	// **ADDED**
	// Possible Phy modes: "BPSK12", "BPSK34", "QPSK12", "QPSK34", "16QAM12", "16QAM34", "64QAM23", "64QAM34"
	private String[] PhyModes = {
		"64QAM34",
		"QPSK12",
		"BPSK12",
		"BPSK12",
		"BPSK12",
		"BPSK12",
	};
	// **ADDED**
	private int currentPhyMode = 0;

	// **ADDED**
	private int step = 0;

	public MobComp_Assignment07(String name, JE802_11Mac mac) {
		super(name, mac);
		this.theBackoffEntityAC01 = this.mac.getBackoffEntity(1);
		message("This is station " + this.dot11MACAddress.toString() +". MODIFIED MobComp algorithm: '" + this.algorithmName + "'.", 100);
	
		// **ADDED**
		// Create PID controller
		// The three parameters are Kp, Ki, and Kd (placed in this order)
		this.pid_controller = new PIDController(0.001, 0.002, 0.0001);

		// Initial PhyMode (queue is empty, so be optimistic):
		this.mac.getPhy().setCurrentPhyMode("64QAM34"); 

		// Same goes for AIFSN and CWmin:
		theBackoffEntityAC01.setDot11EDCAAIFSN(2);
		theBackoffEntityAC01.setDot11EDCACWmin(15);

		// it is possible to change the PhyMode
		this.mac.getPhy().setCurrentTransmitPower_dBm(0);   
	}

	// **ADDED*
	private int setIntValue(double state, int lowerBound, int upperBound) {
		double ret = state * (upperBound - lowerBound);
		// Good max and min operations ik ;)
		if (ret >= upperBound) {
			return upperBound;
		}
		if (ret <= lowerBound) {
			return lowerBound;
		}
		return (int)Math.round(ret);
	}

	@SuppressWarnings("unused")
	@Override
	public void compute() {

		this.mac.getMlme().setTheIterationPeriod(0.1);  // the sampling period in seconds, which is the time between consecutive calls of this method "compute()"
		this.theSamplingTime_sec =  this.mac.getMlme().getTheIterationPeriod().getTimeS(); // this sampling time can only be read after the MLME was constructed.
		
		// observe outcome:  (might need to be stored from iteration to iteration)
		int aQueueSize = this.theBackoffEntityAC01.getQueueSize();
		int aCurrentQueueSize = this.theBackoffEntityAC01.getCurrentQueueSize();
		JE802PhyMode aCurrentPhyMode = this.mac.getPhy().getCurrentPhyMode();

		// **ADDED**
		// --------------------- ASSIGNMENT -------------------------------:

		// Observe backoff entities:
		Integer AIFSN_AC01 = theBackoffEntityAC01.getDot11EDCAAIFSN();
		Integer CWmin_AC01 = theBackoffEntityAC01.getDot11EDCACWmin();

		// We effectively want a 0 queue, therefore the error of each 

		this.pid_controller.response((double)aCurrentQueueSize, this.theSamplingTime_sec);


		double state = this.pid_controller.getLastResponse() / (double)aQueueSize;

		// When queue is low, it AIFSN and CWmin can go loooow
		// When queue is high, a lot more energy must be invested in getting single packets through at a lower rate. 
		// Same goes for the Phy-modulation used to send the signals.

		theBackoffEntityAC01.setDot11EDCAAIFSN(this.setIntValue(state, 1, 20));
		theBackoffEntityAC01.setDot11EDCACWmin(this.setIntValue(state, 1, 50));

		// Stepper function to make the Phy-mode switch less often to reduce the queue window going into oscillation because of the rapid switch of more or less optimistic Phy-modes
		// could go larger, currently this is 20 seconds (value = 200)
		this.step += 1;
		if (this.step >= 200) {
			this.currentPhyMode = Math.max(Math.min(this.setIntValue(state, 0, PhyModes.length-1), this.currentPhyMode + 1), this.currentPhyMode-1);
			this.step = 0;
		}
	
		// --------------------- ASSIGNMENT -------------------------------
	}
	
	@Override
	public void plot() {
		if (plotter == null) {
			plotter = new JEMultiPlotter("PID Controller, Station " + this.dot11MACAddress.toString(), "max", "time [s]", "MAC Queue", this.theUniqueEventScheduler.getEmulationEnd().getTimeMs() / 1000.0, true);
			plotter.addSeries("PID");
			plotter.addSeries("AIFSN");
			plotter.addSeries("CWmin");
			plotter.addSeries("current");
			plotter.display();
		}
		plotter.plot(((Double) theUniqueEventScheduler.now().getTimeMs()).doubleValue() / 1000.0, theBackoffEntityAC01.getQueueSize(), 0);
		plotter.plot(((Double) theUniqueEventScheduler.now().getTimeMs()).doubleValue() / 1000.0, pid_controller.getLastResponse(), 1);
		plotter.plot(((Double) theUniqueEventScheduler.now().getTimeMs()).doubleValue() / 1000.0, theBackoffEntityAC01.getDot11EDCAAIFSN(), 2);
		plotter.plot(((Double) theUniqueEventScheduler.now().getTimeMs()).doubleValue() / 1000.0, theBackoffEntityAC01.getDot11EDCACWmin(), 3);
		plotter.plot(((Double) theUniqueEventScheduler.now().getTimeMs()).doubleValue() / 1000.0, theBackoffEntityAC01.getCurrentQueueSize(), 4);
	
	}

}
