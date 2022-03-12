import os
import pandas as pd
import numpy as np
from keras.models import Sequential
from keras.layers import Conv2D, MaxPooling2D, Dense, Flatten
from keras.utils import to_categorical
from sklearn.model_selection import train_test_split

#data should be csv with RESULT (float) and FINAL_BOARD (int64) fields 
data_path = os.path.join("data", "")

df = pd.read_csv(data_path, header=0)

#df.drop(df.loc[df["RESULT"] == 0.5].index, inplace=True)

Y = df["RESULT"].values.reshape(-1, 1)
X = df["FINAL_BOARD"].values

X = np.array([np.array(list(np.binary_repr(x).zfill(49))).astype(np.int8).reshape(7,7) for x in X])

X = X.reshape((len(X), 7, 7, 1))

X_train, X_test, Y_train, Y_test = train_test_split(X, Y, test_size=0.1)

k_classes = 3
Y_train = to_categorical(Y_train, num_classes=k_classes)
Y_test = to_categorical(Y_test, num_classes=k_classes)

model = Sequential()
model.add(Conv2D(32, (3, 3), activation='relu', input_shape=(7, 7, 1), padding='same'))
model.add(Conv2D(32, (2, 2), activation='relu', padding='same'))
model.add(MaxPooling2D((2, 2), padding='same'))
model.add(Conv2D(64, (3, 3), activation='relu', padding='same'))
model.add(Conv2D(64, (2, 2), activation='relu', padding='same'))
model.add(MaxPooling2D((2, 2), padding='same'))
model.add(Flatten())
model.add(Dense(64, activation='relu'))
model.add(Dense(k_classes, activation='softmax'))

model.compile(loss='categorical_crossentropy',
              optimizer='sgd',
              metrics=['accuracy'])

model.fit(X_train, Y_train, batch_size=10, epochs=4, verbose=1)

score = model.evaluate(X_test, Y_test)

print(score)
