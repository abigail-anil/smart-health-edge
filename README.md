Project Title
Latency-Aware Smart Health Monitoring: Edge vs Cloud Execution using iFogSim and Azure IoT Hub

1. Project Overview
This project demonstrates a smart health monitoring system leveraging IoT, Edge Computing, and Cloud Integration.

It consists of:

Azure real-time pipeline for low-latency health data processing

iFogSim simulation for evaluating edge vs cloud deployments in terms of latency, energy, network usage, and cost

Key Features:

* Simulated IoT health data (heart rate, blood pressure, pulse signals)

* Edge-based ML inference using XGBoost

* Azure IoT Hub → Event Hub → Azure Function → Edge ML pipeline

* iFogSim simulation comparing edge vs cloud module placement

2. Project Architecture
Azure Real-Time Prototype:

Python IoT simulation → IoT Hub → Event Hub → Azure Function → Flask-based XGBoost (via ngrok) → Cloud Logging

iFogSim Simulation:

Edge-enhanced vs Cloud-centric predictor placement

Metrics: Application loop delay, energy usage, network traffic, cost

4. How to Run
A. Azure Real-Time Prototype
Deploy Azure IoT Hub and Event Hub

Create an Azure Function triggered by Event Hub messages

Run Flask edge model (with ngrok or IoT Edge)

Start iot_simulation.py to stream data

B. iFogSim Simulation
Open Eclipse 

Import iFogSim

Run:

SmartHealthSim.java → Edge Deployment

SmartHealthSim_Cloud.java → Cloud Deployment

Compare results in the console logs

5. Results
Edge Deployment:-

Lower network usage and simulated cost

Distributed energy load with reduced cloud dependency

Cloud Deployment:-

Higher network traffic and energy consumption

Slightly faster per-tuple computation

Azure Real-Time:-

Sub-second end-to-end latency (~280 ms)

Validates feasibility for near real-time health monitoring