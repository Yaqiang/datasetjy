/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.meteothink.data.meteodata.netcdf;

import java.util.ArrayList;
import java.util.List;
import org.meteothink.data.meteodata.Attribute;
import org.meteothink.data.meteodata.Variable;
import org.meteothink.ndarray.Array;
import org.meteothink.ndarray.DataType;
import org.meteothink.ndarray.Dimension;

/**
 *
 * @author Yaqiang Wang
 */
public class NCUtil {
    
    /**
     * Convert netcdf data type to meteothink data type
     * @param ncDataType Netcdf data type
     * @return MeteoThink data type
     */
    public static DataType convertDataType(ucar.ma2.DataType ncDataType) {
        DataType dataType = DataType.getType(ncDataType.toString());
        
        return dataType;
    }
    
    /**
     * Convert meteothink data type to netcdf data type
     * @param dataType MeteoThink data type
     * @return Netcdf data type
     */
    public static ucar.ma2.DataType convertDataType(DataType dataType) {
        ucar.ma2.DataType ncDataType = ucar.ma2.DataType.valueOf(dataType.toString());
        
        return ncDataType;
    }
    
    /**
     * Convert netcdf array to meteothink array
     * @param ncArray Netcdf array
     * @return MeteoThink array
     */
    public static Array convertArray(ucar.ma2.Array ncArray) {
        DataType dt = convertDataType(ncArray.getDataType());
        if (dt == DataType.OBJECT && ncArray.getObject(0).getClass() == String.class){
            dt = DataType.STRING;
        }
        Array array = Array.factory(dt, ncArray.getShape(), ncArray.getStorage());
        
        return array;
    }
    
    /**
     * Convert meteothink array to netcdf array
     * @param array MeteoThink array
     * @return Netcdf array
     */
    public static ucar.ma2.Array convertArray(Array array) {
        ucar.ma2.Array ncArray = ucar.ma2.Array.factory(convertDataType(array.getDataType()), array.getShape(), array.getStorage());
        
        return ncArray;
    }
    
    /**
     * Convert from netcdf dimension to meteothink dimension
     * @param ncDim Netcdf dimension
     * @return MeteoThink dimension
     */
    public static Dimension convertDimension(ucar.nc2.Dimension ncDim) {
        Dimension dim = new Dimension();
        dim.setShortName(ncDim.getShortName());
        dim.setLength(ncDim.getLength());
        dim.setUnlimited(ncDim.isUnlimited());
        dim.setShared(ncDim.isShared());
        dim.setVariableLength(ncDim.isVariableLength());
        
        return dim;
    }
    
    /**
     * Convert netcdf dimensions to meteothink dimensions
     * @param ncDims Netcdf dimensions
     * @return MeteoThink dimensions
     */
    public static List<Dimension> convertDimensions(List<ucar.nc2.Dimension> ncDims) {
        List<Dimension> dims = new ArrayList<>();
        for (ucar.nc2.Dimension ncDim : ncDims) {
            dims.add(convertDimension(ncDim));
        }
        
        return dims;
    }
    
    /**
     * Convert netcdf attribute to meteothink attribute
     * @param ncAttr Netcdf attribute
     * @return MeteoThink attribute
     */
    public static Attribute convertAttribute(ucar.nc2.Attribute ncAttr) {
        Attribute attr = new Attribute(ncAttr.getShortName());
        attr.setStringValue(ncAttr.getStringValue());
        attr.setValues(convertArray(ncAttr.getValues()));
        
        return attr;
    }
    
    /**
     * Convert netcdf variable to meteothink variable
     * @param ncVar Netcdf variable
     * @return MeteoThink variable
     */
    public static Variable convertVariable(ucar.nc2.Variable ncVar) {
        Variable var = new Variable();
        var.setName(ncVar.getShortName());
        var.setDataType(convertDataType(ncVar.getDataType()));
        var.setDescription(ncVar.getDescription());
        var.setDimensions(convertDimensions(ncVar.getDimensions()));
        for (ucar.nc2.Attribute ncAttr : ncVar.getAttributes()) {
            var.addAttribute(convertAttribute(ncAttr));
        }
        var.setUnits(ncVar.getUnitsString());
        
        return var;
    }
}
