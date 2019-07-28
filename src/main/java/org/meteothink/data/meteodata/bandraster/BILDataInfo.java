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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.meteothink.data.meteodata.DataInfo;
import org.meteothink.ndarray.Dimension;
import org.meteothink.ndarray.DimensionType;
import org.meteothink.data.meteodata.MeteoDataType;
import org.meteothink.data.meteodata.Variable;
import org.meteothink.util.DataConvert;
import org.meteothink.ndarray.Array;
import org.meteothink.ndarray.DataType;
import org.meteothink.ndarray.Index2D;
import org.meteothink.ndarray.IndexIterator;
import org.meteothink.ndarray.InvalidRangeException;
import org.meteothink.ndarray.Range;
import org.meteothink.ndarray.Section;
import org.meteothink.data.meteodata.Attribute;
import org.meteothink.ndarray.DimArray;

/**
 *
 * @author yaqiang
 */
public class BILDataInfo extends DataInfo {

    // <editor-fold desc="Variables">
    ByteOrder _byteOrder = ByteOrder.LITTLE_ENDIAN;
    private String _layout = "BIL";
    private int _nrows;
    private int _ncols;
    private int _nbands = 1;
    private int _nbits = 8;
    private String _pixeltype = "UNSIGNEDINT";
    private int _skipbytes = 0;
    private int _bandrowbytes = 0;
    private int _totalrowbytes = 0;

    // </editor-fold>
    // <editor-fold desc="Constructor">
    public BILDataInfo() {
        this.setDataType(MeteoDataType.BIL);
    }
    // </editor-fold>
    // <editor-fold desc="Get Set Methods">
    // </editor-fold>
    // <editor-fold desc="Methods">

