package wrimsv2.external;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import wrimsv2.components.ControlData;
import wrimsv2.components.TimeUsage;
import wrimsv2.external.ExternalFunction;
import wrimsv2.ilp.ILP;
import wrimsv2.config.ConfigUtils;
import calsim.surrogate.*;



/*  ANNCommon2.wresl
define JP {value 1}
define RS {value 2}
define EM {value 3}
define AN {value 4}
define CO {value 5}
define CH {value 6}
define LV {value 7}
define MR {value 8}
define VI {value 9}
define CV {value 10}
define CC {value 11}
define CI {value 12}
define BI {value 15
define BL {value 20}
define MRZ {value 21}
define X2 {value 30}
*/

/* Current codes in SalinitySurrogateManager and in CalSim which I got from the fortran code. 
 * These have to be manually coordinated
	public final int JER_CALSIM = 1; // Jersey Point
	public final int RSL_CALSIM = 2; // Rock Slough
	public final int EMM_CALSIM = 3; // Emmaton
	public final int ANH_CALSIM = 4; // Antioch
	public final int CLL_CALSIM = 5; // Collinsville
	public final int MAL_CALSIM = 6; // Mallard is Chipps
	public final int LVR_CALSIM = 7; // Los Vaqueros Intake
	public final int MDR_CALSIM = 8; // Middle River Intake
	public final int CCW_CALSIM = 9; // Victoria River Intake
	public final int TRP_CALSIM = 10; // CVP Tracy Intake
	public final int CCF_CALSIM = 11;
	public final int CCI_CALSIM = 12;
	
	public final int BDL_CALSIM = 20; // Beldons Landing
	public final int MRZ_CALSIM = 21;
	public final int X2_CALSIM =  30; // X2
 */


public class SalinitySurrogateSetup {
	// There is a Calsim config file we can use.  I found that my config changes
	// to that file wouldn't persist.  Changes done in the UI would overwrite the config
	// and any ANN related config options I had added there would be gone.
	private static final String PROPERTIES_FILE = "/wrimsv2/external/config.properties"; // relative to the current class
	public static Properties annProperties = null;
	// For development or debugging it can be useful to have config.properties reloaded at every use.
	// The default is to not reload and that will be more performant.
	public final static boolean alwaysLoadProperties = Boolean.getBoolean("reloadAnnProps");
	public final static String CONFIG_KEY_EC = "salinitySurrogateMultitask";
	public final static String CONFIG_KEY_EC_OUT = "salinitySurrogateOutput";
	public final static String CONFIG_KEY_X2 = "x2Surrogate";
	public final static String CONFIG_KEY_X2_OUT = "x2SurrogateOutput";

	static boolean isInitialized = false;
	static SalinitySurrogateManager ssm;


	// This is a map between the integer codes CalSim uses and the python (zero-based)
	// output index within the ANN. The latter is ANN specific, as is the number of surrogates
	//
	private static final Map<Integer, Integer> outputIndexMap = new HashMap<>();
	static {
		outputIndexMap.put(SalinitySurrogateManager.JER_CALSIM, 11);
		outputIndexMap.put(SalinitySurrogateManager.RSL_CALSIM, 16);
		outputIndexMap.put(SalinitySurrogateManager.MRZ_CALSIM, 12);
		outputIndexMap.put(SalinitySurrogateManager.MAL_CALSIM, 3);
		outputIndexMap.put(SalinitySurrogateManager.BAC_CALSIM, 15);
		outputIndexMap.put(SalinitySurrogateManager.EMM_CALSIM, 8);
		outputIndexMap.put(SalinitySurrogateManager.ANH_CALSIM, 10);
		outputIndexMap.put(SalinitySurrogateManager.CLL_CALSIM, 7);
		outputIndexMap.put(SalinitySurrogateManager.BDL_CALSIM, 5);
    	outputIndexMap.put(SalinitySurrogateManager.MRZ_CALSIM,  1);
		outputIndexMap.put(SalinitySurrogateManager.X2_CALSIM,  30);
	}

