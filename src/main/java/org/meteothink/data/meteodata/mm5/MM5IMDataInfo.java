/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.meteothink.data.meteodata.mm5;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.meteothink.data.meteodata.DataInfo;
import org.meteothink.ndarray.Dimension;
import org.meteothink.ndarray.DimensionType;
import org.meteothink.data.meteodata.MeteoDataType;
import org.meteothink.data.meteodata.Variable;
import org.meteothink.util.DataConvert;
import org.meteothink.util.DateUtil;
import org.meteothink.util.GlobalUtil;
import org.meteothink.ndarray.Array;
import org.meteothink.data.meteodata.Attribute;

/**
 * MM5 regrid intermediate data info
 *
 * @author yaqiang
 */
public class MM5IMDataInfo extends DataInfo {

    // <editor-fold desc="Variables">
    private final ByteOrder _byteOrder = ByteOrder.BIG_ENDIAN;
    private DataOutputStream _bw = null;
    private final List<DataHead> _dataHeads = new ArrayList<>();
    // </editor-fold>
    // <editor-fold desc="Constructor">
    /**
     * Constructor
     */
    public MM5IMDataInfo(){
        this.setDataType(MeteoDataType.MM5IM);
    }
    // </editor-fold>
    // <editor-fold desc="Get Set Methods">
    // </editor-fold>
    // <editor-fold desc="Methods">    

