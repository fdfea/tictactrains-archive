import os
import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import MinMaxScaler
from sklearn.neural_network import MLPRegressor
from sklearn.metrics import mean_absolute_error

def mean_absolute_percentage_error(y_test, y_pred): 
    y_test, y_pred = np.array(y_test), np.array(y_pred)
    return np.mean(np.abs((y_test - y_pred) / y_test)) * 100

#data should be csv with LONGEST_PATH (int) and N1...33 (int) fields
data_path = os.path.join("data", "")

df = pd.read_csv(data_path, header=0)
df.drop_duplicates(inplace=True, ignore_index=True)

Y = df["LONGEST_PATH"].values.reshape(-1, 1)
X = df.iloc[:,1:len(df.columns)]

scaler = MinMaxScaler()
Y = scaler.fit_transform(Y)
Y = np.ravel(Y)
X = scaler.fit_transform(X)

X_train, X_test, Y_train, Y_test = train_test_split(X, Y, test_size=0.1)

regressor = MLPRegressor(
    hidden_layer_sizes=(20,),
    activation='relu', 
    solver='adam',
    learning_rate='constant',
    learning_rate_init=1e-3,
    batch_size=1000, 
    alpha=1e-15,
    tol=1e-8,
    beta_1=0.9, 
    beta_2=0.99, 
    epsilon=1e-8,
    max_iter=200, 
    verbose=True,
)

regressor.fit(X_train, Y_train)

y_pred = regressor.predict(X_test)

mae = mean_absolute_error(Y_test, y_pred)
mape = mean_absolute_percentage_error(Y_test, y_pred)

print(f"MAE of {regressor}: {mae}")
print(f"MAPE of {regressor}: {mape}")
print("Coeffs:", regressor.coefs_)
print("Intercepts:", regressor.intercepts_)

#predict a test sample
inputs = X_test[0]
output = Y_test[0]
predicted = y_pred[0]
coeff_matrix_h = np.swapaxes(regressor.coefs_[0], 0, 1)
hidden_vals = np.array([regressor.intercepts_[0][i] \
    for i in range(len(regressor.intercepts_[0]))])
for i, coeff_arr in enumerate(coeff_matrix_h):
    for j, coeff in enumerate(coeff_arr):
        hidden_vals[i] += inputs[j] * coeff
hidden_vals = np.maximum(hidden_vals, 0)
coeff_matrix_o = regressor.coefs_[1].flatten()
output_val = regressor.intercepts_[1][0]
for i, hidden_val in enumerate(hidden_vals):
    output_val += hidden_val * coeff_matrix_o[i]
output_val = max(output_val, 0)

print("True:", output)
print("Pred:", predicted)
print("Calc:", output_val)
