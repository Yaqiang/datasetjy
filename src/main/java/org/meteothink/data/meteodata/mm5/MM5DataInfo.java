/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.meteothink.data.meteodata.mm5;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.meteothink.data.meteodata.DataInfo;
import org.meteothink.ndarray.Dimension;
import org.meteothink.ndarray.DimensionType;
import org.meteothink.data.meteodata.MeteoDataType;
import org.meteothink.data.meteodata.Variable;
import org.meteothink.util.DataConvert;
import org.meteothink.util.DateUtil;
import org.meteothink.common.projection.KnownCoordinateSystems;
import org.meteothink.common.projection.ProjUtil;
import org.meteothink.ndarray.Array;
import org.meteothink.ndarray.DataType;
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
public class MM5DataInfo extends DataInfo {

    // <editor-fold desc="Variables">
    private ByteOrder _byteOrder = ByteOrder.BIG_ENDIAN;
    private BigHeader _bigHeader = new BigHeader();
    List<SubHeader> _subHeaders = new ArrayList<>();
    // </editor-fold>
    // <editor-fold desc="Constructor">

    /**
     * Constructor
     */
    public MM5DataInfo() {
        this.setDataType(MeteoDataType.MM5);
    }
    // </editor-fold>
    // <editor-fold desc="Get Set Methods">
    // </editor-fold>
    // <editor-fold desc="Methods">

    @Override
    public void readDataInfo(String fileName) {
        BigHeader bh = null;
        this.readDataInfo(fileName, bh);
    }