	public static String readPropString(String key, String defaultValue) {

        if( annProperties == null || alwaysLoadProperties){
            // Load the properties file dynamically each time
            try (InputStream inputStream = SalinitySurrogateSetup.class.getResourceAsStream(PROPERTIES_FILE)) {
                if (inputStream == null) {
                    System.err.println("Properties file not found at: " + PROPERTIES_FILE);
                    return defaultValue;
                }
                Properties properties = new Properties();
                properties.load(inputStream);
                annProperties = properties;
            } catch (IOException e) {
                System.err.println("Failed to load properties file: " + PROPERTIES_FILE);
                e.printStackTrace();
                return defaultValue;
            }
		}

		// Return the value from the properties file, or the default value
		return annProperties.getProperty(key, defaultValue);
	}
	public static String readPropString(String key) {
		return readPropString(key, null);
	}


	public SalinitySurrogateSetup(){}

	public static SalinitySurrogateManager getManager(){
		ssm = SalinitySurrogateManager.INSTANCE;
		// Initialization should only be done once
		if (isInitialized) return ssm;


		// Set up call logging, which is done here in the run rather than in the library
		// because we are using CalSIM's active logging directory
		String loggingSurrogateKey = "loggingSurrogate"; //default is false
		boolean logSurrogate = ConfigUtils.readBoolean(ConfigUtils.configMap, loggingSurrogateKey, false);
		if (logSurrogate){
			//Log output path in File format for WRIMS 2 can be obtained by the following method:
			File ilpDir=ILP.getIlpDir();
			String logPathDir = ilpDir.getAbsolutePath() + File.separatorChar+"surrogate.log";
			ssm.enableLogging(logPathDir);
		}

		String multiTaskANNLoc = readPropString(CONFIG_KEY_EC);
		if( multiTaskANNLoc == null){
			System.out.println("The location of the ANN was not found in the properties. " +
					"The properties file " + PROPERTIES_FILE + " needs a line like: " +
			CONFIG_KEY_EC + "= /ann/schism_base.suisun_gru2_tf");
		}
		System.out.println("Loading ANN from "+multiTaskANNLoc);
        Surrogate deltaANN = loadSurrogate(multiTaskANNLoc);
        System.out.println("Loaded ANN from "+multiTaskANNLoc);

		//set up an ANN surrogate month for emmaton
		DisaggregateMonths spline = new DisaggregateMonthsSpline(5);
		DisaggregateMonths repeat = new DisaggregateMonthsRepeat(5);
		DisaggregateMonths daysOps = new DisaggregateMonthsDaysToOps(5, 1., 0.);
		DisaggregateMonths[] disagg = { spline, spline, spline, spline, repeat, repeat, daysOps, repeat };


		List<ExogTimeSeriesAssignment> assignments = new ArrayList<>();
		assignments.add(new ExogTimeSeriesAssignment("calsim/surrogate/sf_tide.csv",
		    "sf_tidal_energy", "serving_default_sf_tidal_energy:0"));
		assignments.add(new ExogTimeSeriesAssignment("calsim/surrogate/sf_tide.csv",
		    "sf_tidal_filter", "serving_default_sf_tidal_filter:0"));
		for (AggregateMonths agg : AggregateMonths.values()) {
			// Create a new surrogate for the current aggregator
			SurrogateMonth annMonthDelta = new SurrogateMonth(disagg, deltaANN, agg, assignments);
			int aggCode = agg.calsimCode;
			// For each location, assign this surrogate if not already present.
			for (Integer location : outputIndexMap.keySet()){
				// Ignore X2
				if (location == SalinitySurrogateManager.X2_CALSIM){ continue; }
				int outIndex = outputIndexMap.get(location);
				if (!ssm.hasSurrogateForSite(location, aggCode)) {
					ssm.setSurrogateForSite(location, aggCode, annMonthDelta);
					ssm.setIndexForSite(location, outIndex);   // e.g. 0 if univariate has only one index
				}
			}
		}

		String x2ANNLoc = readPropString(CONFIG_KEY_X2);
		if( x2ANNLoc == null){
			System.out.println("The location of the X2 ANN was not found in the properties. " +
					"The properties file " + PROPERTIES_FILE + " needs a line like: " +
					CONFIG_KEY_X2 + "= /ann/x2_schism_base.suisun_gru2_tf");
		}

		System.out.println("Loading X2 ANN from "+x2ANNLoc);
        Surrogate x2ANN = loadX2Surrogate(x2ANNLoc);
        System.out.println("Loaded X2 ANN from "+x2ANNLoc);
		List<ExogTimeSeriesAssignment> assignmentsX2 = new ArrayList<>();
		assignmentsX2.add(new ExogTimeSeriesAssignment("calsim/surrogate/sf_tide.csv",
		    "sf_tidal_energy", "serving_default_sf_tidal_energy:0"));
		assignmentsX2.add(new ExogTimeSeriesAssignment("calsim/surrogate/sf_tide.csv",
		    "sf_tidal_filter", "serving_default_sf_tidal_filter:0"));


		//set up an ANN surrogate month for emmaton
        System.out.println("Registering X2 " + SalinitySurrogateManager.X2_CALSIM);

		DisaggregateMonths[] disaggX2 = { spline, spline, spline, repeat };
		for (AggregateMonths agg : AggregateMonths.values()) {
			// Create a new surrogate for the current aggregator
			SurrogateMonth x2MonthDelta = new SurrogateMonth(disaggX2, x2ANN, agg, assignmentsX2);
			int aggCode = agg.calsimCode;

			// Above we skipped X2. Do it now using specialty ANN
			int location = outputIndexMap.get(SalinitySurrogateManager.X2_CALSIM);
			System.out.println("Registering aggCode "+aggCode+" for site "+location);
    		int outIndex = 0;
                // This is an override of what was done above for X2
   			ssm.setSurrogateForSite(location, aggCode, x2MonthDelta);
    		ssm.setIndexForSite(location, outIndex);   // e.g. 0 if univariate has only one index
		}

		isInitialized = true;
		return ssm;
	}

