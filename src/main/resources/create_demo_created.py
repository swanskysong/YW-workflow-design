
import netCDF4
import numpy as np
from netCDF4 import ma
import matplotlib.pyplot as plt
from matplotlib.backends.backend_pdf import PdfPages

    # @BEGIN standardize_with_mask
    # @IN data @AS NEE_data
    # @IN mask @AS land_water_mask
    # @OUT data @AS standardized_NEE_data
    native = data.mean(2)
    latShape = mask.shape[0]
    logShape = mask.shape[1]
    for x in range(latShape):
        for y in range(logShape):
            if mask[x,y] == 1 and ma.getmask(native[x,y]) == 1:
                for index in range(data.shape[2]):
                    data[x,y,index] = 0
    # @END standardize_with_mask
Code pieces a
Code pieces b

Code pieces c
    # @BEGIN simple_diagnose
    # @PARAM fmodel
    # @IN data @AS standardized_NEE_data
    # @OUT pp  @AS result_NEE_pdf  @URI file:result_NEE.pdf
    plt.imshow(np.mean(data,2))
    plt.xlabel("Mean 1982-2010 NEE [gC/m2/mon]")
    plt.title(fmodel + ":BG1")
    pp = PdfPages('result_NEE.pdf')
    pp.savefig()
    pp.close()    
    # @END simple_diagnose

Code pieces d
# @CREATE load_data
# @PARAM db_pth
# @IN not_vaid
# @OUT not_GOOD

# @END load_data