    /**
     * Read data info - the the data file has no big header
     *
     * @param fileName The data file name
     * @param bigHeaderFile The data file with BigHeader
     */
    public void readDataInfo(String fileName, String bigHeaderFile) {
        this.setFileName(fileName);
        try {
            RandomAccessFile br = new RandomAccessFile(bigHeaderFile, "r");
            //Read flag
            br.skipBytes(4);
            int flag = br.readInt();
            br.skipBytes(4);

            BigHeader bh = null;
            if (flag == 0) {    //Read big header
                bh = this.readBigHeader(br);
            }
            br.close();

            this.readDataInfo(fileName, bh);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(MM5DataInfo.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(MM5DataInfo.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Read data info - the the data file has no big header
     *
     * @param fileName The data file name
     * @param ebh Extra BigHeader
     */
    public void readDataInfo(String fileName, BigHeader ebh) {
        this.setFileName(fileName);
        try {
            RandomAccessFile br = new RandomAccessFile(fileName, "r");
            int flag;
            int xn = 0, yn = 0, zn = 0;
            //byte[] bytes;
            List<Variable> variables = new ArrayList<>();
            int tn = 0;
            Dimension xdim = new Dimension(DimensionType.X);
            xdim.setShortName("x");
            Dimension ydim = new Dimension(DimensionType.Y);
            ydim.setShortName("y");
            Dimension zdim = new Dimension(DimensionType.Z);
            zdim.setShortName("level");

            if (ebh != null) {
                this._bigHeader = ebh;
                xn = ebh.getXNum();
                yn = ebh.getYNum();
                zn = ebh.getZNum();
                float[] values = new float[zn];
                for (int i = 0; i < zn; i++) {
                    values[i] = i + 1;
                }
                zdim.setValues(values);
                String projStr = this.getCRS().getParameterString();
                int mapProj = ebh.getMapProj();
                switch (mapProj) {
                    case 1:
                        projStr = "+proj=lcc"
                                + " +lat_1=" + String.valueOf(ebh.getTrueLatSouth())
                                + " +lat_2=" + String.valueOf(ebh.getTrueLatNorth())
                                + " +lat_0=" + String.valueOf(ebh.getXLATC())
                                + " +lon_0=" + String.valueOf(ebh.getXLONC());
                        break;
                    case 2:
                        projStr = "+proj=stere"
                                + "+lat_0=" + String.valueOf(ebh.getXLATC())
                                + "+lon_0=" + String.valueOf(ebh.getXLONC());
                        break;
                    case 3:
                        projStr = "+proj=tmerc"
                                + "+lat_0=" + String.valueOf(ebh.getXLATC())
                                + "+lon_0=" + String.valueOf(ebh.getXLONC());
                        break;
                }
                this.setCRS(ProjUtil.factory(projStr));
                //Set X Y
                double[] X = new double[xn];
                double[] Y = new double[yn];
                float centeri = xn / 2.0f;
                float centerj = yn / 2.0f;
                getProjectedXY(this.getCRS(), ebh.getDeltaX(), centeri, centerj, ebh.getXLONC(),
                        ebh.getXLATC(), X, Y);
                xdim.setValues(X);
                ydim.setValues(Y);
                this.setXDimension(xdim);
                this.setYDimension(ydim);
                this.setZDimension(zdim);
                this.addDimension(xdim);
                this.addDimension(ydim);
                this.addDimension(zdim);
            }

            List<Date> times = new ArrayList<>();
            Date ct;
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
            //int shIdx = 0;
            while (true) {
                if (br.getFilePointer() >= br.length() - 100) {
                    break;
                }

                //Read flag
                br.skipBytes(4);
                flag = br.readInt();
                br.skipBytes(4);

                if (flag == 0) {    //Read big header
                    BigHeader bh = this.readBigHeader(br);
                    if (ebh == null) {
                        this._bigHeader = bh;
                        xn = bh.getXNum();
                        yn = bh.getYNum();
                        zn = bh.getZNum();
                        float[] values = new float[zn];
                        for (int i = 0; i < zn; i++) {
                            values[i] = i + 1;
                        }
                        zdim.setValues(values);
                        this.setZDimension(zdim);
                        String projStr = this.getCRS().getParameterString();
                        int mapProj = bh.getMapProj();
                        switch (mapProj) {
                            case 1:
                                projStr = "+proj=lcc"
                                        + " +lat_1=" + String.valueOf(bh.getTrueLatSouth())
                                        + " +lat_2=" + String.valueOf(bh.getTrueLatNorth())
                                        + " +lat_0=" + String.valueOf(bh.getXLATC())
                                        + " +lon_0=" + String.valueOf(bh.getXLONC());
                                break;
                            case 2:
                                projStr = "+proj=stere"
                                        + "+lat_0=" + String.valueOf(bh.getXLATC())
                                        + "+lon_0=" + String.valueOf(bh.getXLONC());
                                break;
                            case 3:
                                projStr = "+proj=tmerc"
                                        + "+lat_0=" + String.valueOf(bh.getXLATC())
                                        + "+lon_0=" + String.valueOf(bh.getXLONC());
                                break;
                        }
                        this.setCRS(ProjUtil.factory(projStr));
                        //Set X Y
                        double[] X = new double[xn];
                        double[] Y = new double[yn];
                        float centeri = xn / 2.0f;
                        float centerj = yn / 2.0f;
                        getProjectedXY(this.getCRS(), bh.getDeltaX(), centeri, centerj, bh.getXLONC(),
                                bh.getXLATC(), X, Y);
                        xdim.setValues(X);
                        ydim.setValues(Y);
                        this.setXDimension(xdim);
                        this.setYDimension(ydim);
                        this.addDimension(xdim);
                        this.addDimension(ydim);
                        this.addDimension(zdim);
                    }
                } else if (flag == 1) {    //Read sub header
                    long pos = br.getFilePointer();
                    SubHeader sh = this.readSubHeader(br);
                    sh.timeIndex = tn;
                    sh.position = pos;
                    sh.length = (int) (br.getFilePointer() - pos);
                    this._subHeaders.add(sh);
                    if (sh.ordering.equals("YXS") || sh.ordering.equals("YXP")) {
                        br.skipBytes(xn * yn * zn * 4 + 8);
                    } else if (sh.ordering.equals("YXW")) {
                        br.skipBytes(xn * yn * (zn + 1) * 4 + 8);
                    } else if (sh.ordering.equals("YX")) {
                        br.skipBytes(xn * yn * 4 + 8);
                    } else if (sh.ordering.equals("CA")) {
                        br.skipBytes(sh.end_index[0] * sh.end_index[1] * 4 + 8);
                    } else if (sh.ordering.equals("XSB")) {
                        br.skipBytes(yn * zn * 5 * 4 + 8);
                    } else if (sh.ordering.equals("YSB")) {
                        br.skipBytes(xn * zn * 5 * 4 + 8);
                    } else if (sh.ordering.equals("XWB")) {
                        br.skipBytes(yn * (zn + 1) * 5 * 4 + 8);
                    } else if (sh.ordering.equals("YWB")) {
                        br.skipBytes(xn * (zn + 1) * 5 * 4 + 8);
                    } else if (sh.ordering.equals("S")) {
                        br.skipBytes(zn * 4 + 8);
                    } else if (sh.ordering.equals("P")) {
                        br.skipBytes(zn * 4 + 8);
                    }
//                    if (shIdx == 0) {
//                        ct = format.parse(sh.current_date);
//                        times.add(ct);
//                    }
                    ct = format.parse(sh.current_date);
                    if (times.contains(ct)) {
                        sh.timeIndex = times.indexOf(ct);
                    } else {
                        times.add(ct);
                        sh.timeIndex = times.size() - 1;
                    }
                    //shIdx += 1;
                } else if (flag == 2) {
                    tn += 1;
                    //shIdx = 0;
                }
            }

            List<Double> values = new ArrayList<>();
            for (Date t : times) {
                values.add(DateUtil.toOADate(t));
            }
            Dimension tDim = new Dimension(DimensionType.T);
            tDim.setShortName("time");
            tDim.setValues(values);
            this.setTimeDimension(tDim);
            this.addDimension(tDim);
            
            //Set variables
            List<SubHeader> shs = new ArrayList<>();
            List<String> varNames = new ArrayList<>();
            boolean nameDup = false;
            for (SubHeader sh : this._subHeaders) {
                if (sh.timeIndex == 0) {
                    if (varNames.contains(sh.name)) {
                        sh.name = sh.name + String.valueOf(varNames.size());
                        nameDup = true;
                    }
                    varNames.add(sh.name);
                    shs.add(sh);
                }
            }
            if (nameDup) {
                for (int i = 1; i < times.size(); i++) {
                    varNames = new ArrayList<>();
                    for (SubHeader sh : this._subHeaders) {
                        if (sh.timeIndex == i) {
                            if (varNames.contains(sh.name)) {
                                sh.name = sh.name + String.valueOf(varNames.size());
                                nameDup = true;
                            }
                            varNames.add(sh.name);
                        }
                    }
                }
            }
            for (SubHeader sh : shs) {
                Variable var = new Variable();
                var.setName(sh.name);
                var.setDataType(DataType.FLOAT);
                //var.addLevel(dh.level);
                var.setUnits(sh.unit);
                var.setDescription(sh.description);

                 if (sh.ordering.equals("YXS") || sh.ordering.equals("YXP")
                        || sh.ordering.equals("YXW") || sh.ordering.equals("YX")) {
                    var.addDimension(xdim);
                    var.addDimension(0, ydim);
                }
                if (sh.ordering.equals("YXS") || sh.ordering.equals("YXP")
                        || sh.ordering.equals("YXW") || sh.ordering.equals("S")
                        || sh.ordering.equals("P")) {
                    var.addDimension(0, zdim);
                }
                variables.add(var);
            }

            for (Variable var : variables) {
                var.addDimension(0, tDim);
                //var.updateZDimension();
            }

            this.setVariables(variables);

            br.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(MM5DataInfo.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException | ParseException ex) {
            Logger.getLogger(MM5DataInfo.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Read big header
     *
     * @param br The randomAccessFile
     * @return The big header
     * @throws IOException
     */
    public BigHeader readBigHeader(RandomAccessFile br) throws IOException {
        return readBigHeader(br, true);
    }

    /**
     * Read big header
     *
     * @param br The randomAccessFile
     * @param isSequential If is sequential
     * @return The big header
     * @throws IOException
     */
    public BigHeader readBigHeader(RandomAccessFile br, boolean isSequential) throws IOException {
        BigHeader bh = new BigHeader();
        if (isSequential) {
            br.skipBytes(4);
        }
        byte[] bytes = new byte[80];
        int i, j;
        for (i = 0; i < 20; i++) {
            for (j = 0; j < 50; j++) {
                bh.bhi[j][i] = br.readInt();
            }
        }
        for (i = 0; i < 20; i++) {
            for (j = 0; j < 20; j++) {
                bh.bhr[j][i] = br.readFloat();
            }
        }
        for (i = 0; i < 20; i++) {
            for (j = 0; j < 50; j++) {
                br.read(bytes);
                bh.bhic[j][i] = new String(bytes).trim();
            }
        }
        for (i = 0; i < 20; i++) {
            for (j = 0; j < 20; j++) {
                br.read(bytes);
                bh.bhrc[j][i] = new String(bytes).trim();
            }
        }

        if (isSequential) {
            br.skipBytes(4);
        }

        return bh;
    }

//    /**
//     * Write big header file
//     * @param bh The big header
//     * @param fileName The file name
//     */
//    public void writeBigHeaderFile(BigHeader bh, String fileName) throws FileNotFoundException, IOException{
//        DataOutputStream dos = new DataOutputStream(new FileOutputStream(new File(fileName)));
//        byte[] bytes = new byte[4];
//        dos.write(bytes);
//        int i, j;
//        for (i = 0; i < 20; i++) {
//            for (j = 0; j < 50; j++) {
//                dos.writeInt(bh.bhi[j][i]);
//            }
//        }
//        for (i = 0; i < 20; i++) {
//            for (j = 0; j < 20; j++) {
//                dos.writeFloat(bh.bhr[j][i]);
//            }
//        }
//        for (i = 0; i < 20; i++) {
//            for (j = 0; j < 50; j++) {
//                dos.write(bh.bhic[j][i].getBytes());
//            }
//        }
//        for (i = 0; i < 20; i++) {
//            for (j = 0; j < 20; j++) {
//                dos.write(bh.bhrc[j][i].getBytes());
//            }
//        }
//        dos.write(bytes);
//        dos.close();
//    }
    /**
     * Read sub header
     *
     * @param br The randomAccessFile
     * @return The sub header
     * @throws IOException
     */
    public SubHeader readSubHeader(RandomAccessFile br) throws IOException {
        return readSubHeader(br, true);
    }

    /**
     * Read sub header
     *
     * @param br The randomAccessFile
     * @param isSequential If if sequential
     * @return The sub header
     * @throws IOException
     */
    public SubHeader readSubHeader(RandomAccessFile br, boolean isSequential) throws IOException {
        SubHeader sh = new SubHeader();
        byte[] bytes = new byte[4];
        int i;
        if (isSequential) {
            br.skipBytes(4);
        }

        sh.ndim = br.readInt();
        for (i = 0; i < 4; i++) {
            sh.start_index[i] = br.readInt();
        }
        for (i = 0; i < 4; i++) {
            sh.end_index[i] = br.readInt();
        }
        sh.xtime = br.readFloat();
        br.read(bytes);
        sh.staggering = new String(bytes).trim();
        br.read(bytes);
        sh.ordering = new String(bytes).trim();
        bytes = new byte[24];
        br.read(bytes);
        sh.current_date = new String(bytes).trim();
        bytes = new byte[9];
        br.read(bytes);
        sh.name = new String(bytes).trim();
        bytes = new byte[25];
        br.read(bytes);
        sh.unit = new String(bytes).trim();
        bytes = new byte[46];
        br.read(bytes);
        sh.description = new String(bytes).trim();

        if (isSequential) {
            br.skipBytes(4);
        }

        return sh;
    }

    private void getProjectedXY(CoordinateReferenceSystem projInfo, float size,
            float sync_XP, float sync_YP, float sync_Lon, float sync_Lat,
            double[] X, double[] Y) {
        //Get sync X/Y
        CoordinateReferenceSystem fromProj = KnownCoordinateSystems.geographic.world.WGS1984;
        double sync_X, sync_Y;
        double[][] points = new double[1][];
        points[0] = new double[]{sync_Lon, sync_Lat};
        ProjUtil.reprojectPoints(points, fromProj, projInfo, 0, 1);
        sync_X = points[0][0];
        sync_Y = points[0][1];

        //Get integer sync X/Y            
        int i_XP, i_YP;
        double i_X, i_Y;
        i_XP = (int) sync_XP;
        if (sync_XP == i_XP) {
            i_X = sync_X;
        } else {
            i_X = sync_X - (sync_XP - i_XP) * size;
        }
        i_YP = (int) sync_YP;
        if (sync_YP == i_YP) {
            i_Y = sync_Y;
        } else {
            i_Y = sync_Y - (sync_YP - i_YP) * size;
        }

        //Get left bottom X/Y
        int nx, ny;
        nx = X.length;
        ny = Y.length;
        double xlb, ylb;
        xlb = i_X - (i_XP - 1) * size;
        ylb = i_Y - (i_YP - 1) * size;

        //Get X Y with orient 0
        int i;
        for (i = 0; i < nx; i++) {
            X[i] = xlb + i * size;
        }
        for (i = 0; i < ny; i++) {
            Y[i] = ylb + i * size;
        }
    }

    private SubHeader findSubHeader(String varName, int tIdx) {
        for (SubHeader sh : this._subHeaders) {
            if (sh.timeIndex == tIdx && sh.name.equals(varName)) {
                return sh;
            }
        }

        return this._subHeaders.get(0);
    }

    /**
     * Get global attributes
     *
     * @return Global attributes
     */
    @Override
    public List<Attribute> getGlobalAttributes() {
        return new ArrayList<>();
    }

//    @Override
//    public String generateInfoText() {
//        String dataInfo;
//        int i, j;
//        Attribute aAttS;
//        dataInfo = "File Name: " + this.getFileName();
//        dataInfo += System.getProperty("line.separator") + "Dimensions: " + this.getDimensions().size();
//        for (i = 0; i < this.getDimensions().size(); i++) {
//            dataInfo += System.getProperty("line.separator") + "\t" + this.getDimensions().get(i).getShortName()+ " = "
//                    + String.valueOf(this.getDimensions().get(i).getLength()) + ";";
//        }
//        
//        Dimension tdim = this.getTimeDimension();
//        if (tdim != null) {
//            dataInfo += System.getProperty("line.separator") + "T Dimension: Tmin = " + String.valueOf(tdim.getMinValue())
//                    + "; Tmax = " + String.valueOf(tdim.getMaxValue()) + "; Tsize = "
//                    + String.valueOf(tdim.getLength()) + "; Tdelta = " + String.valueOf(tdim.getDeltaValue());
//        }
//
//        Dimension zdim = this.getZDimension();
//        if (zdim != null) {
//            dataInfo += System.getProperty("line.separator") + "Z Dimension: Zmin = " + String.valueOf(zdim.getMinValue())
//                    + "; Zmax = " + String.valueOf(zdim.getMaxValue()) + "; Zsize = "
//                    + String.valueOf(zdim.getLength()) + "; Zdelta = " + String.valueOf(zdim.getDeltaValue());
//        }
//        
//        Dimension xdim = this.getXDimension();
//        if (xdim != null) {
//            dataInfo += System.getProperty("line.separator") + "X Dimension: Xmin = " + String.valueOf(xdim.getMinValue())
//                    + "; Xmax = " + String.valueOf(xdim.getMaxValue()) + "; Xsize = "
//                    + String.valueOf(xdim.getLength()) + "; Xdelta = " + String.valueOf(xdim.getDeltaValue());
//        }
//        Dimension ydim = this.getYDimension();
//        if (ydim != null) {
//            dataInfo += System.getProperty("line.separator") + "Y Dimension: Ymin = " + String.valueOf(ydim.getMinValue())
//                    + "; Ymax = " + String.valueOf(ydim.getMaxValue()) + "; Ysize = "
//                    + String.valueOf(ydim.getLength()) + "; Ydelta = " + String.valueOf(ydim.getDeltaValue());
//        }
//
//        dataInfo += System.getProperty("line.separator") + "Global Attributes: ";
//        dataInfo += System.getProperty("line.separator") + "\t: " + "Data type: MM5 output";
//
//        dataInfo += System.getProperty("line.separator") + "Variations: " + this.getVariables().size();
//        for (i = 0; i < this.getVariables().size(); i++) {
//            dataInfo += System.getProperty("line.separator") + "\t" + this.getVariables().get(i).getName()+ "(";
//            List<org.meteothink.data.meteodata.Dimension> dims = this.getVariables().get(i).getDimensions();
//            for (j = 0; j < dims.size(); j++) {
//                dataInfo += dims.get(j).getShortName()+ ",";
//            }
//            dataInfo = dataInfo.substring(0, dataInfo.length() - 1);
//            dataInfo += ");";
//            List<Attribute> atts = this.getVariables().get(i).getAttributes();
//            for (j = 0; j < atts.size(); j++) {
//                aAttS = atts.get(j);
//                dataInfo += System.getProperty("line.separator") + "\t" + "\t" + this.getVariables().get(i).getName()
//                        + ": " + aAttS.toString();
//            }
//        }
//
//        for (Dimension dim : this.getDimensions()) {
//            if (dim.isUnlimited()) {
//                dataInfo += System.getProperty("line.separator") + "Unlimited dimension: " + dim.getShortName();
//            }
//            break;
//        }
//
//        return dataInfo;
//    }
    public String generateInfoText_bak() {
        String dataInfo;
        dataInfo = "File Name: " + this.getFileName();
        int i, j;
        for (i = 0; i < 50; i++) {
            for (j = 0; j < 20; j++) {
                dataInfo += System.getProperty("line.separator") + String.format("[%d][%d]", i + 1, j + 1) + " "
                        + this._bigHeader.bhic[i][j] + ": " + String.valueOf(this._bigHeader.bhi[i][j]);
            }
        }
        for (i = 0; i < 20; i++) {
            for (j = 0; j < 20; j++) {
                dataInfo += System.getProperty("line.separator") + String.format("[%d][%d]", i + 1, j + 1) + " "
                        + this._bigHeader.bhrc[i][j] + ": " + String.valueOf(this._bigHeader.bhr[i][j]);
            }
        }
//        dataInfo += System.getProperty("line.separator") + "Xsize = " + String.valueOf(this.getXDimension().getDimLength())
//                + "  Ysize = " + String.valueOf(this.getYDimension().getDimLength());               
//        dataInfo += System.getProperty("line.separator") + "Number of Variables = " + String.valueOf(this.getVariableNum());
//        for (String v : this.getVariableNames()) {
//            dataInfo += System.getProperty("line.separator") + v;
//        }

        return dataInfo;
    }

    /**
     * Read array data of a variable
     *
     * @param varName Variable name
     * @return Array data
     */
    @Override
    public DimArray read(String varName) {
        Variable var = this.getVariable(varName);
        int n = var.getDimNumber();
        int[] origin = new int[n];
        int[] size = new int[n];
        int[] stride = new int[n];
        for (int i = 0; i < n; i++) {
            origin[i] = 0;
            size[i] = var.getDimLength(i);
            stride[i] = 1;
        }

        DimArray r = read(varName, origin, size, stride);

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
    public DimArray read(String varName, int[] origin, int[] size, int[] stride) {
        try {
            Variable var = this.getVariable(varName);
            Section section = new Section(origin, size, stride);
            Array dataArray = Array.factory(DataType.FLOAT, section.getShape());
            int rangeIdx = 0;
            Range timeRange = var.getTDimension() != null ? section
                    .getRange(rangeIdx++)
                    : new Range(0, 0);

            Range levRange = var.getLevelNum() > 0 ? section
                    .getRange(rangeIdx++)
                    : new Range(0, 0);

            Range yRange = var.getYDimension() != null ? 
                    section.getRange(rangeIdx++)
                    : new Range(0,0);
            Range xRange = var.getXDimension() != null ?
                    section.getRange(rangeIdx)
                    : new Range(0, 0);

            IndexIterator ii = dataArray.getIndexIterator();

            for (int timeIdx = timeRange.first(); timeIdx <= timeRange.last();
                    timeIdx += timeRange.stride()) {
                int levelIdx = levRange.first();

                for (; levelIdx <= levRange.last();
                        levelIdx += levRange.stride()) {
                    readXY(varName, timeIdx, levelIdx, yRange, xRange, ii);
                }
            }

            return new DimArray(dataArray.reduce(), var.getDimensions(section));
        } catch (InvalidRangeException ex) {
            Logger.getLogger(MM5DataInfo.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    private void readXY(String varName, int timeIdx, int levelIdx, Range yRange, Range xRange, IndexIterator ii) {
        try {
            int varIdx = this.getVariableNames().indexOf(varName);
            RandomAccessFile br = new RandomAccessFile(this.getFileName(), "r");
            Variable var = this.getVariables().get(varIdx);
            Dimension xdim = var.getXDimension();
            Dimension ydim = var.getYDimension();
            int xn = xdim != null ? xdim.getLength() : 1;
            int yn = ydim != null ? ydim.getLength() : 1;
            SubHeader sh = this.findSubHeader(var.getName(), timeIdx);
            br.seek(sh.position + sh.length);
            int n = xn * yn;
            br.skipBytes(4);
            br.skipBytes(n * 4 * levelIdx);
            byte[] dataBytes = new byte[n * 4];
            br.read(dataBytes);
            br.close();

            int i, j;
            float[] data = new float[n];
            int start = 0;
            byte[] bytes = new byte[4];
            for (i = 0; i < xn; i++) {
                for (j = 0; j < yn; j++) {
                    System.arraycopy(dataBytes, start, bytes, 0, 4);
                    data[j * xn + i] = DataConvert.bytes2Float(bytes, _byteOrder);
                    start += 4;
                }
            }

            br.close();
            for (int y = yRange.first(); y <= yRange.last();
                    y += yRange.stride()) {
                for (int x = xRange.first(); x <= xRange.last();
                        x += xRange.stride()) {
                    int index = y * xn + x;
                    ii.setFloatNext(data[index]);
                }
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(MM5DataInfo.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(MM5DataInfo.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Add big header from a given file to a new file
     * @param fileName The given file without big header
     * @param newFileName The new file added big header
     * @param refFileName The referece file with big header
     * @throws FileNotFoundException
     * @throws IOException 
     */
    public static void addBigHeader(String fileName, String newFileName, String refFileName) 
            throws FileNotFoundException, IOException {
        DataInputStream dis = new DataInputStream(new FileInputStream(fileName));
        DataInputStream rdis = new DataInputStream(new FileInputStream(refFileName));
        DataOutputStream dos = new DataOutputStream(new FileOutputStream(newFileName));

         //write flag 0
        dos.writeInt(4);
        dos.writeInt(0);
        dos.writeInt(4);

         //write big header
        int n = 117600;
        byte[] bytes = new byte[n];
        dos.writeInt(n);
        rdis.read(new byte[12]);
        rdis.readInt();
        rdis.read(bytes);
        dos.write(bytes);
        dos.writeInt(n);

         //Write data
        bytes = new byte[32 * 1024];
        int numBytes;
        while((numBytes = dis.read(bytes)) != -1) {
            dos.write(bytes, 0, numBytes);
        }

         //close
        dis.close();
        rdis.close();
        dos.close();
    }
    
    // </editor-fold>       
}
