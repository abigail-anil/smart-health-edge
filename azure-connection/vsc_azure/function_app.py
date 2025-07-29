import azure.functions as func
import logging
import json
import requests
import time

app = func.FunctionApp()

@app.event_hub_message_trigger(
    arg_name="event",
    event_hub_name="%IoTHubEventHubName%",
    connection="IoTHubConnection"
)
def ProcessIoTMessages(event: func.EventHubEvent):
    function_start = time.time()
    logging.info('Function triggered by IoT Hub Event.')

    # Get the message body
    message_body = event.get_body()

    if isinstance(message_body, bytes):
        try:
            message_str = message_body.decode('utf-8')
        except Exception as e:
            logging.error(f"Failed to decode message as UTF-8: {e}")
            return
    else:
        message_str = str(message_body)

    logging.info(f"Processing message: {message_str[:100]}...")

    try:
        # Parse sensor data JSON
        sensor_data = json.loads(message_str)
        logging.info(f"Received sensor data: {sensor_data}")

        # Capture timestamp sent from device if available
        timestamp_sent = sensor_data.get("timestamp_sent")

        # Call the Flask ML API and track latency
        edge_node_url = "https://2b57f45207f9.ngrok-free.app/predict"
        api_start = time.time()
        response = requests.post(edge_node_url, json=sensor_data)
        api_end = time.time()
        response.raise_for_status()

        prediction = response.json()
        logging.info(f"Edge Node Prediction: {prediction}")

        # Log latencies
        function_end = time.time()
        total_function_latency = (function_end - function_start) * 1000
        flask_api_latency = (api_end - api_start) * 1000

        logging.info(f"Azure Function total processing latency: {total_function_latency:.2f} ms")
        logging.info(f"Flask API call latency: {flask_api_latency:.2f} ms")

        if timestamp_sent:
            end_to_end_latency = (function_end - float(timestamp_sent)) * 1000
            logging.info(f"End-to-End latency (device to prediction complete): {end_to_end_latency:.2f} ms")

    except json.JSONDecodeError as e:
        logging.error(f"JSON decoding failed: {e}. Raw: {message_str}")
    except requests.RequestException as e:
        logging.error(f"Failed to reach Edge Node: {e}")
    except Exception as e:
        logging.error(f"Unexpected error: {e}")