	private static String readConfigMapString(String key, String defaultValue){
	// ConfigUtils apparently doesn't have a readString method but ConfigUtils.configMap is public
	    String retval = defaultValue;
	    if(ConfigUtils.configMap != null){
	    	retval = ConfigUtils.configMap.getOrDefault(key, defaultValue);
	    }
        return retval;
	}

	// Every ANN that changes inputs or their names must be altered here. You need to enter these in the
	// order they are given in training. The utility saved_model_cli will give you the names but
	// it permutes the order
	public static Surrogate loadSurrogate(String dir) {
		String fname = ExternalFunction.externalDir+dir;
		String[] tensorNames = { "serving_default_northern_flow:0",
				"serving_default_exports:0",
				"serving_default_sjr_flow:0",
				"serving_default_cu_delta:0",
				"serving_default_sf_tidal_energy:0",
				"serving_default_sf_tidal_filter:0",
				"serving_default_dcc:0",
				"serving_default_smscg:0", };

		String[] tensorNamesInt = new String[0];

		String outName = readPropString(CONFIG_KEY_EC_OUT);
		if( outName == null){
			System.out.println("The ANN output to use from the ANN was not found in the properties." +
			"For BASE The properties file needs a line like: " + CONFIG_KEY_EC_OUT + "= StatefulPartitionedCall:1\n" +
			"For Suisun The properties file needs a line like: " + CONFIG_KEY_EC_OUT + "= StatefulPartitionedCall:2"			);
		}


		// This describes how daily histories are aggregated and presented to the ANN
		// starting from an (oversized) set of daily values. The Blocked implementation
		// does something akin to legacy CalSim (individual values then averages, all reversed)
		// The Default implementation is more of a pass-through that doesn't aggregate or reversed
		// the data. It describes the length of the history in days that goes into one batch
		DailyToSurrogate dayToANN = new DailyToSurrogateDefault(90,false);
		Surrogate wrap = new TensorWrapper(fname,
				tensorNames,
				tensorNamesInt,
				outName,
				dayToANN);
		return wrap;
	}


