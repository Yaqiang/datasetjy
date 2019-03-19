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
import org.meteothink.data.meteodata.Variable;
import org.meteothink.common.MIMath;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import org.meteothink.common.io.FileCharsetDetector;
import org.meteothink.data.meteodata.MeteoDataType;
import org.meteothink.util.GlobalUtil;
import org.meteothink.ndarray.Array;
import org.meteothink.data.meteodata.Attribute;

/**
 *
 * @author yaqiang
 */
public class LonLatStationDataInfo extends DataInfo {
    // <editor-fold desc="Variables">

    private List<String> _fields = new ArrayList<>();
    private String delimiter = null;

    //private int lonIdx = 1;
    //private int latIdx = 2;
    // </editor-fold>
    // <editor-fold desc="Constructor">

    /**
     * Constructor
     */
    public LonLatStationDataInfo() {
        this.setDataType(MeteoDataType.LonLatStation);
    }
    // </editor-fold>
    // <editor-fold desc="Get Set Methods">
    // </editor-fold>
    // <editor-fold desc="Methods">

    @Override
    public void readDataInfo(String fileName) {
        BufferedReader sr = null;
        try {
            this.setFileName(fileName);
            FileCharsetDetector chardet = new FileCharsetDetector();
            String charset = chardet.guestFileEncoding(this.getFileName());
            sr = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), charset));
            String[] dataArray, fieldArray;
            String aLine = sr.readLine().trim();    //Title
            delimiter = GlobalUtil.getDelimiter(aLine);
            fieldArray = GlobalUtil.split(aLine, delimiter);
            if (fieldArray.length < 4) {
                JOptionPane.showMessageDialog(null, "The data should have at least four fields!");
                return;
            }
            _fields = Arrays.asList(fieldArray);

            //Judge field type
            aLine = sr.readLine();    //First line
            dataArray = GlobalUtil.split(aLine, delimiter);
            List<Variable> variables = new ArrayList<>();
            for (int i = 3; i < dataArray.length; i++) {
                if (MIMath.isNumeric(dataArray[i])) {
                    Variable var = new Variable();
                    var.setName(fieldArray[i]);
                    var.setStation(true);
                    variables.add(var);
                }
            }
            this.setVariables(variables);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(LonLatStationDataInfo.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(LonLatStationDataInfo.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                sr.close();
            } catch (IOException ex) {
                Logger.getLogger(LonLatStationDataInfo.class.getName()).log(Level.SEVERE, null, ex);
            }
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
        //dataInfo += System.getProperty("line.separator") + "Station Number: " + StationNum;
        dataInfo += System.getProperty("line.separator") + "Fields: ";
        for (String aField : _fields) {
            dataInfo += System.getProperty("line.separator") + "  " + aField;
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
    public Array read(String varName){
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
    public Array read(String varName, int[] origin, int[] size, int[] stride) {
        return null;
    }

    // </editor-fold>
}
