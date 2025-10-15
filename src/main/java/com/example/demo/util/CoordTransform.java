package com.example.demo.util;

/**
 * Coordinate transforms between WGS-84 and GCJ-02 (Mars) system used in mainland China.
 * The formulas are widely known and used by many open-source projects.
 */
public class CoordTransform {
    private static final double PI = Math.PI;
    private static final double AXIS = 6378245.0;
    private static final double EE = 0.00669342162296594323;

    private static boolean outOfChina(double lat, double lon) {
        return lon < 72.004 || lon > 137.8347 || lat < 0.8293 || lat > 55.8271;
    }

    private static double transformLat(double x, double y) {
        double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(y * PI) + 40.0 * Math.sin(y / 3.0 * PI)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(y / 12.0 * PI) + 320 * Math.sin(y * PI / 30.0)) * 2.0 / 3.0;
        return ret;
    }

    private static double transformLon(double x, double y) {
        double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(x * PI) + 40.0 * Math.sin(x / 3.0 * PI)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(x / 12.0 * PI) + 300.0 * Math.sin(x / 30.0 * PI)) * 2.0 / 3.0;
        return ret;
    }

    public static double[] wgsToGcj(double lat, double lon) {
        if (outOfChina(lat, lon)) return new double[]{lat, lon};
        double dLat = transformLat(lon - 105.0, lat - 35.0);
        double dLon = transformLon(lon - 105.0, lat - 35.0);
        double radLat = lat / 180.0 * PI;
        double magic = Math.sin(radLat);
        magic = 1.0 - EE * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((AXIS * (1.0 - EE)) / (magic * sqrtMagic) * PI);
        dLon = (dLon * 180.0) / (AXIS / sqrtMagic * Math.cos(radLat) * PI);
        double mgLat = lat + dLat;
        double mgLon = lon + dLon;
        return new double[]{mgLat, mgLon};
    }

    /**
     * GCJ-02 转 WGS-84（近似反算一次迭代）。
     * <p>
     * 在国内坐标系反算中，进行一次迭代即可满足多数展示需求，
     * 如需更高精度可进行多次迭代或采用更精确算法。
     * </p>
     *
     * @param lat GCJ-02 纬度
     * @param lon GCJ-02 经度
     * @return WGS-84 坐标数组 [lat, lon]
     */
    public static double[] gcjToWgs(double lat, double lon) {
        if (outOfChina(lat, lon)) return new double[]{lat, lon};
        double[] enc = wgsToGcj(lat, lon);
        double dLat = enc[0] - lat;
        double dLon = enc[1] - lon;
        return new double[]{lat - dLat, lon - dLon};
    }
}