    @Override
    public void readDataInfo(String fileName) {
        this.setFileName(fileName);
        try {
            RandomAccessFile br = new RandomAccessFile(fileName, "r");
            List<Variable> variables = new ArrayList<Variable>();
            List<Date> times = new ArrayList<Date>();
            while (true) {
                if (br.getFilePointer() >= br.length() - 100) {
                    break;
                }

                long pos = br.getFilePointer();
                DataHead dh = this.readDataHead(br);
                if (!times.contains(dh.getDate()))
                    times.add(dh.getDate());
                dh.position = pos;
                dh.length = (int)(br.getFilePointer() - pos);
                _dataHeads.add(dh);
                int n = dh.idim * dh.jdim;
                br.skipBytes(n * 4 + 8);

                boolean isNewVar = true;
                for (Variable var : variables) {
                    if (var.getName().equals(dh.field)) {
                        isNewVar = false;
                        var.addLevel(dh.level);
                        break;
                    }
                }
                if (isNewVar) {
                    Variable var = new Variable();
                    var.setName(dh.field);
                    var.addLevel(dh.level);
                    var.setUnits(dh.units);
                    var.setDescription(dh.desc);
                    double[] X = new double[dh.idim];
                    int i;
                    for (i = 0; i < dh.idim; i++) {
                        X[i] = dh.startlon + dh.deltalon * i;
                    }
                    double[] Y = new double[dh.jdim];
                    for (i = 0; i < dh.jdim; i++) {
                        Y[i] = dh.startlat + dh.deltalat * (dh.jdim - 1 - i);
                    }
                    Dimension xdim = new Dimension(DimensionType.X);
                    xdim.setValues(X);
                    Dimension ydim = new Dimension(DimensionType.Y);
                    ydim.setValues(Y);
                    var.setXDimension(xdim);
                    var.setYDimension(ydim);
                    variables.add(var);
                    if (this._dataHeads.size() == 1) {
                        this.setXDimension(xdim);
                        this.setYDimension(ydim);
                    }
                }
            }
            
            List<Double> values = new ArrayList<Double>();
            for (Date t : times) {
                values.add(DateUtil.toOADate(t));
            }
            Dimension tDim = new Dimension(DimensionType.T);
            tDim.setValues(values);
            this.setTimeDimension(tDim);
            for (Variable var : variables){
                var.updateZDimension();
                var.setTDimension(tDim);
            }
            
            this.setVariables(variables);

            br.close();
        } catch (IOException ex) {
            Logger.getLogger(MM5IMDataInfo.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private DataHead readDataHead(RandomAccessFile br) throws IOException {
        DataHead dh = new DataHead();
        byte[] bytes;

        //Record 1: 4 + 8 bytes
        br.skipBytes(4);
        bytes = new byte[4];
        br.read(bytes);
        dh.iversion = DataConvert.bytes2Int(bytes, _byteOrder);
        br.skipBytes(4);

        //Record 2: 124 + 8 bytes       
        br.skipBytes(4);
        bytes = new byte[24];
        br.read(bytes);
        dh.hdate = new String(bytes).trim();
        bytes = new byte[4];
        br.read(bytes);
        dh.xfcst = DataConvert.bytes2Float(bytes, _byteOrder);
        bytes = new byte[9];
        br.read(bytes);
        dh.field = new String(bytes).trim();
        dh.field = dh.field.split("\\s+")[0];
        bytes = new byte[25];
        br.read(bytes);
        dh.units = new String(bytes).trim();
        bytes = new byte[46];
        br.read(bytes);
        dh.desc = new String(bytes).trim();
        bytes = new byte[4];
        br.read(bytes);
        dh.level = DataConvert.bytes2Float(bytes, _byteOrder);
        br.read(bytes);
        dh.idim = DataConvert.bytes2Int(bytes, _byteOrder);
        br.read(bytes);
        dh.jdim = DataConvert.bytes2Int(bytes, _byteOrder);
        br.read(bytes);
        dh.llflag = DataConvert.bytes2Int(bytes, _byteOrder);
        br.skipBytes(4);

        //Record 3: 16 + 8 bytes
        br.skipBytes(4);
        if (dh.llflag == 0) {
            br.read(bytes);
            dh.startlat = DataConvert.bytes2Float(bytes, _byteOrder);
            br.read(bytes);
            dh.startlon = DataConvert.bytes2Float(bytes, _byteOrder);
            br.read(bytes);
            dh.deltalat = DataConvert.bytes2Float(bytes, _byteOrder);
            br.read(bytes);
            dh.deltalon = DataConvert.bytes2Float(bytes, _byteOrder);
        }
        br.skipBytes(4);

        return dh;
    }
    
    private DataHead findDataHead(String varName, double level){
        for (DataHead dh : this._dataHeads){
            if (dh.field.equals(varName) && dh.level == level){
                return dh;
            }
        }
        
        return this._dataHeads.get(0);
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
        dataInfo += System.getProperty("line.separator") + "Xsize = " + String.valueOf(this.getXDimension().getLength())
                + "  Ysize = " + String.valueOf(this.getYDimension().getLength());               
        dataInfo += System.getProperty("line.separator") + "Number of Variables = " + String.valueOf(this.getVariableNum());
        for (String v : this.getVariableNames()) {
            dataInfo += System.getProperty("line.separator") + v;
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
        return null;
    }
    
    // </editor-fold>

    // <editor-fold desc="Write">
    /**
     * Create MM5 binary data file
     *
     * @param fileName File name
     */
    public void createDataFile(String fileName) {
        try {
            _bw = new DataOutputStream(new FileOutputStream(new File(fileName)));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(MM5IMDataInfo.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Close the data file created by previos step
     */
    public void closeDataFile() {
        try {
            _bw.close();
        } catch (IOException ex) {
            Logger.getLogger(MM5IMDataInfo.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Write data head
     *
     * @param dh The data head
     * @throws IOException
     */
    public void writeDataHead(DataHead dh) throws IOException {
        int skip = 4;
        //Record 1:
        _bw.writeInt(skip);
        _bw.writeInt(dh.iversion);
        _bw.writeInt(skip);

        //Record 2:
        skip = 124;
        _bw.writeInt(skip);
        _bw.writeBytes(GlobalUtil.padRight(dh.hdate, 24, ' '));
        _bw.writeFloat(dh.xfcst);
        _bw.writeBytes(GlobalUtil.padRight(dh.field, 9, ' '));
        _bw.writeBytes(GlobalUtil.padRight(dh.units, 25, ' '));
        _bw.writeBytes(GlobalUtil.padRight(dh.desc, 46, ' '));
        _bw.writeFloat(dh.level);
        _bw.writeInt(dh.idim);
        _bw.writeInt(dh.jdim);
        _bw.writeInt(dh.llflag);
        _bw.writeInt(skip);

        //Record 3:
        skip = 16;
        _bw.writeInt(skip);
        _bw.writeFloat(dh.startlat);
        _bw.writeFloat(dh.startlon);
        _bw.writeFloat(dh.deltalat);
        _bw.writeFloat(dh.deltalon);
        _bw.writeInt(skip);
    }

    // </editor-fold>
}