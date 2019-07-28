/* Copyright 2012 Yaqiang Wang,
 * yaqiang.wang@gmail.com
 * 
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 */
package org.meteothink.data.meteodata;

import java.io.File;
import org.meteothink.data.meteodata.arl.ARLDataInfo;
import org.meteothink.data.meteodata.ascii.ASCIIGridDataInfo;
import org.meteothink.data.meteodata.ascii.LonLatStationDataInfo;
import org.meteothink.data.meteodata.ascii.SurferGridDataInfo;
import org.meteothink.data.meteodata.grads.GrADSDataInfo;
import org.meteothink.data.meteodata.hysplit.HYSPLITConcDataInfo;
import org.meteothink.data.meteodata.hysplit.HYSPLITPartDataInfo;
import org.meteothink.data.meteodata.hysplit.HYSPLITTrajDataInfo;
import org.meteothink.data.meteodata.micaps.MICAPS1DataInfo;
import org.meteothink.data.meteodata.micaps.MICAPS3DataInfo;
import org.meteothink.data.meteodata.micaps.MICAPS4DataInfo;
import org.meteothink.data.meteodata.micaps.MICAPSDataInfo;
import org.meteothink.data.meteodata.netcdf.NetCDFDataInfo;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.meteothink.math.ArrayMath;
import org.meteothink.data.meteodata.micaps.MICAPS11DataInfo;
import org.meteothink.data.meteodata.micaps.MICAPS120DataInfo;
import org.meteothink.data.meteodata.micaps.MICAPS13DataInfo;
import org.meteothink.data.meteodata.micaps.MICAPS7DataInfo;
import org.meteothink.data.meteodata.mm5.MM5DataInfo;
import org.meteothink.data.meteodata.mm5.MM5IMDataInfo;
import org.meteothink.data.meteodata.awx.AWXDataInfo;
import org.meteothink.data.meteodata.bandraster.BILDataInfo;
import org.meteothink.data.meteodata.bandraster.GeoTiffDataInfo;
import org.meteothink.data.meteodata.metar.METARDataInfo;
import org.meteothink.data.meteodata.micaps.MICAPS131DataInfo;
import org.meteothink.data.meteodata.micaps.MICAPS2DataInfo;
import org.meteothink.data.meteodata.synop.SYNOPDataInfo;
import org.meteothink.ndarray.Array;
import org.meteothink.ndarray.DimArray;
import org.meteothink.ndarray.Dimension;
import org.meteothink.ndarray.DimensionType;
import org.meteothink.ndarray.InvalidRangeException;
import org.meteothink.ndarray.Range;
import ucar.nc2.NetcdfFile;

/**
 *
 * @author Yaqiang Wang
 */
public class MeteoDataInfo {
    // <editor-fold desc="Variables">

    private PlotDimension _dimensionSet = PlotDimension.Lat_Lon;
    private int _varIdx;
    private int _timeIdx;
    private int _levelIdx;
    private int _latIdx;
    private int _lonIdx;
    /// <summary>
    /// Is Lont/Lat
    /// </summary>
    public boolean IsLonLat;
    /// <summary>
    /// If the U/V of the wind are along latitude/longitude.
    /// </summary>
    public boolean EarthWind;
    private DataInfo _dataInfo;
    /// <summary>
    /// Data information text
    /// </summary>
    private String _infoText;
    /// <summary>
    /// Wind U/V variable name
    /// </summary>
    private MeteoUVSet _meteoUVSet;
    /// <summary>
    /// If X reserved
    /// </summary>
    public boolean xReserve;
    /// <summary>
    /// If Y reserved
    /// </summary>
    public boolean yReserve;
    // </editor-fold>
    // <editor-fold desc="Constructor">

    /**
     * Constructor
     */
    public MeteoDataInfo() {
        _dataInfo = null;
        IsLonLat = true;
        EarthWind = true;
        _infoText = "";
        _meteoUVSet = new MeteoUVSet();
        xReserve = false;
        yReserve = false;
    }
    // </editor-fold>
    // <editor-fold desc="Get Set Methods">

    /**
     * Get data info
     *
     * @return Data info
     */
    public DataInfo getDataInfo() {
        return _dataInfo;
    }

    /**
     * Set data info
     *
     * @param value Data info
     */
    public void setDataInfo(DataInfo value) {
        _dataInfo = value;
        _infoText = _dataInfo.generateInfoText();
    }

    /**
     * Get Coordinate Reference System
     *
     * @return CoordinateReferenceSystem
     */
    public CoordinateReferenceSystem getCRS() {
        return _dataInfo.getCRS();
    }

    /**
     * Get meteo data type
     *
     * @return Meteo data type
     */
    public MeteoDataType getDataType() {
        return this._dataInfo.getDataType();
    }

