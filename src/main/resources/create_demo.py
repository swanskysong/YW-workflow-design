
import netCDF4
import numpy as np
from netCDF4 import ma
import matplotlib.pyplot as plt
from matplotlib.backends.backend_pdf import PdfPages

    # @CREATE standardize_with_mask
    # @IN NEE_data
    # @IN land_water_mask
    # @OUT standardized_NEE_data

    # @END standardize_with_mask
Code pieces a
Code pieces b

Code pieces c
# @CREATE simple_diagnose
# @PARAM fmodel
# @IN standardized_NEE_data
# @OUT result_NEE_pdf  @URI file:result_NEE.pdf
#
# @END simple_diagnose

Code pieces d
# @CREATE load_data
# @PARAM db_pth
# @IN not_vaid
# @OUT not_GOOD

# @END load_data