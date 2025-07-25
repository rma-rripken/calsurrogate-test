# Salinity Surrogate Configuration Guide

This document provides instructions on how to configure the salinity surrogate models within WRIMSv2.  SalinitySurrogateSetup is currently hard-coded to read from: `\wrimsv2\external\config.properties`.

## Selecting the ANN Model

You can configure the ANN used for Salinity predictions and X2 prediction separately.

1.  **Locate Example Configuration Files:** Example config files named for each ANN are already configured for that specific ANN (e.g. `config.schism_cache_ann.properties`).
2.  **Copy and Rename:** Copy one of the examples and rename it `config.properties`.
3.  **Verify ANN Loading:** When the ANN is first loaded during the compute WRIMS outputs a log message with the specific ANN that is being used. This log message can be used to double-check that your desired ANN is being used.

## Multi-Head ANN Configuration

Some ANN models were contrastively trained and include multiple prediction heads. You need to configure which output you'd like WRIMSv2 to use.

## Configuration Parameters

The following parameters need to be configured within the `config.properties` file:

| Parameter                     | Description                                                                                                                    |
| ----------------------------- | ------------------------------------------------------------------------------------------------------------------------------ |
| `salinitySurrogateMultitask` | Path to the salinity surrogate ANN model.                                                                                   |
| `salinitySurrogateOutput`   | Specifies which output layer to use for the salinity prediction.                                                             |
| `x2Surrogate`                 | Path to the X2 surrogate ANN model.                                                                                         |
| `x2SurrogateOutput`         | Specifies which output layer to use for the X2 prediction.                                                                   |

## Example Configuration

Here is an example configuration block for the `config.properties` file:

```properties
# Salinity Surrogate Configuration
# If it's just a single model like dsm2_base, then you probably want StatefulPartitionedCall:0
# If it's scenario-specific like schism_base.suisun_gru2_tf, then base is at 1 and suisun is at 2
salinitySurrogateMultitask=/ann/schism_base.slr_gru2
salinitySurrogateOutput=StatefulPartitionedCall:2
x2Surrogate=/ann/x2_schism_base.slr_gru2
x2SurrogateOutput=StatefulPartitionedCall:2