    /**
     * Get plot dimension
     *
     * @return Plot dimension
     */
    public PlotDimension getDimensionSet() {
        return _dimensionSet;
    }

    /**
     * Set plot dimension
     *
     * @param value Plot dimension
     */
    public void setDimensionSet(PlotDimension value) {
        _dimensionSet = value;
    }

    /**
     * Get data info text
     *
     * @return Data info text
     */
    public String getInfoText() {
        return _infoText;
    }

    /**
     * Get time index
     *
     * @return Time index
     */
    public int getTimeIndex() {
        return _timeIdx;
    }

    /**
     * Set time index
     *
     * @param value Time index
     */
    public void setTimeIndex(int value) {
        _timeIdx = value;
    }

    /**
     * Get level index
     *
     * @return Level index
     */
    public int getLevelIndex() {
        return _levelIdx;
    }

    /**
     * Set level index
     *
     * @param value Level index
     */
    public void setLevelIndex(int value) {
        _levelIdx = value;
    }

    /**
     * Get variable index
     *
     * @return Variable index
     */
    public int getVariableIndex() {
        return _varIdx;
    }

    /**
     * Set variable index
     *
     * @param value Variable index
     */
    public void setVariableIndex(int value) {
        _varIdx = value;
    }

    /**
     * Get longitude index
     *
     * @return Longitude index
     */
    public int getLonIndex() {
        return _lonIdx;
    }

    /**
     * Set longitude index
     *
     * @param value Longitude index
     */
    public void setLonIndex(int value) {
        _lonIdx = value;
    }

    /**
     * Get latitude index
     *
     * @return Latitude index
     */
    public int getLatIndex() {
        return _latIdx;
    }

    /**
     * Set latitude index
     *
     * @param value Latitude index
     */
    public void setLatIndex(int value) {
        _latIdx = value;
    }

    /**
     * Get Meteo U/V setting
     *
     * @return Meteo U/V setting
     */
    public MeteoUVSet getMeteoUVSet() {
        return _meteoUVSet;
    }

    /**
     * Set Meteo U/V Setting
     *
     * @param value Meteo U/V setting
     */
    public void setMeteoUVSet(MeteoUVSet value) {
        _meteoUVSet = value;
    }

    /**
     * Get missing value
     *
     * @return Missing value
     */
    public double getMissingValue() {
        return _dataInfo.getMissingValue();
    }

    /**
     * Get if is grid data
     *
     * @return Boolean
     */
    public boolean isGridData() {

        switch (this.getDataType()) {
            case ARL_Grid:
            case ASCII_Grid:
            case GrADS_Grid:
            case GRIB1:
            case GRIB2:
            case HYSPLIT_Conc:
            case MICAPS_11:
            case MICAPS_13:
            case MICAPS_4:
            case MICAPS_131:
            case Sufer_Grid:
            case MM5:
            case MM5IM:
                return true;
            case NetCDF:
                if (((NetCDFDataInfo) _dataInfo).isSWATH()) {
                    return false;
                } else {
                    return true;
                }
            case GEOTIFF:
                return true;
            case AWX:
                switch (((AWXDataInfo) this.getDataInfo()).getProductType()) {
                    case 1:
                    case 2:
                    case 3:
                        return true;
                    default:
                        return false;
                }
            default:
                return false;
        }
    }

    /**
     * Get if is station data
     *
     * @return Boolean
     */
    public boolean isStationData() {
        switch (this.getDataType()) {
            case GrADS_Station:
            case ISH:
            case METAR:
            case MICAPS_1:
            case MICAPS_2:
            case MICAPS_3:
            case MICAPS_120:
            case LonLatStation:
            case SYNOP:
            case HYSPLIT_Particle:
                return true;
            case AWX:
                if (((AWXDataInfo) this.getDataInfo()).getProductType() == 4) {
                    return true;
                } else {
                    return false;
                }
            default:
                return false;
        }
    }

    /**
     * Get if is trajectory data
     *
     * @return Boolean
     */
    public boolean isTrajData() {
        switch (this.getDataType()) {
            case HYSPLIT_Traj:
            case MICAPS_7:
                return true;
            default:
                return false;
        }
    }

    /**
     * Get if is SWATH data
     *
     * @return Boolean
     */
    public boolean isSWATHData() {
        switch (this.getDataType()) {
            case NetCDF:
                if (((NetCDFDataInfo) _dataInfo).isSWATH()) {
                    return true;
                }
            default:
                return false;
        }
    }

