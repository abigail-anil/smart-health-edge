from flask import Flask, request, jsonify
import joblib
import time

# Load trained model
model = joblib.load('glucose_model_xgb.pkl')

app = Flask(__name__)

@app.route('/predict', methods=['POST'])
def predict():
    data = request.json
    try:
        features = [[
            data['PPG_Signal'],
            data['Heart_Rate'],
            data['Systolic_Peak'],
            data['Diastolic_Peak'],
            data['Pulse_Area'],
            data['Age'],
            data['Weight'],
            data['Gender']
        ]]

        start_time = time.time()
        prediction = float(model.predict(features)[0]) 
        latency = round(time.time() - start_time, 4)

        return jsonify({
            "glucose_prediction": round(prediction, 2),
            "latency_seconds": latency
        })
    except Exception as e:
        return jsonify({"error": str(e)}), 400

@app.route('/', methods=['GET'])
def index():
    return '''
        <h2>Edge Node is Running</h2>
        <p>This server accepts sensor data via <code>POST /predict</code>.</p>
        <p>To use it, send a JSON payload with features like:</p>
        <pre>
        {
            "PPG_Signal": 0.95,
            "Heart_Rate": 75,
            "Systolic_Peak": 120,
            "Diastolic_Peak": 80,
            "Pulse_Area": 23.5,
            "Age": 40,
            "Weight": 70,
            "Gender": 1
        }
        </pre>
    '''


if __name__ == '__main__':
    app.run(port=5000)
