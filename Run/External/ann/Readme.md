# ANN Models Documentation

**Date**: 7/27/2025

The ANN models in this directory were generated from the data and Python code in the [Casanntra project](https://github.com/CADWRDeltaModeling/casanntra/commit/44f0cfacb0be8922031251f9572aa6c91659e79c) on June 2, 2025.

Additional changes were applied from: [Pull Request #17](https://github.com/CADWRDeltaModeling/casanntra/pull/17)

## Getting Started

To generate the models, run:
```bash
python example\transfer_example.py 
python example\x2_transfer_example.py
```

## Configuration for WRIMSv2

To use these ANN models in a WRIMSv2 run, adjust the file: `Run\External\wrimsv2\external\config.properties`

There are example config files named for each ANN that are already configured for that specific ANN (e.g., `config.schism_cache_ann.properties`). You can copy one of the examples and rename it to `config.properties`.

When the ANN is first loaded during computation, WRIMS outputs a log message with the specific ANN being used. This log message can be used to verify that your desired ANN is being used.

## ANN Folder Naming Convention

### Model Types
- **Salinity ANN**: Standard naming
- **X2 ANN**: Start with an `x2_` prefix in the folder name

### Training Types
There are three types of ANN training used in Casanntra:

1. **Traditional ANN training**
2. **"Direct" transfer learning** - Similar to fine-tuning
3. **"Contrastive" transfer learning** - Adds a contrast term to the training objective

In the naming convention, period characters (`.`) indicate that a transfer learning step has been applied.

### Examples

| Model Name | Description |
|------------|-------------|
| `dsm2_base_gru2` | Traditional ANN training on DSM2 outputs from the "base" scenario |
| `dsm2.schism_base_gru2` | Starting from `dsm2_base_gru2`, additional Direct transfer learning on SCHISM output from the "base" scenario |
| `schism_base.suisun_gru2` | Starting from `dsm2.schism_base_gru2`, additional Contrastive training using "suisun" scenario output |

## Config File Naming

ANN models resulting from Contrastive training include multiple output heads that simultaneously produce predictions for both cases they were trained on. WRIMS needs to know which prediction to use.

Config file names include a final `base` or `ann` term:
- **`base` configs**: Set up to predict the base condition
- **`ann` configs**: Set up to predict the scenario-specific outputs

### Examples

| Config File | Description |
|-------------|-------------|
| `config.schism_cache_ann.properties` | Uses `schism_base.cache_gru2` and `x2_schism_base.cache_gru2`, configured to output predictions for the Cache scenario |
| `config.schism_cache_base.properties` | Uses `schism_base.cache_gru2` and `x2_schism_base.cache_gru2`, configured to output predictions for the Base scenario |

## Why Use "Base" Output from Contrastive ANN?

The contrastive training process offers several advantages:

1. **Improved Performance**: Uses roughly twice the training data, improving both base and scenario predictions
2. **Consistent Comparison**: Neural network training includes randomness. When comparing predictions from two different models, it's challenging to determine whether trends are from differences in the training process or from trends in the training data
3. **Shared Representation**: The contrastive term is introduced late in the transfer learning process, ensuring both output heads base their decisions on the same learned representation
4. **Better Trend Visualization**: Allows plots with both predictions to better highlight contrastive trends that might be obscured by noise from independently trained ANNs
