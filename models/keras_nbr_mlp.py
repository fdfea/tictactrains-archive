import os
import pandas as pd
import numpy as np
from keras.models import Sequential
from keras.layers import Dense
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler, MinMaxScaler

#data should be csv with LONGEST_PATH (int) and N1...4 (int) fields
data_path = os.path.join("data", "")

df = pd.read_csv(data_path, header=0)
df.drop_duplicates(inplace=True)

Y = df["LONGEST_PATH"].values.reshape(-1, 1)
X = df[["N1","N2","N3","N4"]].values

X_train, X_test, Y_train, Y_test = train_test_split(X, Y, test_size=0.1)

std_scaler = StandardScaler().fit(X_train)
X_train = std_scaler.transform(X_train)
X_test = std_scaler.transform(X_test)

mm_scaler = MinMaxScaler().fit(Y_train)
Y_train = mm_scaler.transform(Y_train)
Y_test = mm_scaler.transform(Y_test)

model = Sequential()

model.add(Dense(4, activation='relu', input_dim=4))
model.add(Dense(2, activation='relu'))
model.add(Dense(1, activation='sigmoid'))

model.compile(loss='mean_squared_error',
              optimizer='sgd',
              metrics=['mse', 'mae', 'mape'])

model.fit(X_train, Y_train, epochs=3, batch_size=1, verbose=1)

Y_pred = model.predict(X_test)
score = model.evaluate(X_test, Y_test, verbose=1)
print(score)

#print("Y_pred: \n", Y_pred[0:20])
#print("Y_test: \n", Y_test[0:20])