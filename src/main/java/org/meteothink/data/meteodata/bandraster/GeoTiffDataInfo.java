 /* Copyright 2012 - Yaqiang Wang,
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
package org.meteothink.data.meteodata.bandraster;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.meteothink.data.mapdata.geotiff.GeoTiff;
import org.meteothink.data.meteodata.DataInfo;
import org.meteothink.ndarray.Dimension;
import org.meteothink.ndarray.DimensionType;
import org.meteothink.data.meteodata.MeteoDataType;
import org.meteothink.data.meteodata.Variable;
import org.meteothink.ndarray.Array;
import org.meteothink.ndarray.IndexIterator;
import org.meteothink.ndarray.InvalidRangeException;
import org.meteothink.ndarray.Range;
import org.meteothink.data.meteodata.Attribute;
import org.meteothink.ndarray.DimArray;

/**
 *
 * @author yaqiang
 */
public class GeoTiffDataInfo extends DataInfo {

    // <editor-fold desc="Variables">
    private GeoTiff geoTiff;
    private int bandNum;

    // </editor-fold>
    // <editor-fold desc="Constructor">
    /**
     * Constructor
     */
    public GeoTiffDataInfo() {
        this.setDataType(MeteoDataType.GEOTIFF);
    }
    // </editor-fold>
    // <editor-fold desc="Get Set Methods">
    // </editor-fold>
    // <editor-fold desc="Methods">

    @Override
    public void readDataInfo(String fileName) {
        this.setFileName(fileName);
        geoTiff = new GeoTiff(fileName);
        try {
            geoTiff.read();
        } catch (IOException ex) {
            Logger.getLogger(GeoTiffDataInfo.class.getName()).log(Level.SEVERE, null, ex);
        }
        List<double[]> xy = geoTiff.readXY();
        double[] X = xy.get(0);
        double[] Y = xy.get(1);
        Dimension xDim = new Dimension(DimensionType.X);
        xDim.setValues(X);
        this.setXDimension(xDim);
        this.addDimension(xDim);
        Dimension yDim = new Dimension(DimensionType.Y);
        yDim.setValues(Y);
        yDim.setReverse(true);
        this.setYDimension(yDim);
        this.addDimension(yDim);
        this.bandNum = this.geoTiff.getBandNum();
        Dimension bDim = null;
        if (this.bandNum > 1){
            bDim = new Dimension(DimensionType.Other);
            bDim.setValues(new double[this.bandNum]);
            this.addDimension(bDim);
        }
        List<Variable> variables = new ArrayList<>();
        Variable aVar = new Variable();
        aVar.setName("var");
        aVar.addDimension(yDim);
        aVar.addDimension(xDim);
        if (this.bandNum > 1){
            aVar.addDimension(bDim);
        }
        variables.add(aVar);
        this.setVariables(variables);
        this.setCRS(geoTiff.readProj());
    }
    
    /**
     * Get global attributes
     * @return Global attributes
     */
    @Override
    public List<Attribute> getGlobalAttributes(){
        return new ArrayList<>();
    }

    @Override
    public String generateInfoText() {
        String dataInfo = "Data Type: GeoTiff";        
        dataInfo += System.getProperty("line.separator") + super.generateInfoText();

        return dataInfo;
    }
    
    /**
     * Read array data of a variable
     * 
     * @param varName Variable name
     * @return Array data
     */
    @Override
    public DimArray read(String varName){        
        Array r = null;
        try {
            r = this.geoTiff.readArray();
        } catch (IOException ex) {
            Logger.getLogger(GeoTiffDataInfo.class.getName()).log(Level.SEVERE, null, ex);
        }
        Variable var = this.getVariable(varName);
        
        return new DimArray(r, var.getDimensions());
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
    @Override
    public DimArray read(String varName, int[] origin, int[] size, int[] stride) {
        try {
            DimArray da = read(varName);
            return da.section(origin, size, stride);
        } catch (InvalidRangeException ex) {
            Logger.getLogger(GeoTiffDataInfo.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }        
    }
    
    private void readXY(Range yRange, Range xRange, IndexIterator ii) {
        int[][] data = this.geoTiff.readData();
        for (int y = yRange.first(); y <= yRange.last();
                y += yRange.stride()) {
            for (int x = xRange.first(); x <= xRange.last();
                    x += xRange.stride()) {
                ii.setFloatNext(data[y][x]);
            }
        }
    }
    
    // </editor-fold>
}