    /**
     * Get variable dimension number
     *
     * @return Variable dimension number
     */
    public int getDimensionNumber() {
        int dn = 2;
        switch (_dimensionSet) {
            case Lat_Lon:
            case Level_Lat:
            case Level_Lon:
            case Level_Time:
            case Time_Lat:
            case Time_Lon:
                dn = 2;
                break;
            case Level:
            case Lon:
            case Time:
            case Lat:
                dn = 1;
                break;
        }

        return dn;
    }

    // </editor-fold>
    // <editor-fold desc="Methods">
    // <editor-fold desc="Open Data">
    /**
     * Open data file
     *
     * @param fileName File name
     */
    public void openData(String fileName) {
        try {
            boolean canOpen = NetcdfFile.canOpen(fileName);
            if (canOpen) {
                this.openNetCDFData(fileName);
            } else if (ARLDataInfo.canOpen(fileName)) {
                this.openARLData(fileName);
            }
        } catch (IOException ex) {
            Logger.getLogger(MeteoDataInfo.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Open data file
     *
     * @param fileName File name
     * @param keepOpen Keep the file opened or not
     */
    public void openData(String fileName, boolean keepOpen) {
        try {
            boolean canOpen = NetcdfFile.canOpen(fileName);
            if (canOpen) {
                this.openNetCDFData(fileName, keepOpen);
            } else if (ARLDataInfo.canOpen(fileName)) {
                this.openARLData(fileName);
            }
        } catch (IOException ex) {
            Logger.getLogger(MeteoDataInfo.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Open data file
     *
     * @param ncfile Netcdf file
     * @param keepOpen Keep the file opened or not
     */
    public void openData(NetcdfFile ncfile, boolean keepOpen) {
        this.openNetCDFData(ncfile, keepOpen);
    }

    /**
     * Close opened file
     */
    public void close() {
        if (this._dataInfo.getDataType() == MeteoDataType.NetCDF) {
            NetCDFDataInfo dinfo = (NetCDFDataInfo) this._dataInfo;
            if (dinfo.getFile() != null) {
                try {
                    dinfo.getFile().close();
                } catch (IOException ex) {
                    Logger.getLogger(MeteoDataInfo.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    /**
     * Open GrADS data
     *
     * @param aFile Data file path
     */
    public void openGrADSData(String aFile) {
        _dataInfo = new GrADSDataInfo();
        _dataInfo.readDataInfo(aFile);
        _infoText = _dataInfo.generateInfoText();
        GrADSDataInfo aDataInfo = (GrADSDataInfo) _dataInfo;
        if (aDataInfo.DTYPE.equals("Gridded")) {
            yReserve = aDataInfo.OPTIONS.yrev;

            if (!aDataInfo.isLatLon) {
                IsLonLat = false;
                EarthWind = aDataInfo.EarthWind;
            }
        }
    }

    /**
     * Open ARL packed meteorological data
     *
     * @param aFile File path
     */
    public void openARLData(String aFile) {
        ARLDataInfo aDataInfo = new ARLDataInfo();
        aDataInfo.readDataInfo(aFile);
        _dataInfo = aDataInfo;
        IsLonLat = aDataInfo.isLatLon;

        //Get data info text
        _infoText = aDataInfo.generateInfoText();
    }

    /**
     * Open AWX data
     *
     * @param aFile File path
     */
    public void openAWXData(String aFile) {
        AWXDataInfo aDataInfo = new AWXDataInfo();
        aDataInfo.readDataInfo(aFile);
        _dataInfo = aDataInfo;
        //IsLonLat = aDataInfo.isLatLon;

        //Get data info text
        _infoText = aDataInfo.generateInfoText();
        if (aDataInfo.getProductType() == 4) {
            _meteoUVSet.setUV(false);
            _meteoUVSet.setFixUVStr(true);
            _meteoUVSet.setUStr("WindDirection");
            _meteoUVSet.setVStr("WindSpeed");
        }
    }

    /**
     * Open SYNOP data
     *
     * @param aFile File path
     * @param stFile Station file name
     */
    public void openSYNOPData(String aFile, String stFile) {
        SYNOPDataInfo aDataInfo = new SYNOPDataInfo();
        aDataInfo.setStationFileName(stFile);
        aDataInfo.readDataInfo(aFile);
        _dataInfo = aDataInfo;
        //IsLonLat = aDataInfo.isLatLon;

        //Get data info text
        _infoText = aDataInfo.generateInfoText();
        _meteoUVSet.setUV(false);
        _meteoUVSet.setFixUVStr(true);
        _meteoUVSet.setUStr("WindDirection");
        _meteoUVSet.setVStr("WindSpeed");
    }

    /**
     * Open SYNOP data
     *
     * @param aFile File path
     * @param stFile Station file name
     */
    public void openMETARData(String aFile, String stFile) {
        METARDataInfo aDataInfo = new METARDataInfo();
        aDataInfo.setStationFileName(stFile);
        aDataInfo.readDataInfo(aFile);
        _dataInfo = aDataInfo;
        //IsLonLat = aDataInfo.isLatLon;

        //Get data info text
        _infoText = aDataInfo.generateInfoText();
        _meteoUVSet.setUV(false);
        _meteoUVSet.setFixUVStr(true);
        _meteoUVSet.setUStr("WindDirection");
        _meteoUVSet.setVStr("WindSpeed");
    }

    /**
     * Open ASCII grid data
     *
     * @param aFile File path
     */
    public void openASCIIGridData(String aFile) {
        ASCIIGridDataInfo aDataInfo = new ASCIIGridDataInfo();
        aDataInfo.readDataInfo(aFile);
        _dataInfo = aDataInfo;
        //ProjInfo = aDataInfo.projInfo;
        //IsLonLat = aDataInfo.isLatLon;

        //Get data info text
        _infoText = aDataInfo.generateInfoText();
    }

    /**
     * Open Geotiff grid data
     *
     * @param aFile File path
     */
    public void openGeoTiffData(String aFile) {
        GeoTiffDataInfo aDataInfo = new GeoTiffDataInfo();
        aDataInfo.readDataInfo(aFile);
        _dataInfo = aDataInfo;

        //Get data info text
        _infoText = aDataInfo.generateInfoText();
    }

    /**
     * Open BIL grid data
     *
     * @param aFile File path
     */
    public void openBILData(String aFile) {
        BILDataInfo aDataInfo = new BILDataInfo();
        aDataInfo.readDataInfo(aFile);
        _dataInfo = aDataInfo;

        //Get data info text
        _infoText = aDataInfo.generateInfoText();
    }

    /**
     * Open HYSPLIT concentration grid data
     *
     * @param aFile File path
     */
    public void openHYSPLITConcData(String aFile) {
        HYSPLITConcDataInfo aDataInfo = new HYSPLITConcDataInfo();
        aDataInfo.readDataInfo(aFile);
        _dataInfo = aDataInfo;
        //ProjInfo = aDataInfo.projInfo;
        //IsLonLat = aDataInfo.isLatLon;

        //Get data info text
        _infoText = aDataInfo.generateInfoText();
    }

    /**
     * Open HYSPLIT concentration grid data
     *
     * @param aFile File path
     * @param bigendian Big endian or not
     */
    public void openHYSPLITConcData(String aFile, boolean bigendian) {
        HYSPLITConcDataInfo aDataInfo = new HYSPLITConcDataInfo(bigendian);
        aDataInfo.readDataInfo(aFile);
        _dataInfo = aDataInfo;
        //ProjInfo = aDataInfo.projInfo;
        //IsLonLat = aDataInfo.isLatLon;

        //Get data info text
        _infoText = aDataInfo.generateInfoText();
    }

    /**
     * Open HYSPLIT trajectory data
     *
     * @param aFile File path
     */
    public void openHYSPLITTrajData(String aFile) {
        //Read data info                            
        HYSPLITTrajDataInfo aDataInfo = new HYSPLITTrajDataInfo();
        aDataInfo.readDataInfo(aFile);
        _dataInfo = aDataInfo;
        _infoText = aDataInfo.generateInfoText();
    }

//    /**
//     * Open HYSPLIT traject data
//     *
//     * @param trajFiles File paths
//     */
//    public void openHYSPLITTrajData(String[] trajFiles) {
//        try {
//            //Read data info                            
//            HYSPLITTrajDataInfo aDataInfo = new HYSPLITTrajDataInfo();
//            aDataInfo.readDataInfo(trajFiles);
//            _dataInfo = aDataInfo;
//            _infoText = aDataInfo.generateInfoText();
//        } catch (IOException ex) {
//            Logger.getLogger(MeteoDataInfo.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }
//
//    /**
//     * Open HYSPLIT traject data
//     *
//     * @param trajFiles File paths
//     */
//    public void openHYSPLITTrajData(List<String> trajFiles) {
//        String[] files = trajFiles.toArray(new String[0]);
//        openHYSPLITTrajData(files);
//    }
    /**
     * Open HYSPLIT particle data
     *
     * @param fileName File path
     */
    public void openHYSPLITPartData(String fileName) {
        //Read data info                            
        HYSPLITPartDataInfo aDataInfo = new HYSPLITPartDataInfo();
        aDataInfo.readDataInfo(fileName);
        _dataInfo = aDataInfo;
        _infoText = aDataInfo.generateInfoText();
    }

    /**
     * Open NetCDF data
     *
     * @param fileName File path
     */
    public void openNetCDFData(String fileName) {
        NetCDFDataInfo aDataInfo = new NetCDFDataInfo();
        aDataInfo.readDataInfo(fileName);
        _dataInfo = aDataInfo;
        _infoText = aDataInfo.generateInfoText();
    }

    /**
     * Open NetCDF data
     *
     * @param fileName File path
     * @param keepOpen Keep file opened or not
     */
    public void openNetCDFData(String fileName, boolean keepOpen) {
        NetCDFDataInfo aDataInfo = new NetCDFDataInfo();
        aDataInfo.readDataInfo(fileName, keepOpen);
        _dataInfo = aDataInfo;
        _infoText = aDataInfo.generateInfoText();
    }

    /**
     * Open NetCDF data
     *
     * @param ncfile Netcdf file
     * @param keepOpen Keep file opened or not
     */
    public void openNetCDFData(NetcdfFile ncfile, boolean keepOpen) {
        NetCDFDataInfo aDataInfo = new NetCDFDataInfo();
        aDataInfo.readDataInfo(ncfile, keepOpen);
        _dataInfo = aDataInfo;
        _infoText = aDataInfo.generateInfoText();
    }

    /**
     * Open GRIB data by predifined version - for mixed GRIB-1 and GRIB-2 data
     * file.
     *
     * @param fileName File path
     * @param version GRIB data version: 1 or 2.
     */
    public void openGRIBData(String fileName, int version) {
        NetCDFDataInfo aDataInfo = new NetCDFDataInfo();
        MeteoDataType mdt = MeteoDataType.GRIB2;
        if (version == 1) {
            mdt = MeteoDataType.GRIB1;
        }
        aDataInfo.readDataInfo(fileName, mdt);
        _dataInfo = aDataInfo;
        _infoText = aDataInfo.generateInfoText();
    }

    /**
     * Open Lon/Lat station data
     *
     * @param fileName File path
     */
    public void openLonLatData(String fileName) {
        _dataInfo = new LonLatStationDataInfo();
        _dataInfo.readDataInfo(fileName);
        _infoText = _dataInfo.generateInfoText();
    }

    /**
     * Open Surfer ASCII grid data
     *
     * @param fileName File path
     */
    public void openSurferGridData(String fileName) {
        _dataInfo = new SurferGridDataInfo();
        _dataInfo.readDataInfo(fileName);
        _infoText = _dataInfo.generateInfoText();
    }

    /**
     * Open MM5 Output data
     *
     * @param fileName File path
     */
    public void openMM5Data(String fileName) {
        _dataInfo = new MM5DataInfo();
        _dataInfo.readDataInfo(fileName);
        _infoText = _dataInfo.generateInfoText();
    }

    /**
     * Open MM5 Output data
     *
     * @param fileName The MM5 output data file without big head
     * @param bigHeadFile The MM5 output data file with big head
     */
    public void openMM5Data(String fileName, String bigHeadFile) {
        _dataInfo = new MM5DataInfo();
        ((MM5DataInfo) _dataInfo).readDataInfo(fileName, bigHeadFile);
        _infoText = _dataInfo.generateInfoText();
    }

    /**
     * Open MM5 Intermediate data
     *
     * @param fileName File path
     */
    public void openMM5IMData(String fileName) {
        _dataInfo = new MM5IMDataInfo();
        _dataInfo.readDataInfo(fileName);
        _infoText = _dataInfo.generateInfoText();
    }

    /**
     * Open MICAPS data
     *
     * @param fileName File name
     */
    public void openMICAPSData(String fileName) {
        MeteoDataType mdType = MICAPSDataInfo.getDataType(fileName);
        if (mdType == null) {
            return;
        }

        switch (mdType) {
            case MICAPS_1:
                _dataInfo = new MICAPS1DataInfo();
                _meteoUVSet.setUV(false);
                _meteoUVSet.setFixUVStr(true);
                _meteoUVSet.setUStr("WindDirection");
                _meteoUVSet.setVStr("WindSpeed");
                break;
            case MICAPS_2:
                _dataInfo = new MICAPS2DataInfo();
                _meteoUVSet.setUV(false);
                _meteoUVSet.setFixUVStr(true);
                _meteoUVSet.setUStr("WindDirection");
                _meteoUVSet.setVStr("WindSpeed");
                break;
            case MICAPS_3:
                _dataInfo = new MICAPS3DataInfo();
                _meteoUVSet.setUV(false);
                _meteoUVSet.setFixUVStr(true);
                _meteoUVSet.setUStr("WindDirection");
                _meteoUVSet.setVStr("WindSpeed");
                break;
            case MICAPS_4:
                _dataInfo = new MICAPS4DataInfo();
                break;
            case MICAPS_7:
                _dataInfo = new MICAPS7DataInfo();
                break;
            case MICAPS_11:
                _dataInfo = new MICAPS11DataInfo();
                break;
            case MICAPS_13:
                _dataInfo = new MICAPS13DataInfo();
                break;
            case MICAPS_120:
                _dataInfo = new MICAPS120DataInfo();
                break;
            case MICAPS_131:
                _dataInfo = new MICAPS131DataInfo();
                break;
        }
        _dataInfo.readDataInfo(fileName);
        _infoText = _dataInfo.generateInfoText();
    }
    // </editor-fold>

    // <editor-fold desc="Get Data">
    /**
     * Get file name
     *
     * @return File name
     */
    public String getFileName() {
        return _dataInfo.getFileName();
    }

    /**
     * Read array data of the variable
     *
     * @param varName Variable name
     * @return Array data
     */
    public DimArray read(String varName) {
        return this._dataInfo.read(varName);
    }

    /**
     * Read array data of the variable
     *
     * @param varName Variable name
     * @param origin The origin array
     * @param size The size array
     * @param stride The stride array
     * @return Array data
     */
    public DimArray read(String varName, int[] origin, int[] size, int[] stride) {
        return this._dataInfo.read(varName, origin, size, stride);
    }

    /**
     * Read array data from a variable
     *
     * @param varName Variable name
     * @param ranges List of dimension ranges
     * @return Array data
     */
    public DimArray read(String varName, List<Range> ranges) {
        int n = ranges.size();
        int[] origin = new int[n];
        int[] size = new int[n];
        int[] stride = new int[n];
        for (int i = 0; i < n; i++) {
            origin[i] = ranges.get(i).first();
            size[i] = ranges.get(i).last() - ranges.get(i).first() + 1;
            stride[i] = ranges.get(i).stride();
        }

        return read(varName, origin, size, stride);
    }

    /**
     * Read array data of the variable
     *
     * @param varName Variable name
     * @param origin The origin array
     * @param size The size array
     * @param stride The stride array
     * @return Array data
     */
    public DimArray read(String varName, List<Integer> origin, List<Integer> size, List<Integer> stride) {
        int n = origin.size();
        int[] origin_a = new int[n];
        int[] size_a = new int[n];
        int[] stride_a = new int[n];
        for (int i = 0; i < n; i++) {
            origin_a[i] = origin.get(i);
            size_a[i] = size.get(i);
        }
        if (stride == null) {
            for (int i = 0; i < n; i++) {
                stride_a[i] = 1;
            }
        } else {
            for (int i = 0; i < n; i++) {
                stride_a[i] = stride.get(i);
            }
        }

        return this._dataInfo.read(varName, origin_a, size_a, stride_a);
    }

    /**
     * Read array data of the variable
     *
     * @param varName Variable name
     * @param origin The origin array
     * @param size The size array
     * @return Array data
     */
    public DimArray read(String varName, List<Integer> origin, List<Integer> size) {
        return this.read(varName, origin, size, null);
    }

    /**
     * Take array data from the variable
     *
     * @param varName Variable name
     * @param ranges Range list
     * @return Array data
     * @throws InvalidRangeException
     */
    public DimArray take(String varName, List<Object> ranges) throws InvalidRangeException {
        Variable var = this.getDataInfo().getVariable(varName);
        List<Dimension> dims = var.getDimensions();
        List<Dimension> ndims = new ArrayList<>();
        int n = ranges.size();
        List<Range> nranges = new ArrayList<>();
        List<Object> branges = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (ranges.get(i) instanceof Range) {
                nranges.add((Range) ranges.get(i));
                branges.add(new Range(0, ((Range) ranges.get(i)).length() - 1, 1));
                ndims.add(dims.get(i).extract((Range) ranges.get(i)));
            } else {
                List<Integer> list = (List<Integer>) ranges.get(i);
                int min = list.get(0);
                int max = min;
                if (list.size() > 1) {
                    for (int j = 1; j < list.size(); j++) {
                        if (min > list.get(j)) {
                            min = list.get(j);
                        }
                        if (max < list.get(j)) {
                            max = list.get(j);
                        }
                    }
                }
                Range range = new Range(min, max, 1);
                nranges.add(range);
                List<Integer> nlist = new ArrayList<>();
                for (int j = 0; j < list.size(); j++) {
                    nlist.add(list.get(j) - min);
                }
                branges.add(nlist);
                ndims.add(dims.get(i).extract(nlist));
            }
        }

        DimArray r = read(varName, nranges);
        Array a = ArrayMath.take(r.getArray(), branges);

        return new DimArray(a, ndims);
    }

    /**
     * Get grid data
     *
     * @param varName Variable name
     * @return Grid data
     */
    public DimArray getGridData(String varName) {
        _varIdx = getVariableIndex(varName);
        if (_varIdx < 0) {
//            MathParser mathParser = new MathParser(this);
//            try {
//                GridData gridData = (GridData) mathParser.evaluate(varName);
//                gridData.projInfo = this.getProjectionInfo();
//                return gridData;
//            } catch (ParseException | IOException ex) {
//                Logger.getLogger(MeteoDataInfo.class.getName()).log(Level.SEVERE, null, ex);
//                return null;
//            }
            return null;
        } else {
            try {
                DimArray gridData = this.getGridData();
                return gridData;
            } catch (InvalidRangeException ex) {
                Logger.getLogger(MeteoDataInfo.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        }
    }

    /**
     * Get grid data
     *
     * @return Grid data
     * @throws org.meteothink.ndarray.InvalidRangeException
     */
    public DimArray getGridData() throws InvalidRangeException {
        if (_varIdx < 0) {
            return null;
        }

        Variable var = this.getDataInfo().getVariables().get(_varIdx);
        List<Dimension> dims = var.getDimensions();
        List<Range> ranges = new ArrayList<>();
        switch (_dimensionSet) {
            case Lat_Lon:
                for (Dimension dim : dims) {
                    switch (dim.getDimType()) {
                        case T:
                            ranges.add(new Range(_timeIdx, _timeIdx, 1));
                            break;
                        case Z:
                            ranges.add(new Range(_levelIdx, _levelIdx, 1));
                            break;
                        case Y:
                            ranges.add(new Range(0, dim.getLength() - 1, 1));
                            break;
                        case X:
                            ranges.add(new Range(0, dim.getLength() - 1, 1));
                            break;
                        default:
                            ranges.add(new Range(0, 0, 1));
                    }
                }
                break;
            case Time_Lon:
                for (Dimension dim : dims) {
                    switch (dim.getDimType()) {
                        case T:
                            ranges.add(new Range(0, dim.getLength() - 1, 1));
                            break;
                        case Z:
                            ranges.add(new Range(_levelIdx, _levelIdx, 1));
                            break;
                        case Y:
                            ranges.add(new Range(_latIdx, _latIdx, 1));
                            break;
                        case X:
                            ranges.add(new Range(0, dim.getLength() - 1, 1));
                            break;
                        default:
                            ranges.add(new Range(0, 0, 1));
                    }
                }
                break;
            case Time_Lat:
                for (Dimension dim : dims) {
                    switch (dim.getDimType()) {
                        case T:
                            ranges.add(new Range(0, dim.getLength() - 1, 1));
                            break;
                        case Z:
                            ranges.add(new Range(_levelIdx, _levelIdx, 1));
                            break;
                        case Y:
                            ranges.add(new Range(0, dim.getLength() - 1, 1));
                            break;
                        case X:
                            ranges.add(new Range(_lonIdx, _lonIdx, 1));
                            break;
                        default:
                            ranges.add(new Range(0, 0, 1));
                    }
                }
                break;
            case Level_Lon:
                for (Dimension dim : dims) {
                    switch (dim.getDimType()) {
                        case T:
                            ranges.add(new Range(_timeIdx, _timeIdx, 1));
                            break;
                        case Z:
                            ranges.add(new Range(0, dim.getLength() - 1, 1));
                            break;
                        case Y:
                            ranges.add(new Range(_latIdx, _latIdx, 1));
                            break;
                        case X:
                            ranges.add(new Range(0, dim.getLength() - 1, 1));
                            break;
                        default:
                            ranges.add(new Range(0, 0, 1));
                    }
                }
                break;
            case Level_Lat:
                for (Dimension dim : dims) {
                    switch (dim.getDimType()) {
                        case T:
                            ranges.add(new Range(_timeIdx, _timeIdx, 1));
                            break;
                        case Z:
                            ranges.add(new Range(0, dim.getLength() - 1, 1));
                            break;
                        case Y:
                            ranges.add(new Range(0, dim.getLength() - 1, 1));
                            break;
                        case X:
                            ranges.add(new Range(_lonIdx, _lonIdx, 1));
                            break;
                        default:
                            ranges.add(new Range(0, 0, 1));
                    }
                }
                break;
            case Level_Time:
                for (Dimension dim : dims) {
                    switch (dim.getDimType()) {
                        case T:
                            ranges.add(new Range(0, dim.getLength() - 1, 1));
                            break;
                        case Z:
                            ranges.add(new Range(0, dim.getLength() - 1, 1));
                            break;
                        case Y:
                            ranges.add(new Range(_latIdx, _latIdx, 1));
                            break;
                        case X:
                            ranges.add(new Range(_lonIdx, _lonIdx, 1));
                            break;
                        default:
                            ranges.add(new Range(0, 0, 1));
                    }
                }
                break;
            case Lat:
                for (Dimension dim : dims) {
                    switch (dim.getDimType()) {
                        case T:
                            ranges.add(new Range(_timeIdx, _timeIdx, 1));
                            break;
                        case Z:
                            ranges.add(new Range(_levelIdx, _levelIdx, 1));
                            break;
                        case Y:
                            ranges.add(new Range(0, dim.getLength() - 1, 1));
                            break;
                        case X:
                            ranges.add(new Range(_lonIdx, _lonIdx, 1));
                            break;
                        default:
                            ranges.add(new Range(0, 0, 1));
                    }
                }
                break;
            case Level:
                for (Dimension dim : dims) {
                    switch (dim.getDimType()) {
                        case T:
                            ranges.add(new Range(_timeIdx, _timeIdx, 1));
                            break;
                        case Z:
                            ranges.add(new Range(0, dim.getLength() - 1, 1));
                            break;
                        case Y:
                            ranges.add(new Range(_latIdx, _latIdx, 1));
                            break;
                        case X:
                            ranges.add(new Range(_lonIdx, _lonIdx, 1));
                            break;
                        default:
                            ranges.add(new Range(0, 0, 1));
                    }
                }
                break;
            case Lon:
                for (Dimension dim : dims) {
                    switch (dim.getDimType()) {
                        case T:
                            ranges.add(new Range(_timeIdx, _timeIdx, 1));
                            break;
                        case Z:
                            ranges.add(new Range(_levelIdx, _levelIdx, 1));
                            break;
                        case Y:
                            ranges.add(new Range(_latIdx, _latIdx, 1));
                            break;
                        case X:
                            ranges.add(new Range(0, dim.getLength() - 1, 1));
                            break;
                        default:
                            ranges.add(new Range(0, 0, 1));
                    }
                }
                break;
            case Time:
                for (Dimension dim : dims) {
                    switch (dim.getDimType()) {
                        case T:
                            ranges.add(new Range(0, dim.getLength() - 1, 1));
                            break;
                        case Z:
                            ranges.add(new Range(_levelIdx, _levelIdx, 1));
                            break;
                        case Y:
                            ranges.add(new Range(_latIdx, _latIdx, 1));
                            break;
                        case X:
                            ranges.add(new Range(_lonIdx, _lonIdx, 1));
                            break;
                        default:
                            ranges.add(new Range(0, 0, 1));
                    }
                }
                break;
        }
        DimArray gdata = this.read(var.getShortName(), ranges);

        return gdata;
    }

    /**
     * Get station data
     *
     * @param varName Variable name
     * @return Station data
     */
    public DimArray getStationData(String varName) {
        _varIdx = getVariableIndex(varName);
        if (_varIdx >= 0) {
            try {
                return this.getStationData();
            } catch (InvalidRangeException ex) {
                Logger.getLogger(MeteoDataInfo.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        } else {
//            MathParser mathParser = new MathParser(this);
//            try {
//                StationData stationData = (StationData) mathParser.evaluate(varName);
//                stationData.projInfo = this.getProjectionInfo();
//                return stationData;
//            } catch (ParseException ex) {
//                Logger.getLogger(MeteoDataInfo.class.getName()).log(Level.SEVERE, null, ex);
//                return null;
//            } catch (IOException ex) {
//                Logger.getLogger(MeteoDataInfo.class.getName()).log(Level.SEVERE, null, ex);
//                return null;
//            }
            return null;
        }
    }

    /**
     * Get station data
     *
     * @return Station data
     * @throws org.meteothink.ndarray.InvalidRangeException
     */
    public DimArray getStationData() throws InvalidRangeException {
        if (_varIdx >= 0) {
            Variable var = this.getDataInfo().getVariables().get(_varIdx);
            List<Dimension> dims = var.getDimensions();
            List<Range> ranges = new ArrayList<>();
            for (Dimension dim : dims) {
                switch (dim.getDimType()) {
                    case T:
                        ranges.add(new Range(_timeIdx, _timeIdx, 1));
                        break;
                    case Z:
                        ranges.add(new Range(_levelIdx, _levelIdx, 1));
                        break;
                    default:
                        ranges.add(new Range(0, dim.getLength() - 1, 1));
                }
            }
            DimArray stData = this.read(var.getShortName(), ranges);
            return stData;
        } else {
            return null;
        }
    }

    /**
     * Get variable index
     *
     * @param varName Variable name
     * @return Variable index
     */
    public int getVariableIndex(String varName) {
        List<String> varList = _dataInfo.getVariableNames();
        int idx = varList.indexOf(varName);

        return idx;
    }

    // </editor-fold>
    // <editor-fold desc="Others">
    @Override
    public String toString() {
        return new File(this.getFileName()).getName();
    }
    // </editor-fold>
    // </editor-fold>
}