	public static Surrogate loadX2Surrogate(String dir) {
		String fname = ExternalFunction.externalDir+dir;
		String[] tensorNames = { "serving_default_ndo:0",
				"serving_default_sf_tidal_energy:0",
				"serving_default_sf_tidal_filter:0",
				"serving_default_smscg:0", };

		String[] tensorNamesInt = new String[0];

		String outName = readPropString(CONFIG_KEY_X2_OUT);
		if( outName == null){
			System.out.println("The X2 ANN output to use from the ANN was not found in the properties." +
					"For BASE The properties file needs a line like: " + CONFIG_KEY_X2_OUT + "= StatefulPartitionedCall:1\n" +
					"For Suisun The properties file needs a line like: " + CONFIG_KEY_X2_OUT + "= StatefulPartitionedCall:2"			);
		}

		// This describes how daily histories are aggregated and presented to the ANN
		// starting from an (oversized) set of daily values. The Blocked implementation
		// does something akin to legacy CalSim (individual values then averages, all reversed)
		// The Default implementation is more of a pass-through that doesn't aggregate or reversed
		// the data. It describes the length of the history in days that goes into one batch 
		DailyToSurrogate dayToANN = new DailyToSurrogateDefault(90,false);
		Surrogate wrap = new TensorWrapper(fname, 
				tensorNames, 
				tensorNamesInt, 
				outName, 
				dayToANN);
		return wrap;
	}	


}
/* This is the order of the inputs during training:
      - northern_flow
      - exports
      - sjr_flow
      - cu_flow
      - sf_tidal_energy
      - sf_tidal_filter
      - dcc
      - smscg

  And this is the signature of the ANN as obtained from 
  > saved_model_cli show --dir schism_base.suisun_gru2_tf --tag_set serve --signature_def serving_default
  inputs['cu_flow'] tensor_info:
      dtype: DT_FLOAT
      shape: (-1, 90)
      name: serving_default_cu_flow:0
  inputs['dcc'] tensor_info:
      dtype: DT_FLOAT
      shape: (-1, 90)
      name: serving_default_dcc:0
  inputs['exports'] tensor_info:
      dtype: DT_FLOAT
      shape: (-1, 90)
      name: serving_default_exports:0
  inputs['northern_flow'] tensor_info:
      dtype: DT_FLOAT
      shape: (-1, 90)
      name: serving_default_northern_flow:0
  inputs['sf_tidal_energy'] tensor_info:
      dtype: DT_FLOAT
      shape: (-1, 90)
      name: serving_default_sf_tidal_energy:0
  inputs['sf_tidal_filter'] tensor_info:
      dtype: DT_FLOAT
      shape: (-1, 90)
      name: serving_default_sf_tidal_filter:0
  inputs['sjr_flow'] tensor_info:
      dtype: DT_FLOAT
      shape: (-1, 90)
      name: serving_default_sjr_flow:0
  inputs['smscg'] tensor_info:
      dtype: DT_FLOAT
      shape: (-1, 90)
      name: serving_default_smscg:0
The given SavedModel SignatureDef contains the following output(s):
  outputs['unscale_base'] tensor_info:
      dtype: DT_FLOAT
      shape: (-1, 19)
      name: StatefulPartitionedCall:0
  outputs['unscale_contrast'] tensor_info:
      dtype: DT_FLOAT
      shape: (-1, 19)
      name: StatefulPartitionedCall:1
  outputs['unscale_suisun'] tensor_info:
      dtype: DT_FLOAT
      shape: (-1, 19)
      name: StatefulPartitionedCall:2
Method name is: tensorflow/serving/predict


>saved_model_cli show --dir x2_schism_base.suisun_gru2_tf --tag_set serve --signature_def serving_default
The given SavedModel SignatureDef contains the following input(s):
  inputs['ndo'] tensor_info:
      dtype: DT_FLOAT
      shape: (-1, 90)
      name: serving_default_ndo:0
  inputs['sf_tidal_energy'] tensor_info:
      dtype: DT_FLOAT
      shape: (-1, 90)
      name: serving_default_sf_tidal_energy:0
  inputs['sf_tidal_filter'] tensor_info:
      dtype: DT_FLOAT
      shape: (-1, 90)
      name: serving_default_sf_tidal_filter:0
  inputs['smscg'] tensor_info:
      dtype: DT_FLOAT
      shape: (-1, 90)
      name: serving_default_smscg:0
The given SavedModel SignatureDef contains the following output(s):
  outputs['out_contrast_unscaled'] tensor_info:
      dtype: DT_FLOAT
      shape: (-1, 9)
      name: StatefulPartitionedCall:0
  outputs['out_source_unscaled'] tensor_info:
      dtype: DT_FLOAT
      shape: (-1, 9)
      name: StatefulPartitionedCall:1
  outputs['out_target_unscaled'] tensor_info:
      dtype: DT_FLOAT
      shape: (-1, 9)
      name: StatefulPartitionedCall:2
Method name is: tensorflow/serving/predict


 */




