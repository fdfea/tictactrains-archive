# **TicTacTrains Java**

### **Summary**

This is the original TicTacTrains engine, implemented in Java (not the engine that runs on the website). It allows slightly more configuration than the [C engine](https://github.com/fdfea/tictactrains-engine), but it is more difficult to use (as the code may need to be changed to enable certain features), less performant, and some bugs may exist. Additionally, this program can be used to generate data for use with the [models](../models/). 

### **Usage**

To build the project with Maven, run `mvn clean install` from the root directory. This will create `tictactrains-1.0-SNAPSHOT-dist.zip` in the `target/` directory. Unzip this file with `tar -xvzf tictactrains-1.0-SNAPSHOT-dist.zip`. This will create a directory called `tictactrains-1.0-SNAPSHOT`. Inside, there will be `tictactrains-1.0-SNAPSHOT.jar`. You can run the program using `java -jar tictactrains-1.0-SNAPSHOT.jar`. The default behavior is to play a game of TicTacTrains from the command line. There is a configuration file, `application.conf`, in the `conf/` directory, which can be used to configure the game at runtime. 

To enable other functionality, you will need to modify the code and recompile. To generate data, look at the `DataGenerator.java` file and uncomment the data generator code block in `Main.java`. Then, make your changes, recompile, and run. Generated data will be output to the `data/` directory. To run the engine with multiple threads, uncomment the executor code in `Main.java` and specify the number of threads in `application.conf`. 
