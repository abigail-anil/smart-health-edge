from azure.iot.device import IoTHubDeviceClient, Message
import time
import random
import json  

# Connection string for the IoT Hub device. This allows the device to authenticate with Azure IoT Hub.
CONNECTION_STRING = "HostName=iothub-fog.azure-devices.net;DeviceId=iot-device1;SharedAccessKey=mVFNDRzyr1Wwmg3XnwWkDHN198U3nWyO92LvS947kUk="

def simulate_sensor_data():
    """Generates a dictionary with simulated sensor readings and user data."""
    return {
        "timestamp_sent": time.time(),
        "PPG_Signal": round(random.uniform(0.5, 1.5), 2),
        "Heart_Rate": random.randint(60, 100),
        "Systolic_Peak": round(random.uniform(100, 140), 1),
        "Diastolic_Peak": round(random.uniform(60, 90), 1),
        "Pulse_Area": round(random.uniform(10, 30), 2),
        "Age": 30,
        "Weight": 70,
        "Gender": 1  # 1 = Male, 0 = Female
    }

def main():
    """Main function to create a client, connect, and send simulated data."""
    # Creates an instance of IoTHubDeviceClient using the connection string
    client = IoTHubDeviceClient.create_from_connection_string(CONNECTION_STRING)
    client.connect()

    while True:
        data = simulate_sensor_data()
        message = Message(json.dumps(data)) # Converts the dictionary into a JSON string and creates a Message object
        print(f"Sending message: {data}")
        client.send_message(message) # Sends the message to the IoT Hub
        time.sleep(5)

if __name__ == "__main__":
    main()