    @Override
    public void readDataInfo(String fileName) {
        this.setFileName(fileName);

        //Find header file
        String hfn = fileName.replace(fileName.substring(fileName.lastIndexOf(".")), ".hdr");
        if (new File(hfn).exists()) {
            try {
                BufferedReader sr = new BufferedReader(new FileReader(new File(hfn)));
                String line = "";
                String[] dataArray;
                String key;
                double ulxmap = 0, ulymap = 0, xdim = 1, ydim = 1, nodata = -9999;
                int mn = 0;
                line = sr.readLine();
                while (line != null) {
                    if (line.isEmpty()) {
                        line = sr.readLine();
                        continue;
                    }
                    dataArray = line.split("\\s+");
                    key = dataArray[0].trim().toLowerCase();
                    switch (key) {
                        case "nrows":
                            _nrows = Integer.parseInt(dataArray[1]);
                            break;
                        case "ncols":
                            _ncols = Integer.parseInt(dataArray[1]);
                            break;
                        case "nbits":
                            _nbits = Integer.parseInt(dataArray[1]);
                            break;
                        case "pixeltype":
                            _pixeltype = dataArray[1].trim();
                            break;
                        case "byteorder":
                            String byteOrder = dataArray[1].trim();
                            if (byteOrder.toLowerCase().equals("m")) {
                                this._byteOrder = ByteOrder.BIG_ENDIAN;
                            }   break;
                        case "layout":
                            _layout = dataArray[1].trim();
                            break;
                        case "bandrowbytes":
                            _bandrowbytes = Integer.parseInt(dataArray[1]);
                            break;
                        case "totalrowbytes":
                            _totalrowbytes = Integer.parseInt(dataArray[1]);
                            break;
                        case "ulxmap":
                            ulxmap = Double.parseDouble(dataArray[1]);
                            mn += 1;
                            break;
                        case "ulymap":
                            ulymap = Double.parseDouble(dataArray[1]);
                            mn += 1;
                            break;
                        case "xdim":
                            xdim = Double.parseDouble(dataArray[1]);
                            mn += 1;
                            break;
                        case "ydim":
                            ydim = Double.parseDouble(dataArray[1]);
                            mn += 1;
                            break;
                        case "nodata":
                            nodata = Double.parseDouble(dataArray[1]);
                            break;
                        default:
                            break;
                    }

                    line = sr.readLine();
                }
                sr.close();

                if (this._bandrowbytes == 0) {
                    if (this._layout.toLowerCase().equals("bil")) {
                        this._bandrowbytes = this._ncols * this._nbits / 8;
                        this._totalrowbytes = this._bandrowbytes * this._nbands;
                    }
                }

                this.setMissingValue(nodata);

                //Get X/Y coordinate
                if (mn < 4) {
                    String wfn = fileName.replace(fileName.substring(fileName.lastIndexOf(".")), ".blw");
                    if (new File(wfn).exists()) {
                        sr = new BufferedReader(new FileReader(new File(wfn)));
                        xdim = Double.parseDouble(sr.readLine());
                        sr.readLine();
                        sr.readLine();
                        ydim = -Double.parseDouble(sr.readLine());
                        ulxmap = Double.parseDouble(sr.readLine());
                        ulymap = Double.parseDouble(sr.readLine());
                        sr.close();
                    }
                }

                double[] X = new double[_ncols];
                int i;
                for (i = 0; i < _ncols; i++) {
                    X[i] = ulxmap + i * xdim;
                }
                if (X[_ncols - 1] + xdim - X[0] == 360) {
                    this.setGlobal(true);
                }

                double[] Y = new double[_nrows];
                for (i = 0; i < _nrows; i++) {
                    Y[_nrows - 1 - i] = ulymap - i * ydim;
                }

                Dimension xDim = new Dimension(DimensionType.X);
                xDim.setValues(X);
                this.setXDimension(xDim);
                this.addDimension(xDim);
                Dimension yDim = new Dimension(DimensionType.Y);
                yDim.setValues(Y);
                this.setYDimension(yDim);
                this.addDimension(yDim);

                List<Variable> variables = new ArrayList<>();
                DataType dtype = DataType.INT;                
                switch (this._pixeltype.toLowerCase()){
                    case "float":
                        dtype = DataType.FLOAT;
                        break;                    
                }
                int nbytes = this._nbits / 8;
                if (nbytes == 1){
                    dtype = DataType.BYTE;
                }
                for (i = 0; i < this._nbands; i++) {
                    Variable aVar = new Variable();
                    aVar.setName("band" + String.valueOf(i));
                    aVar.setDataType(dtype);
                    aVar.addDimension(yDim);
                    aVar.addDimension(xDim);                    
                    aVar.setFillValue(nodata);
                    variables.add(aVar);
                }
                this.setVariables(variables);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(BILDataInfo.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(BILDataInfo.class.getName()).log(Level.SEVERE, null, ex);
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
        dataInfo += System.getProperty("line.separator") + "Data Type: BIL Grid";
        Dimension xdim = this.getXDimension();
        Dimension ydim = this.getYDimension();
        dataInfo += System.getProperty("line.separator") + "XNum = " + String.valueOf(xdim.getLength())
                + "  YNum = " + String.valueOf(ydim.getLength());
        dataInfo += System.getProperty("line.separator") + "XMin = " + String.valueOf(xdim.getValues()[0])
                + "  YMin = " + String.valueOf(ydim.getValues()[0]);
        dataInfo += System.getProperty("line.separator") + "XSize = " + String.valueOf(xdim.getValues()[1] - xdim.getValues()[0])
                + "  YSize = " + String.valueOf(ydim.getValues()[1] - ydim.getValues()[0]);
        dataInfo += System.getProperty("line.separator") + "UNDEF = " + String.valueOf(this.getMissingValue());
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
        return readArray_bil(varName);
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
            Section section = new Section(origin, size, stride);
            Variable var = this.getVariable(varName);
            int varIdx = this.getVariables().indexOf(var);
            Array array = Array.factory(var.getDataType(), section.getShape());
            int rangeIdx = 0;
            Range yRange = section.getRange(rangeIdx++);
            Range xRange = section.getRange(rangeIdx);
            IndexIterator ii = array.getIndexIterator();
            this.readArray_bil_xy(varIdx, array.getDataType(), yRange, xRange, ii);
            return new DimArray(array.reduce(), var.getDimensions(section));
        } catch (InvalidRangeException ex) {
            Logger.getLogger(BILDataInfo.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    private DimArray readArray_bil(String varName) {
        Variable var = this.getVariable(varName);
        int varIdx = this.getVariables().indexOf(var);
        try {
            RandomAccessFile br = new RandomAccessFile(this.getFileName(), "r");
            Array gData = Array.factory(var.getDataType(), new int[]{_nrows, _ncols});
            Index2D index = (Index2D)gData.getIndex();
            
            br.seek(this._skipbytes);
            int i, j;
            int nbytes = this._nbits / 8;
            byte[] bytes;
            int start;
            long position;
            for (i = 0; i < _nrows; i++) {
                position = br.getFilePointer();
                if (varIdx > 0) {
                    br.seek(this._bandrowbytes * varIdx);
                }
                byte[] byteData = new byte[_ncols * nbytes];
                br.read(byteData);
                start = 0;
                for (j = 0; j < _ncols; j++) {
                    bytes = new byte[nbytes];
                    System.arraycopy(byteData, start, bytes, 0, nbytes);
                    start += nbytes;
                    index.set(_nrows - 1 - i, j);
                    switch (gData.getDataType()){
                        case FLOAT:
                            gData.setFloat(index, DataConvert.bytes2Float(bytes, _byteOrder));
                            break;
                        case INT:
                            if (nbytes >= 2) {
                                gData.setInt(index, DataConvert.bytes2Int(bytes, _byteOrder));
                            } else {
                                gData.setByte(index, bytes[0]);
                            }
                            break;
                    }
                }
                br.seek(position + this._totalrowbytes);
            }

            br.close();
            return new DimArray(gData, var.getDimensions());
        } catch (FileNotFoundException ex) {
            Logger.getLogger(BILDataInfo.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(BILDataInfo.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }
    
    private void readArray_bil_xy(int varIdx, DataType dtype, Range yRange, Range xRange, IndexIterator ii) {
        try {
            RandomAccessFile br = new RandomAccessFile(this.getFileName(), "r");            
            br.seek(this._skipbytes);
            int i, j;
            int nbytes = this._nbits / 8;
            byte[] bytes;
            int start;
            long position;
            switch (dtype) {
                case FLOAT:
                    float vf;
                    for (int y = yRange.first(); y <= yRange.last(); y += yRange.stride()) {
                        position = br.getFilePointer();
                        if (varIdx > 0) {
                            br.seek(this._bandrowbytes * varIdx);
                        }
                        byte[] byteData = new byte[_ncols * nbytes];
                        br.read(byteData);
                        start = 0;
                        for (int x = xRange.first(); x <= xRange.last(); x += xRange.stride()) {
                            bytes = new byte[nbytes];
                            System.arraycopy(byteData, start, bytes, 0, nbytes);
                            start += nbytes * xRange.stride();
                            vf = DataConvert.bytes2Float(bytes, _byteOrder);
                            ii.setFloatNext(vf);
                        }
                        if (y < yRange.last()){
                            br.seek(position + this._totalrowbytes * (yRange.stride()));
                        }
                    }
                    break;
                case INT:
                    int vi;
                    for (int y = yRange.first(); y <= yRange.last(); y += yRange.stride()) {
                        position = br.getFilePointer();
                        if (varIdx > 0) {
                            br.seek(this._bandrowbytes * varIdx);
                        }
                        byte[] byteData = new byte[_ncols * nbytes];
                        br.read(byteData);
                        start = 0;
                        for (int x = xRange.first(); x <= xRange.last(); x += xRange.stride()) {
                            bytes = new byte[nbytes];
                            System.arraycopy(byteData, start, bytes, 0, nbytes);
                            start += nbytes * xRange.stride();
                            vi = DataConvert.bytes2Int(bytes, _byteOrder);
                            ii.setIntNext(vi);
                        }
                        if (y < yRange.last()){
                            br.seek(position + this._totalrowbytes * (yRange.stride()));
                        }
                    }
                    break;
                case BYTE:
                    for (int y = yRange.first(); y <= yRange.last(); y += yRange.stride()) {
                        position = br.getFilePointer();
                        if (varIdx > 0) {
                            br.seek(this._bandrowbytes * varIdx);
                        }
                        byte[] byteData = new byte[_ncols * nbytes];
                        br.read(byteData);
                        start = 0;
                        for (int x = xRange.first(); x <= xRange.last(); x += xRange.stride()) {
                            bytes = new byte[nbytes];
                            System.arraycopy(byteData, start, bytes, 0, nbytes);
                            start += nbytes * xRange.stride();
                            ii.setByteNext(bytes[0]);
                        }
                        if (y < yRange.last()){
                            br.seek(position + this._totalrowbytes * (yRange.stride()));
                        }
                    }
                    break;                    
            }
            
            br.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(BILDataInfo.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(BILDataInfo.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    // </editor-fold>
}
