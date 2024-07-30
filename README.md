
# 2D-AI Plane-AutoPilot

## Description

The 2D-AI Plane-AutoPilot project is designed to train a neural network model to classify movement directions based on input features from game data. The project leverages the Encog library to implement and train the neural network. The trained model can be used to automate the piloting of a 2D plane in a game environment.

## Prerequisites

- **Java Development Kit (JDK)**: Ensure you have JDK installed (version 8 or higher is recommended).
- **Encog Library**: Version 3.4 is required for neural network implementation and training.

## Project Structure

- `.classpath`, `.gitattributes`, `.gitignore`, `.project`: Configuration and metadata files.
- `README.md`: Project documentation.
- `classificationModel.eg`: File to save the trained neural network model.
- `game_data.csv`: CSV file containing sample game data used for training the model.
- `ie/`, `images/`, `training/`: Directories for additional project resources and scripts.
- `training/GameModelTrainer.java`: Main script to train the neural network model.

## Setup Instructions

1. **Clone the Repository**:
    ```sh
    git clone https://github.com/yourusername/2D-AI-Plane-AutoPilot.git
    cd 2D-AI-Plane-AutoPilot
    ```

2. **Install Encog 3.4**:
    - Download Encog 3.4 from the [official Encog website](http://www.heatonresearch.com/encog).
    - Add the Encog library to your Java project. Simply by adding it to the project's classpath.

3. **Compile and Run the Training Script**:
    - Open `training/GameModelTrainer.java` in your preferred Java IDE.
    - Ensure the path to `game_data.csv` is correct in the `GameModelTrainer` class.
    - Compile and run the `GameModelTrainer` class to start training the model.

## Training the Model

The `GameModelTrainer.java` script is responsible for training the neural network model. It performs the following steps:

1. **Load Data**: Reads game data from `game_data.csv`.
2. **Define Network Architecture**: Sets up a neural network with an input layer, a hidden layer with ReLU activation, and an output layer with SoftMax activation.
3. **Train the Model**: Uses backpropagation to train the model until the error is below 0.01 or for a maximum of 1000 epochs.
4. **Save the Model**: The trained model is saved to `classificationModel.eg`.

**Note**: Sample game data is included in `game_data.csv` which was used for initial training. You can delete the sample data and play the game to collect your own data for training the model.

## Usage

1. **Play the Game and Collect Data**:
    - Play the game to generate and collect data, which can be used to train the model.
    - 
2. **Train the model**:
    - Run the GameModelTrainer script to train the model.
    
3. **Use the Trained Model**:
    - After training the model, update the game to use the trained model by changing `view = new GameView(false);` to `view = new GameView(true);` in the game code. This will switch the input from player input to the trained model's predictions.

Once the model is trained, it can be integrated into the game environment to automate the piloting of the 2D plane. The model will classify movement directions based on real-time input features.
