import pandas as pd
#from sklearn.ensemble import RandomForestRegressor
from xgboost import XGBRegressor
from sklearn.model_selection import train_test_split
from sklearn.metrics import mean_squared_error, r2_score
import joblib

# Load dataset
df = pd.read_csv('clean-dataset.csv')

# Define features and target
features = ['PPG_Signal', 'Heart_Rate', 'Systolic_Peak', 'Diastolic_Peak', 'Pulse_Area', 'Age', 'Weight', 'Gender']
target = 'Glucose_level'

print(df.isnull().sum())

# Drop missing values
df = df[features + [target]].dropna()

df['Gender'] = df['Gender'].map({'Male': 1, 'Female': 0}) 

# Prepare inputs and outputs
X = df[features]
y = df[target]

# Train-test split
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

# Train XGBoost model
model = XGBRegressor(
    n_estimators=200,
    max_depth=6,
    learning_rate=0.1,
    subsample=0.8,
    colsample_bytree=0.8,
    random_state=42
)
model.fit(X_train, y_train)

# Predict and evaluate
y_pred = model.predict(X_test)
mse = mean_squared_error(y_test, y_pred)
r2 = r2_score(y_test, y_pred)

print(f"Model evaluation:\nMSE = {mse:.2f}\nR² = {r2:.2f}")

# Save model
joblib.dump(model, 'glucose_model_xgb.pkl')