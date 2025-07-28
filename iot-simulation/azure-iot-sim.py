from azure.iot.device import IoTHubDeviceClient, Message
import time
import random
import json  

CONNECTION_STRING = "HostName=iothub-fog.azure-devices.net;DeviceId=iot-device1;SharedAccessKey=mVFNDRzyr1Wwmg3XnwWkDHN198U3nWyO92LvS947kUk="

def simulate_sensor_data():
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
    client = IoTHubDeviceClient.create_from_connection_string(CONNECTION_STRING)
    client.connect()

    while True:
        data = simulate_sensor_data()
        message = Message(json.dumps(data))
        print(f"Sending message: {data}")
        client.send_message(message)
        time.sleep(5)

if __name__ == "__main__":
    main()
