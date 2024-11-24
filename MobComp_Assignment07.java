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
	
	public MobComp_Assignment07(String name, JE802_11Mac mac) {
		super(name, mac);
		this.theBackoffEntityAC01 = this.mac.getBackoffEntity(1);
		message("This is station " + this.dot11MACAddress.toString() +". MODIFIED MobComp algorithm: '" + this.algorithmName + "'.", 100);
	
		// **ADDED**
		// Create PID controller
		// The three parameters are Kp, Ki, and Kd
		this.pid_controller = new PIDController(100, 0.01, 10000);
	}

	@SuppressWarnings("unused")
	@Override
	public void compute() {

		this.mac.getMlme().setTheIterationPeriod(0.1);  // the sampling period in seconds, which is the time between consecutive calls of this method "compute()"
		this.theSamplingTime_sec =  this.mac.getMlme().getTheIterationPeriod().getTimeS(); // this sampling time can only be read after the MLME was constructed.
		
		// observe outcome:  (might need to be stored from iteration to iteration)
		int aQueueSize = this.theBackoffEntityAC01.getQueueSize();
		int aCurrentQueueSize = this.theBackoffEntityAC01.getCurrentQueueSize();
		double aCurrentTxPower_dBm = this.mac.getPhy().getCurrentTransmitPower_dBm();
		JE802PhyMode aCurrentPhyMode = this.mac.getPhy().getCurrentPhyMode();

		// --------------------- ASSIGNMENT - add your PID controller here:

		// Possible Phy modes: "BPSK12", "BPSK34", "QPSK12", "QPSK34", "16QAM12", "16QAM34", "64QAM23", "64QAM34"
    
		// this.mac.getPhy().setCurrentPhyMode("BPSK12");   // it is possible to change the PhyMode
		this.mac.getPhy().setCurrentPhyMode("64QAM34");   // it is possible to change the PhyMode
		// this.mac.getPhy().setCurrentPhyMode("16QAM34");   // it is possible to change the PhyMode
		this.mac.getPhy().setCurrentTransmitPower_dBm(0); // it is also possible to change the transmission power (please not higher than 0dBm)


		// Observe backoff entities:
		Integer AIFSN_AC01 = theBackoffEntityAC01.getDot11EDCAAIFSN();
		Integer CWmin_AC01 = theBackoffEntityAC01.getDot11EDCACWmin();

		message("with the following contention window parameters ...", 10);
		message("    AIFSN[AC01] = " + AIFSN_AC01.toString(), 10);
		message("    CWmin[AC01] = " + CWmin_AC01.toString(), 10);
		message("... the backoff entity queues perform like this:", 10);

		double error = -aCurrentQueueSize;
		double delta_time = this.theSamplingTime_sec;

		double pid_response = this.pid_controller.response(error, this.theSamplingTime_sec);

		// Something like this to regulate e.g. AIFSN_AC01 parameter
		// But I've not implemented it correctly and ran out of time :)
		// theBackoffEntityAC01.setDot11EDCAAIFSN(this.pid_controller.setAIFSN_AC01(AIFSN_AC01));

		// --------------------- ASSIGNMENT -------------------------------
	}
	
	@Override
	public void plot() {
		if (plotter == null) {
			plotter = new JEMultiPlotter("PID Controller, Station " + this.dot11MACAddress.toString(), "max", "time [s]", "MAC Queue", this.theUniqueEventScheduler.getEmulationEnd().getTimeMs() / 1000.0, true);
			plotter.addSeries("current");
			plotter.display();
		}
		plotter.plot(((Double) theUniqueEventScheduler.now().getTimeMs()).doubleValue() / 1000.0, theBackoffEntityAC01.getQueueSize(), 0);
		plotter.plot(((Double) theUniqueEventScheduler.now().getTimeMs()).doubleValue() / 1000.0, theBackoffEntityAC01.getCurrentQueueSize(), 1);
	}

}
