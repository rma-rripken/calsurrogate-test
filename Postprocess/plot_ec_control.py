import os
import pandas as pd
import matplotlib.pyplot as plt
from pyhecdss import get_ts
import numpy as np

def plot_ec_slack_relative_to_standard(out_dir, out_file, title="EC Slack Relative to Standard"):
    fpath = os.path.join(out_dir, out_file)

    slack_paths = {
        "emm": "//EM_EC_SLACK/////",
        "jer": "//JP_EC_SLACK/////",
        "rsl": "//RS_EC_SLACK/////",
    }
    std_paths = {
        "emm": "//EM_EC_STD/////",
        "jer": "//JP_EC_STD/////",
        "rsl": "//RS_EC_STD/////",
    }
    labels = {
        "emm": "Emmaton",
        "jer": "Jersey Point",
        "rsl": "Rock Slough",
    }

    fig, ax = plt.subplots(figsize=(10, 5))

    for key in ["emm", "jer", "rsl"]:
        slack_ts = list(get_ts(fpath, slack_paths[key]))[0].data.squeeze()
        
        std_ts = list(get_ts(fpath, std_paths[key]))[0].data.squeeze()
        print(std_ts)
        # Mask where standard < 10,000
        valid_mask = std_ts.abs() > 3000
        
        rel_slack = -slack_ts
        rel_slack.loc[valid_mask] = np.nan
        #rel_slack.loc[rel_slack < -3000.] = np.nan
        print(slack_ts)
        print(rel_slack)


        # Convert PeriodIndex → Timestamp (DatetimeIndex)
        rel_slack.index = rel_slack.index.to_timestamp()

        # Plot using step
        ax.step(rel_slack.index, rel_slack.values, where="post", label=labels[key])

    ax.axhline(0, color='k', linewidth=0.8, linestyle='--')
    ax.set_title(title)
    ax.set_ylabel("EC Relative to Standard\n(–SLACK) ($\\mu$S/cm)")
    ax.set_xlabel("Date")
    ax.legend()
    plt.tight_layout()
    plt.show()


if __name__ == "__main__":
    out_dir = "../DSS/output"
    out_file = "DCR2023_DV_9.3.1_Danube_Adj_v1.8.dss"
    plot_ec_slack_relative_to_standard(out_dir, out_file)
