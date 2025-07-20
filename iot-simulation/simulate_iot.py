import pandas as pd
import requests
import time

# Load your dataset
df = pd.read_csv('clean-dataset.csv')
df = df.dropna()

# Define expected columns
columns = ['PPG_Signal', 'Heart_Rate', 'Systolic_Peak', 'Diastolic_Peak',
           'Pulse_Area', 'Age', 'Weight', 'Gender']

# Trim to needed columns
df = df[columns]

# Send one row at a time
for index, row in df.iterrows():
    payload = {}
    for k in columns:
        # Explicitly cast every value to a built-in Python float
        payload[k] = float(row[k])

    try:
        # Print payload type to confirm it's clean
        print("Sending:", payload)
        response = requests.post("http://localhost:5000/predict", json=payload)
        print("Received:", response.json())
    except Exception as e:
        print(f"Error at row {index}: {e}")

    time.sleep(1)
