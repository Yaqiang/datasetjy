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
package org.meteothink.data.meteodata.hysplit;

import org.meteothink.data.meteodata.DataInfo;
import org.meteothink.ndarray.Dimension;
import org.meteothink.ndarray.DimensionType;
import org.meteothink.data.meteodata.Variable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.meteothink.data.meteodata.MeteoDataType;
import org.meteothink.util.DateUtil;
import org.meteothink.data.meteodata.Attribute;
import org.meteothink.ndarray.DimArray;

/**
 *
 * @author yaqiang
 */
public class HYSPLITPartDataInfo extends DataInfo {

    // <editor-fold desc="Variables">
    private List<List<Integer>> _parameters = new ArrayList<>();
    // </editor-fold>
    // <editor-fold desc="Constructor">
    /**
     * Constructor
     */
    public HYSPLITPartDataInfo(){
        this.setDataType(MeteoDataType.HYSPLIT_Particle);
    }
    // </editor-fold>
    // <editor-fold desc="Get Set Methods">
    // </editor-fold>
    // <editor-fold desc="Methods">

    @Override
    public void readDataInfo(String fileName) {
        try {
            this.setFileName(fileName);
            RandomAccessFile br = new RandomAccessFile(fileName, "r");
            int year, month, day, hour;
            List<Date> times = new ArrayList<Date>();
            _parameters = new ArrayList<List<Integer>>();

            while (br.getFilePointer() < br.length() - 28) {
                //Read head
                int pos = (int) br.getFilePointer();
                br.skipBytes(4);
                int particleNum = br.readInt();
                int pollutantNum = br.readInt();
                year = br.readInt();
                month = br.readInt();
                day = br.readInt();
                hour = br.readInt();                
                if (year < 50) {
                    year = 2000 + year;
                } else {
                    year = 1900 + year;
                }
                Calendar cal = new GregorianCalendar(year, month - 1, day, hour, 0, 0);
                times.add(cal.getTime());
                List<Integer> data = new ArrayList<Integer>();
                data.add(particleNum);
                data.add(pollutantNum);
                data.add(pos);
                _parameters.add(data);

                //Skip data
                int len = (8 + pollutantNum * 4 + 60) * particleNum + 4;
                br.skipBytes(len);
            }

            br.close();

            List<Double> values = new ArrayList<Double>();
            for (Date t : times) {
                values.add(DateUtil.toOADate(t));
            }
            Dimension tDim = new Dimension(DimensionType.T);
            tDim.setValues(values);
            this.setTimeDimension(tDim);
            
            Variable var = new Variable();
            var.setStation(true);
            var.setName("Particle");
            List<Variable> variables = new ArrayList<>();
            variables.add(var);
            this.setVariables(variables);
            
        } catch (IOException ex) {
            Logger.getLogger(HYSPLITPartDataInfo.class.getName()).log(Level.SEVERE, null, ex);
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
        List<Date> times = this.getTimes();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:00");
        for (int i = 0; i < this.getTimeNum(); i++) {
            dataInfo += System.getProperty("line.separator") + "Time: " + format.format(times.get(i));
            dataInfo += System.getProperty("line.separator") + "\tParticle Number: " + _parameters.get(i).get(0);
            dataInfo += System.getProperty("line.separator") + "\tPollutant Number: " + _parameters.get(i).get(1);
        }

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
        return null;
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
        return null;
    }

    // </editor-fold>
}
