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
package org.meteothink.data.meteodata.micaps;

import org.meteothink.data.meteodata.DataInfo;
import org.meteothink.ndarray.Dimension;
import org.meteothink.ndarray.DimensionType;
import org.meteothink.data.meteodata.Variable;
import org.meteothink.util.MIMath;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.meteothink.data.meteodata.MeteoDataType;
import org.meteothink.util.DateUtil;
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
public class MICAPS4DataInfo extends DataInfo {

    // <editor-fold desc="Variables">
    private String _description;
    private double[] _xArray;
    private double[] _yArray;
    private int _headLineNum;
    private boolean _yReverse = false;
    private int _preHours;
    private int _level;
    // </editor-fold>
    // <editor-fold desc="Constructor">

    /**
     * Constructor
     */
    public MICAPS4DataInfo() {
        this.setDataType(MeteoDataType.MICAPS_4);
        this.setMissingValue(9999.0);
    }
    // </editor-fold>
    // <editor-fold desc="Get Set Methods">
    // </editor-fold>
    // <editor-fold desc="Methods">

    @Override
    public void readDataInfo(String fileName) {
        try {
            BufferedReader sr = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "gbk"));
            String aLine;
            String[] dataArray;
            int i, n;
            List<String> dataList = new ArrayList<>();

            this.setFileName(fileName);
            aLine = sr.readLine().trim();
            _description = aLine;
            aLine = sr.readLine().trim();
            dataArray = aLine.split("\\s+");
            for (i = 0; i < dataArray.length; i++) {
                if (!dataArray[i].isEmpty()) {
                    dataList.add(dataArray[i]);
                }
            }
            _headLineNum = 2;
            for (n = 0; n <= 10; n++) {
                if (dataList.size() < 19) {
                    aLine = sr.readLine().trim();
                    dataArray = aLine.split("\\s+");
                    for (i = 0; i < dataArray.length; i++) {
                        if (!dataArray[i].isEmpty()) {
                            dataList.add(dataArray[i]);
                        }
                    }
                    _headLineNum += 1;
                } else {
                    break;
                }
            }
            sr.close();

            int year = Integer.parseInt(dataList.get(0));
            if (year < 100) {
                if (year < 50) {
                    year = 2000 + year;
                } else {
                    year = 1900 + year;
                }
            }
            _preHours = Integer.parseInt(dataList.get(4));
            Calendar cal = new GregorianCalendar(year, Integer.parseInt(dataList.get(1)) - 1, Integer.parseInt(dataList.get(2)),
                    Integer.parseInt(dataList.get(3)), 0, 0);
            cal.add(Calendar.HOUR_OF_DAY, _preHours);
            Date time = cal.getTime();
            
            _level = Integer.parseInt(dataList.get(5));
            float XDelt = Float.parseFloat(dataList.get(6));
            float YDelt = Float.parseFloat(dataList.get(7));
            float XMin = Float.parseFloat(dataList.get(8));
            float XMax = Float.parseFloat(dataList.get(9));
            float YMin = Float.parseFloat(dataList.get(10));
            float YMax = Float.parseFloat(dataList.get(11));
            int XNum = Integer.parseInt(dataList.get(12));
            int YNum = Integer.parseInt(dataList.get(13));
            float contourDelt = Float.parseFloat(dataList.get(14));
            float contourSValue = Float.parseFloat(dataList.get(15));
            float contourEValue = Float.parseFloat(dataList.get(16));
            float smoothCo = Float.parseFloat(dataList.get(17));
            float boldValue = Float.parseFloat(dataList.get(18));
            boolean isLonLat;
            if (dataList.get(16).equals("-1") || dataList.get(16).equals("-2") || dataList.get(16).equals("-3")) {
                isLonLat = false;
            } else {
                isLonLat = true;
            }
            _xArray = new double[XNum];
            for (i = 0; i < XNum; i++) {
                _xArray[i] = XMin + i * XDelt;
            }
            _yArray = new double[YNum];

            _yReverse = false;
            if (YDelt < 0) {
                _yReverse = true;
                YDelt = -YDelt;
            }
            if (YMin > YMax) {
                float temp = YMin;
                YMin = YMax;
                YMax = temp;
            }
            for (i = 0; i < YNum; i++) {
                _yArray[i] = YMin + i * YDelt;
            }

            Dimension tdim = new Dimension(DimensionType.T);
            double[] values = new double[1];
            values[0] = DateUtil.toOADate(time);
            tdim.setValues(values);
            this.setTimeDimension(tdim);
            this.addDimension(tdim);
            Dimension zdim = new Dimension(DimensionType.Z);
            zdim.setValues(new double[]{_level});
            this.addDimension(zdim);
            Dimension xdim = new Dimension(DimensionType.X);
            xdim.setValues(_xArray);
            this.setXDimension(xdim);            
            Dimension ydim = new Dimension(DimensionType.Y);
            ydim.setValues(_yArray);
            this.setYDimension(ydim);
            this.addDimension(ydim);
            this.addDimension(xdim);

