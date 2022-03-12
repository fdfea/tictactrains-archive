import os
import math
import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
from sklearn.metrics import accuracy_score, confusion_matrix, classification_report
from sklearn.linear_model import LogisticRegression
from sklearn.neural_network import MLPClassifier, MLPRegressor

def mean_absolute_percentage_error(y_test, y_pred): 
    y_test, y_pred = np.array(y_test), np.array(y_pred)
    return np.mean(np.abs((y_test - y_pred) / y_test)) * 100

#data should be csv with RESULT (float) and X_LONGEST_N1...4, O_LONGEST_N1...4 (int) fields
data_path = os.path.join("data", "")

df = pd.read_csv(data_path, header=0) 

df.drop(df.loc[df["RESULT"] == 0.5].index, inplace=True)

df["RESULT"] = (df["RESULT"] == 1.0).astype(int)

df.drop(df.loc[df["RESULT"] == 1.0].index[0:1300000], inplace=True)

Y = df["RESULT"].values
X = df[["X_LONGEST_N1","X_LONGEST_N2","X_LONGEST_N3","X_LONGEST_N4",
        "O_LONGEST_N1","O_LONGEST_N2","O_LONGEST_N3","O_LONGEST_N4"]].values

lb = LabelEncoder()
Y = lb.fit_transform(Y)

X_train, X_test, Y_train, Y_test = train_test_split(X, Y, test_size=0.1)

classifier = LogisticRegression()

classifier.fit(X_train, Y_train)

y_pred= classifier.predict(X_test)

acc = accuracy_score(Y_test, y_pred)
cm = confusion_matrix(Y_test, y_pred)
clsrp = classification_report(Y_test, y_pred)

print("Accuracy of %s: %s"%(classifier, acc))
print("Confusion Matrix of %s: \n%s"%(classifier, cm))
print("Classification Report of %s: \n%s"%(classifier, clsrp))

print("Coefficients:", classifier.coef_, classifier.intercept_)

##### OTHER CLASSIFIERS TESTED #####

# 97.3%, 90%
#classifier = MLPClassifier()

# 96.2%
#classifier = RandomForestClassifier(n_estimators=50, criterion='gini', verbose=2)

# 96.6%
#classifier = KNeighborsClassifier(n_neighbors=200)

# 94.5%
#classifier = DecisionTreeClassifier()

# 96.5%
#classifier = SGDClassifier()

# 91.1%
#classifier = GaussianNB()

# 96.2%
#classifier = LinearDiscriminantAnalysis()

# 96.5%
#classifier = LinearSVC()

# 96.6%, 89%
#classifier = AdaBoostClassifier()

# 96.3%
#classifier = GradientBoostingClassifier()
