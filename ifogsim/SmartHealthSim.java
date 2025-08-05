package org.fog.test.perfeval;

import java.util.*;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;

import org.fog.application.*;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.entities.*;
import org.fog.placement.*;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.*;
import org.fog.utils.distribution.DeterministicDistribution;

public class SmartHealthSim {

    // Lists to store the created Fog devices, sensors, and actuators
    static List<FogDevice> fogDevices = new ArrayList<>();
    static List<Sensor> sensors = new ArrayList<>();
    static List<Actuator> actuators = new ArrayList<>();

    public static void main(String[] args) {
        Log.printLine("Starting Smart Health Fog Simulation...");

        try {
            Logger.ENABLED = true;

            int numUser = 1;
            Calendar calendar = Calendar.getInstance();
            boolean traceFlag = false;

            // Initializes the CloudSim toolkit
            CloudSim.init(numUser, calendar, traceFlag);

            String appId = "smart_health";
            FogBroker broker = new FogBroker("broker");

            Application application = createApplication(appId, broker.getId());
            application.setUserId(broker.getId());

            createFogDevices(broker.getId(), appId);

            // Finds the mobile device from the list of created Fog devices
            FogDevice mobileDevice = fogDevices.stream().filter(d -> d.getName().equals("mobile")).findFirst().orElseThrow();

            // Main sensor
            Sensor ppgSensor = new Sensor("PPG_Sensor", "PPG_STREAM", broker.getId(), appId, new DeterministicDistribution(1));
            Actuator displayActuator = new Actuator("actuator", broker.getId(), appId, "DISPLAY_RESULT");

            // Creates a list of additional sensors
            List<Sensor> additionalSensors = Arrays.asList(
                new Sensor("HeartRate_Sensor", "HEART_RATE_STREAM", broker.getId(), appId, new DeterministicDistribution(1)),
                new Sensor("SystolicPeak_Sensor", "SYSTOLIC_PEAK_STREAM", broker.getId(), appId, new DeterministicDistribution(1)),
                new Sensor("DiastolicPeak_Sensor", "DIASTOLIC_PEAK_STREAM", broker.getId(), appId, new DeterministicDistribution(1)),
                new Sensor("PulseArea_Sensor", "PULSE_AREA_STREAM", broker.getId(), appId, new DeterministicDistribution(1)),
                new Sensor("WeightGender_Sensor", "WEIGHT_GENDER_STREAM", broker.getId(), appId, new DeterministicDistribution(1))
            );

            // Sets properties for the PPG sensor and adds it to the list
            ppgSensor.setGatewayDeviceId(mobileDevice.getId());
            ppgSensor.setLatency(1.0);
            ppgSensor.setApp(application);
            sensors.add(ppgSensor);

            // Sets properties for the additional sensors and adds them to the list
            for (Sensor s : additionalSensors) {
                s.setGatewayDeviceId(mobileDevice.getId());
                s.setLatency(1.0);
                s.setApp(application);
                sensors.add(s);
            }

            // Sets properties for the display actuator and adds it to the list
            displayActuator.setGatewayDeviceId(mobileDevice.getId());
            displayActuator.setLatency(1.0);
            displayActuator.setApp(application);
            actuators.add(displayActuator);

            // Defines the module placement mapping
            ModuleMapping moduleMapping = ModuleMapping.createModuleMapping();
            moduleMapping.addModuleToDevice("SensorReader", "mobile");
            moduleMapping.addModuleToDevice("Predictor", "edge");
            moduleMapping.addModuleToDevice("DataStorage", "cloud");
            moduleMapping.addModuleToDevice("DisplayModule", "cloud");
            moduleMapping.addModuleToDevice("DisplayActuatorModule", "mobile");

            // Creates the controller and submits the application with the defined module placement
            Controller controller = new Controller("master-controller", fogDevices, sensors, actuators);
            controller.submitApplication(application, 0, new ModulePlacementMapping(fogDevices, application, moduleMapping));

            // Starts the simulation
            CloudSim.startSimulation();
            CloudSim.stopSimulation(); // Stops the simulation
            Log.printLine("Smart Health Fog Simulation finished!");

        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("Unwanted errors happen");
        }
    }

    private static void createFogDevices(int userId, String appId) {
        // Creates the cloud device
        FogDevice cloud = createFogDevice("cloud", 44800, 40000, 100, 10000, 0, 0.01, 1650, 1332);
        cloud.setParentId(-1);
        cloud.setUplinkLatency(0);

        // Creates the edge device
        FogDevice edge = createFogDevice("edge", 2800, 4000, 100, 10000, 1, 0.0, 107.339, 83.4333);
        edge.setParentId(cloud.getId());
        edge.setUplinkLatency(100);

        // Creates the mobile device
        FogDevice mobile = createFogDevice("mobile", 1200, 1000, 100, 270, 2, 2.5, 87.53, 82.44);
        mobile.setParentId(edge.getId());
        mobile.setUplinkLatency(50);

        // Adds all created devices to the list
        fogDevices.add(cloud);
        fogDevices.add(edge);
        fogDevices.add(mobile);
    }

