package training;

import org.encog.Encog;
import org.encog.engine.network.activation.ActivationReLU;
import org.encog.engine.network.activation.ActivationSoftMax;
import org.encog.engine.network.activation.ActivationTANH;
import org.encog.ml.data.MLDataSet;
import org.encog.ml.data.basic.BasicMLDataSet;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.layers.BasicLayer;
import org.encog.neural.networks.training.propagation.back.Backpropagation;
import org.encog.neural.networks.training.propagation.resilient.ResilientPropagation;
import org.encog.persist.EncogDirectoryPersistence;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GameModelTrainer {

    private static final String CSV_FILE = "C:\\Users\\User\\Desktop\\aiAssignment-2024-Stubs\\game_data.csv"; // Update this path

    public static void trainClassificationModel() {
        MLDataSet dataSet = loadCSV(CSV_FILE);

        int INPUT_SIZE = dataSet.getInputSize();
        int OUTPUT_SIZE = 3; 

        BasicNetwork network = new BasicNetwork();
        network.addLayer(new BasicLayer(null, true, INPUT_SIZE));
        network.addLayer(new BasicLayer(new ActivationReLU(), true, 25)); 
        network.addLayer(new BasicLayer(new ActivationSoftMax(), false, 3));
        network.getStructure().finalizeStructure();
        network.reset();

        final Backpropagation train = new Backpropagation(network, dataSet);

        int epoch = 1;
        do {
            train.iteration();
            System.out.println("Epoch #" + epoch + " Error:" + train.getError());
            epoch++;
        } while (train.getError() > 0.01 && epoch <= 1000); 

        EncogDirectoryPersistence.saveObject(new File("classificationModel.eg"), network);
        Encog.getInstance().shutdown();
    }

    private static MLDataSet loadCSV(String csvFile) {
        List<double[]> inputData = new ArrayList<>();
        List<double[]> idealData = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                double[] input = new double[values.length - 1];
                double[] ideal = new double[3]; // One hot vector for movement

                for (int i = 0; i < values.length - 1; i++) {
                    input[i] = Double.parseDouble(values[i]);
                }
                int movementIndex = (int)Double.parseDouble(values[values.length - 1]);
                // Movement for one hot encoding
                ideal[movementIndex + 1] = 1.0; 

                inputData.add(input);
                idealData.add(ideal);
                
               //System.out.println("Input Array: " + Arrays.toString(input));
              // System.out.println("Output Array: " + Arrays.toString(ideal));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        double[][] inputArray = inputData.toArray(new double[0][]);
        double[][] idealArray = idealData.toArray(new double[0][]);
        
        return new BasicMLDataSet(inputArray, idealArray);
    }

    public static void main(String[] args) {
        trainClassificationModel();
    }
}
