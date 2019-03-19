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
package org.meteothink.data.meteodata.ascii;

import org.meteothink.data.meteodata.DataInfo;
import org.meteothink.ndarray.Dimension;
import org.meteothink.ndarray.DimensionType;
import org.meteothink.data.meteodata.Variable;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.meteothink.data.meteodata.MeteoDataType;
import org.meteothink.ndarray.Array;
import org.meteothink.ndarray.DataType;
import org.meteothink.ndarray.IndexIterator;
import org.meteothink.ndarray.InvalidRangeException;
import org.meteothink.ndarray.Range;
import org.meteothink.ndarray.Section;
import org.meteothink.data.meteodata.Attribute;

/**
 *
 * @author yaqiang
 */
public class SurferGridDataInfo extends DataInfo {

    // <editor-fold desc="Variables">
    // </editor-fold>
    // <editor-fold desc="Constructor">

    /**
     * Constructor
     */
    public SurferGridDataInfo() {
        this.setDataType(MeteoDataType.Sufer_Grid);
    }
    // </editor-fold>
    // <editor-fold desc="Get Set Methods">
    // </editor-fold>
    // <editor-fold desc="Methods">

    @Override
    public void readDataInfo(String fileName) {
        try {
            this.setFileName(fileName);

            BufferedReader sr = new BufferedReader(new FileReader(new File(fileName)));
            double xmin, ymin, xmax, ymax, zmin, zmax;
            int xnum, ynum, i;
            String aLine;
            String[] dataArray;

            aLine = sr.readLine().trim();
            for (i = 1; i <= 4; i++) {
                aLine = aLine + " " + sr.readLine().trim();
            }

            dataArray = aLine.split("\\s+");
            xnum = Integer.parseInt(dataArray[1]);
            ynum = Integer.parseInt(dataArray[2]);
            xmin = Double.parseDouble(dataArray[3]);
            xmax = Double.parseDouble(dataArray[4]);
            ymin = Double.parseDouble(dataArray[5]);
            ymax = Double.parseDouble(dataArray[6]);
            zmin = Double.parseDouble(dataArray[7]);
            zmax = Double.parseDouble(dataArray[8]);

            double xdelt = (xmax - xmin) / (xnum - 1);
            double ydelt = (ymax - ymin) / (ynum - 1);
            double[] X = new double[xnum];
            for (i = 0; i < xnum; i++) {
                X[i] = xmin + i * xdelt;
            }
            if (X[xnum - 1] + xdelt - X[0] == 360) {
                this.setGlobal(true);
            }

            double[] Y = new double[ynum];
            for (i = 0; i < ynum; i++) {
                Y[i] = ymin + i * ydelt;
            }

            Dimension xDim = new Dimension(DimensionType.X);
            xDim.setValues(X);
            this.setXDimension(xDim);
            Dimension yDim = new Dimension(DimensionType.Y);
            yDim.setValues(Y);
            this.setYDimension(yDim);

            List<Variable> variables = new ArrayList<>();
            Variable aVar = new Variable();
            aVar.setName("var");            
            aVar.addDimension(yDim);
            aVar.addDimension(xDim);
            variables.add(aVar);
            this.setVariables(variables);

            sr.close();
        } catch (IOException ex) {
            Logger.getLogger(SurferGridDataInfo.class.getName()).log(Level.SEVERE, null, ex);
        }
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
        String dataInfo;
        dataInfo = "File Name: " + this.getFileName();
        dataInfo += System.getProperty("line.separator") + "Data Type: Sufer ASCII Grid";
        Dimension xdim = this.getXDimension();
        Dimension ydim = this.getYDimension();
        dataInfo += System.getProperty("line.separator") + "XNum = " + String.valueOf(xdim.getLength())
                + "  YNum = " + String.valueOf(ydim.getLength());
        dataInfo += System.getProperty("line.separator") + "XMin = " + String.valueOf(xdim.getValues()[0])
                + "  YMin = " + String.valueOf(ydim.getValues()[0]);
        dataInfo += System.getProperty("line.separator") + "XSize = " + String.valueOf(xdim.getValues()[1] - xdim.getValues()[0])
                + "  YSize = " + String.valueOf(ydim.getValues()[1] - ydim.getValues()[0]);
        dataInfo += System.getProperty("line.separator") + "UNDEF = " + String.valueOf(this.getMissingValue());

        return dataInfo;
    }
    
    /**
     * Read array data of a variable
     * 
     * @param varName Variable name
     * @return Array data
     */
    @Override
    public Array read(String varName){
        Variable var = this.getVariable(varName);
        int n = var.getDimNumber();
        int[] origin = new int[n];
        int[] size = new int[n];
        int[] stride = new int[n];
        for (int i = 0; i < n; i++){
            origin[i] = 0;
            size[i] = var.getDimLength(i);
            stride[i] = 1;
        }
        
        Array r = read(varName, origin, size, stride);
        
        return r;
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
    public Array read(String varName, int[] origin, int[] size, int[] stride) {
        try {
            Section section = new Section(origin, size, stride);
            Array dataArray = Array.factory(DataType.FLOAT, section.getShape());
            int rangeIdx = 0;
            Range yRange = section.getRange(rangeIdx++);
            Range xRange = section.getRange(rangeIdx);
            IndexIterator ii = dataArray.getIndexIterator();
            readXY(yRange, xRange, ii);

            return dataArray;
        } catch (InvalidRangeException ex) {
            Logger.getLogger(SurferGridDataInfo.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    private void readXY(Range yRange, Range xRange, IndexIterator ii) {
        try {
            int xNum = this.getXDimension().getLength();
            int yNum = this.getYDimension().getLength();
            float[] data = new float[yNum * xNum];
            BufferedReader sr = new BufferedReader(new FileReader(new File(this.getFileName())));
            String[] dataArray;
            int i, j;
            String aLine;

            for (i = 0; i < 5; i++) {
                sr.readLine();
            }

            int idx = 0;
            aLine = sr.readLine();
            while (aLine != null) {
                dataArray = aLine.trim().split("\\s+");
                for (String dstr : dataArray) {
                    data[idx] = Float.parseFloat(dstr);
                    idx += 1;
                }
                aLine = sr.readLine();
            }
            sr.close();

            for (int y = yRange.first(); y <= yRange.last();
                    y += yRange.stride()) {
                for (int x = xRange.first(); x <= xRange.last();
                        x += xRange.stride()) {
                    int index = y * xNum + x;
                    ii.setFloatNext(data[index]);
                }
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(SurferGridDataInfo.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(SurferGridDataInfo.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    // </editor-fold>
}
