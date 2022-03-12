import os
import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.linear_model import LinearRegression

#data should be csv with LONGEST_PATH (int) and N1...4 (int) fields
data_path = os.path.join("data", "")

df = pd.read_csv(data_path, header=0) 
df.drop_duplicates(inplace=True)

Y = df["LONGEST_PATH"].values
X = df[["N1", "N2", "N3", "N4"]].values

X_train, X_test, Y_train, Y_test = train_test_split(X, Y, test_size=0.05)

regressor = LinearRegression()
regressor.fit(X_train, Y_train)

predictions = regressor.predict(X_test)

errors = abs(predictions - Y_test)
print('Mean Absolute Error:', round(np.mean(errors), 2))
mape = 100 * (errors / Y_test)
accuracy = 100 - np.mean(mape)
print('Accuracy:', round(accuracy, 2), '%')

print(regressor.coef_)
print(regressor.intercept_)