            List<Variable> variables = new ArrayList<>();
            Variable var = new Variable();
            var.setName("var");
            var.setDataType(DataType.FLOAT);
            var.setDimension(tdim);
            var.setDimension(zdim);
            var.setDimension(ydim);
            var.setDimension(xdim);
            var.setFillValue(this.getMissingValue());
            variables.add(var);
            this.setVariables(variables);
        } catch (IOException ex) {
            Logger.getLogger(MICAPS4DataInfo.class.getName()).log(Level.SEVERE, null, ex);
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

//    @Override
//    public String generateInfoText() {
//        String dataInfo;
//        dataInfo = "File Name: " + this.getFileName();
//        dataInfo += System.getProperty("line.separator") + "Description: " + _description;
//        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:00");
//        dataInfo += System.getProperty("line.separator") + "Time: " + format.format(this.getTimes().get(0));
//        dataInfo += System.getProperty("line.separator") + "Forecast Hours = " + String.valueOf(_preHours)
//                + "  Level = " + String.valueOf(_level);
//        dataInfo += System.getProperty("line.separator") + "Xsize = " + String.valueOf(this.getXDimension().getLength())
//                + "  Ysize = " + String.valueOf(this.getYDimension().getLength());
//
//        return dataInfo;
//    }
    
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
            int rangeIdx = 2;
            Range yRange = section.getRange(rangeIdx++);
            Range xRange = section.getRange(rangeIdx);
            IndexIterator ii = dataArray.getIndexIterator();
            readXY(yRange, xRange, ii);

            return dataArray;
        } catch (InvalidRangeException ex) {
            Logger.getLogger(MICAPS4DataInfo.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    private void readXY(Range yRange, Range xRange, IndexIterator ii) {
        try {
            BufferedReader sr = new BufferedReader(new InputStreamReader(new FileInputStream(this.getFileName()), "gbk"));
            int i, j;
            for (i = 0; i < _headLineNum; i++) {
                sr.readLine();
            }

            List<String> dataList = new ArrayList<>();
            String[] dataArray;
            int col = 0;
            String aLine;
            int xNum = this.getXDimension().getLength();
            int yNum = this.getYDimension().getLength();
            float[][] theData = new float[yNum][xNum];
            do {
                aLine = sr.readLine();
                if (aLine == null) {
                    break;
                }
                aLine = aLine.trim();
                if (aLine.isEmpty())
                    continue;
                dataArray = aLine.split("\\s+");
                dataList.addAll(Arrays.asList(dataArray));
                if (col == 0) {
                    if (!MIMath.isNumeric(dataList.get(0))) {
                        aLine = sr.readLine().trim();
                        dataArray = aLine.split("\\s+");
                        dataList.clear();
                        dataList.addAll(Arrays.asList(dataArray));
                    }
                }
                for (i = 0; i < 1000; i++) {
                    if (dataList.size() < xNum) {
                        aLine = sr.readLine();
                        if (aLine == null) {
                            break;
                        }
                        aLine = aLine.trim();
                        dataArray = aLine.split("\\s+");
                        dataList.addAll(Arrays.asList(dataArray));
                    } else {
                        break;
                    }
                }
                for (i = 0; i < xNum; i++) {
                    theData[col][i] = Float.parseFloat(dataList.get(i));
                }
                if (dataList.size() > xNum) {
                    dataList = dataList.subList(xNum, dataList.size());
                } else {
                    dataList = new ArrayList<>();
                }
                col += 1;
            } while (aLine != null);

            sr.close();
            
            float[] data = new float[yNum * xNum];
            if (this._yReverse){
                for (i = 0; i < yNum; i++) {
                    for (j = 0; j < xNum; j++) {
                        data[i * xNum + j] = theData[yNum - 1 - i][j];
                    }
                }
            } else {
                for (i = 0; i < yNum; i++) {
                    for (j = 0; j < xNum; j++) {
                        data[i * xNum + j] = theData[i][j];
                    }
                }
            }

            for (int y = yRange.first(); y <= yRange.last();
                    y += yRange.stride()) {
                for (int x = xRange.first(); x <= xRange.last();
                        x += xRange.stride()) {
                    int index = y * xNum + x;
                    ii.setFloatNext(data[index]);
                }
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(MICAPS4DataInfo.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(MICAPS4DataInfo.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
        
    // </editor-fold>
}
