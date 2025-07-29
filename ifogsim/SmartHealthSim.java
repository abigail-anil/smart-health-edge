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

    static List<FogDevice> fogDevices = new ArrayList<>();
    static List<Sensor> sensors = new ArrayList<>();
    static List<Actuator> actuators = new ArrayList<>();

    public static void main(String[] args) {
        Log.printLine("Starting Smart Health Fog Simulation...");

        try {
            Logger.ENABLED = true; // Enable logging

            int numUser = 1;
            Calendar calendar = Calendar.getInstance();
            boolean traceFlag = false;

            CloudSim.init(numUser, calendar, traceFlag);

            String appId = "smart_health";
            FogBroker broker = new FogBroker("broker");

            Application application = createApplication(appId, broker.getId());
            application.setUserId(broker.getId());

            createFogDevices(broker.getId(), appId);

            FogDevice mobileDevice = null;
            for (FogDevice device : fogDevices) {
                if (device.getName().equals("mobile")) {
                    mobileDevice = device;
                    break;
                }
            }

            if (mobileDevice == null) {
                throw new RuntimeException("Mobile device not found!");
            }

            // === Sensor & Actuator Setup ===
            Sensor ppgSensor = new Sensor("PPG_Sensor", "PPG_STREAM", broker.getId(), appId, new DeterministicDistribution(1));
            Actuator displayActuator = new Actuator("actuator", broker.getId(), appId, "DISPLAY_RESULT");

            ppgSensor.setGatewayDeviceId(mobileDevice.getId());
            displayActuator.setGatewayDeviceId(mobileDevice.getId());
            ppgSensor.setLatency(1.0);
            displayActuator.setLatency(1.0);

            ppgSensor.setApp(application);
            displayActuator.setApp(application);

            sensors.add(ppgSensor);
            actuators.add(displayActuator);

            // === Module Mapping ===
            ModuleMapping moduleMapping = ModuleMapping.createModuleMapping();
            moduleMapping.addModuleToDevice("SensorReader", "mobile");
            moduleMapping.addModuleToDevice("Predictor", "edge");
            moduleMapping.addModuleToDevice("DataStorage", "cloud");
            moduleMapping.addModuleToDevice("DisplayModule", "cloud");
            moduleMapping.addModuleToDevice("DisplayActuatorModule", "mobile");


            // === Controller and Application Submission ===
            Controller controller = new Controller("master-controller", fogDevices, sensors, actuators);
            
            System.out.println("--- Module Mapping ---");
            for (Map.Entry<String, List<String>> entry : moduleMapping.getModuleMapping().entrySet()) {
                System.out.println("Module '" + entry.getKey() + "' mapped to device(s): " + entry.getValue());
            }
            System.out.println("----------------------\n");

            
            controller.submitApplication(application, 0, new ModulePlacementMapping(fogDevices, application, moduleMapping));

            // === Debug Info ===
            System.out.println("\n--- Debugging Sensor-AppEdge Link ---");
            System.out.println("Sensor Name: " + ppgSensor.getName());
            System.out.println("Sensor Tuple Type: " + ppgSensor.getTupleType());
            System.out.println("Application ID: " + application.getAppId());

            boolean foundMatchingEdge = false;
            for (AppEdge edge : application.getEdges()) {
                System.out.println(" AppEdge Source: " + edge.getSource() + ", Dest: " + edge.getDestination() + ", Tuple Type: " + edge.getTupleType() + ", Edge Type: " + edge.getEdgeType());
                if (edge.getEdgeType() == AppEdge.SENSOR &&
                        edge.getSource().equals(ppgSensor.getName()) &&
                        edge.getTupleType().equals(ppgSensor.getTupleType())) {
                    System.out.println(" --> MATCH FOUND for PPG_Sensor!");
                    foundMatchingEdge = true;
                }
            }

            if (!foundMatchingEdge) {
                System.out.println("!!! No matching AppEdge found for PPG_Sensor.");
            }
            System.out.println("--- End Debugging --- \n");

            for (AppEdge edge : application.getEdges()) {
                System.out.println("Edge from " + edge.getSource() + " to " + edge.getDestination() + " type: " + edge.getTupleType());
            }

            CloudSim.startSimulation();
            CloudSim.stopSimulation();
            System.out.println("Checking if DisplayActuatorModule received any tuple...");

            Log.printLine("Smart Health Fog Simulation finished!");

        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("Unwanted errors happen");
        }
    }

    private static void createFogDevices(int userId, String appId) {
        FogDevice cloud = createFogDevice("cloud", 44800, 40000, 100, 10000, 0, 0.01, 1650, 1332);
        cloud.setParentId(-1);
        cloud.setUplinkLatency(0);

        FogDevice edge = createFogDevice("edge", 2800, 4000, 100, 10000, 1, 0.0, 107.339, 83.4333);
        edge.setParentId(cloud.getId());
        edge.setUplinkLatency(100);


        FogDevice mobile = createFogDevice("mobile", 1200, 1000, 100, 270, 2, 2.5, 87.53, 82.44);
        mobile.setParentId(edge.getId());
        mobile.setUplinkLatency(50); // Latency from mobile to edge


        fogDevices.add(cloud);
        fogDevices.add(edge);
        fogDevices.add(mobile);
    }

    private static FogDevice createFogDevice(String nodeName, long mips, int ram, long upBw, long downBw,
                                             int level, double ratePerMips, double busyPower, double idlePower) {
        List<Pe> peList = new ArrayList<>();
        peList.add(new Pe(0, new PeProvisionerOverbooking(mips)));

        int hostId = FogUtils.generateEntityId();
        long storage = 1000000;
        int bw = 10000;

        PowerHost host = new PowerHost(
                hostId,
                new RamProvisionerSimple(ram),
                new BwProvisionerOverbooking(bw),
                storage,
                peList,
                new StreamOperatorScheduler(peList),
                new FogLinearPowerModel(busyPower, idlePower)
        );

        List<Host> hostList = new ArrayList<>();
        hostList.add(host);

        FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
                "x86", "Linux", "Xen", host, 10.0, 3.0, 0.05, 0.001, 0.0
        );

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

        application.addAppModule("SensorReader", 100);
        application.addAppModule("Predictor", 100);
        application.addAppModule("DataStorage", 100);
        application.addAppModule("DisplayModule", 100);
        application.addAppModule("DisplayActuatorModule", 100);


        application.addAppEdge("PPG_Sensor", "SensorReader", 1000, 200, 5,
                "PPG_STREAM", Tuple.UP, AppEdge.SENSOR);

        application.addAppEdge("SensorReader", "Predictor", 2000, 500,
                "PREDICTION_TASK", Tuple.UP, AppEdge.MODULE);

        application.addAppEdge("Predictor", "DataStorage", 1000, 100,
                "PREDICTION_RESULT", Tuple.UP, AppEdge.MODULE);

        application.addAppEdge("DataStorage", "DisplayModule", 500, 50, "DISPLAY_RESULT", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("DisplayModule", "DisplayActuatorModule", 100, 20, "DISPLAY_RESULT_FINAL", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("DisplayActuatorModule", "actuator", 10, 5, "ACTUATOR_TRIGGER", Tuple.DOWN, AppEdge.ACTUATOR);

        System.out.println("Added edge and mapping: DISPLAY_RESULT → DISPLAY_RESULT_FINAL");

        application.addTupleMapping("SensorReader", "PPG_STREAM", "PREDICTION_TASK", new FractionalSelectivity(1.0));
        application.addTupleMapping("Predictor", "PREDICTION_TASK", "PREDICTION_RESULT", new FractionalSelectivity(1.0));
        application.addTupleMapping("DataStorage", "PREDICTION_RESULT", "DISPLAY_RESULT", new FractionalSelectivity(1.0));
        application.addTupleMapping("DisplayModule", "DISPLAY_RESULT", "DISPLAY_RESULT_FINAL", new FractionalSelectivity(1.0));
        
        System.out.println("Mapping: DISPLAY_RESULT_FINAL --> ACTUATOR_TRIGGER");
        application.addTupleMapping("DisplayActuatorModule", "DISPLAY_RESULT_FINAL", "ACTUATOR_TRIGGER", new FractionalSelectivity(1.0));
        
        application.setLoops(Collections.singletonList(new AppLoop(Arrays.asList(
        		"PPG_Sensor","SensorReader", "Predictor", "DataStorage","DisplayModule"
        ))));

        return application;
    }
}
