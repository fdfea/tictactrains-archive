import os
import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import MinMaxScaler
from keras.models import Sequential
from keras.layers import Dense, LeakyReLU

#data should be csv with LONGEST_PATH (int) and N1...33 (int) fields
data_path = os.path.join("data", "")

df = pd.read_csv(data_path, header=0)
df.drop_duplicates(inplace=True, ignore_index=True)

Y = df["LONGEST_PATH"].values.reshape(-1, 1)
X = df.iloc[:,1:len(df.columns)]

scaler = MinMaxScaler()
Y = scaler.fit_transform(Y)
X = scaler.fit_transform(X)
Y = np.ravel(Y)

X_train, X_test, Y_train, Y_test = train_test_split(X, Y, test_size=0.1)

model = Sequential()

model.add(Dense(33, activation='relu', input_dim=33))
model.add(Dense(24))
model.add(Dense(1, activation='relu'))

model.compile(loss='mean_squared_error',
              optimizer='adam',
              metrics=['mse', 'mae', 'mape'])

model.fit(X_train, Y_train, epochs=10, batch_size=100, verbose=1)

Y_pred = model.predict(X_test)
score = model.evaluate(X_test, Y_test, verbose=1)
print(score)