    private static FogDevice createFogDevice(String nodeName, long mips, int ram, long upBw, long downBw,
                                             int level, double ratePerMips, double busyPower, double idlePower) {
        // Creates a list of processing elements
        List<Pe> peList = new ArrayList<>();
        peList.add(new Pe(0, new PeProvisionerOverbooking(mips)));

        int hostId = FogUtils.generateEntityId();
        long storage = 1000000;
        int bw = 10000;

        // Creates a PowerHost to represent the physical host of the Fog device
        PowerHost host = new PowerHost(hostId,
                new RamProvisionerSimple(ram),
                new BwProvisionerOverbooking(bw),
                storage, peList,
                new StreamOperatorScheduler(peList),
                new FogLinearPowerModel(busyPower, idlePower));

        // Adds the host to a list
        List<Host> hostList = new ArrayList<>();
        hostList.add(host);

        // Defines the characteristics of the Fog device
        FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
                "x86", "Linux", "Xen", host, 10.0, 3.0, 0.05, 0.001, 0.0);

        FogDevice device = null;
        try {
            device = new FogDevice(nodeName, characteristics,
                    new AppModuleAllocationPolicy(hostList), new LinkedList<>(), 10, upBw, downBw, 0, ratePerMips);
        } catch (Exception e) {
            e.printStackTrace();
        }

        device.setLevel(level);
        return device;
    }

    private static Application createApplication(String appId, int userId) {
        Application application = Application.createApplication(appId, userId);

        // Adds application modules with their MIPS demand
        application.addAppModule("SensorReader", 100);
        application.addAppModule("Predictor", 100);
        application.addAppModule("DataStorage", 100);
        application.addAppModule("DisplayModule", 100);
        application.addAppModule("DisplayActuatorModule", 100);

        // Adds application edges that define the data flow and characteristics between sensors and modules
        application.addAppEdge("PPG_Sensor", "SensorReader", 1000, 200, 5, "PPG_STREAM", Tuple.UP, AppEdge.SENSOR);
        application.addAppEdge("HeartRate_Sensor", "SensorReader", 1000, 200, 5, "HEART_RATE_STREAM", Tuple.UP, AppEdge.SENSOR);
        application.addAppEdge("SystolicPeak_Sensor", "SensorReader", 1000, 200, 5, "SYSTOLIC_PEAK_STREAM", Tuple.UP, AppEdge.SENSOR);
        application.addAppEdge("DiastolicPeak_Sensor", "SensorReader", 1000, 200, 5, "DIASTOLIC_PEAK_STREAM", Tuple.UP, AppEdge.SENSOR);
        application.addAppEdge("PulseArea_Sensor", "SensorReader", 1000, 200, 5, "PULSE_AREA_STREAM", Tuple.UP, AppEdge.SENSOR);
        application.addAppEdge("WeightGender_Sensor", "SensorReader", 1000, 200, 5, "WEIGHT_GENDER_STREAM", Tuple.UP, AppEdge.SENSOR);

        // Adds edges between modules
        application.addAppEdge("SensorReader", "Predictor", 2000, 500, "PREDICTION_TASK", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("Predictor", "DataStorage", 1000, 100, "PREDICTION_RESULT", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("DataStorage", "DisplayModule", 500, 50, "DISPLAY_RESULT", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("DisplayModule", "DisplayActuatorModule", 100, 20, "DISPLAY_RESULT_FINAL", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("DisplayActuatorModule", "actuator", 10, 5, "ACTUATOR_TRIGGER", Tuple.DOWN, AppEdge.ACTUATOR);

        // Defines the tuple mappings (selectivity) between modules
        application.addTupleMapping("SensorReader", "PPG_STREAM", "PREDICTION_TASK", new FractionalSelectivity(1.0));
        application.addTupleMapping("SensorReader", "HEART_RATE_STREAM", "PREDICTION_TASK", new FractionalSelectivity(1.0));
        application.addTupleMapping("SensorReader", "SYSTOLIC_PEAK_STREAM", "PREDICTION_TASK", new FractionalSelectivity(1.0));
        application.addTupleMapping("SensorReader", "DIASTOLIC_PEAK_STREAM", "PREDICTION_TASK", new FractionalSelectivity(1.0));
        application.addTupleMapping("SensorReader", "PULSE_AREA_STREAM", "PREDICTION_TASK", new FractionalSelectivity(1.0));
        application.addTupleMapping("SensorReader", "WEIGHT_GENDER_STREAM", "PREDICTION_TASK", new FractionalSelectivity(1.0));

        application.addTupleMapping("Predictor", "PREDICTION_TASK", "PREDICTION_RESULT", new FractionalSelectivity(1.0));
        application.addTupleMapping("DataStorage", "PREDICTION_RESULT", "DISPLAY_RESULT", new FractionalSelectivity(1.0));
        application.addTupleMapping("DisplayModule", "DISPLAY_RESULT", "DISPLAY_RESULT_FINAL", new FractionalSelectivity(1.0));
        application.addTupleMapping("DisplayActuatorModule", "DISPLAY_RESULT_FINAL", "ACTUATOR_TRIGGER", new FractionalSelectivity(1.0));

        // Defines the application loops for monitoring latency and dependencies
        application.setLoops(Collections.singletonList(new AppLoop(Arrays.asList(
                "SensorReader", "Predictor", "DataStorage", "DisplayModule"
        ))));

        return application;
    }
}
