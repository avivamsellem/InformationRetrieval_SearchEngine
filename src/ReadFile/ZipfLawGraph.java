//package ReadFile;
//
//import org.jfree.chart.ChartFactory;
//import org.jfree.chart.ChartPanel;
//import org.jfree.chart.JFreeChart;
//import org.jfree.chart.axis.ValueAxis;
//import org.jfree.chart.plot.PlotOrientation;
//import org.jfree.chart.plot.XYPlot;
//import org.jfree.chart.ui.ApplicationFrame;
//import org.jfree.data.xy.XYSeries;
//import org.jfree.data.xy.XYSeriesCollection;
//
//
//public class ZipfLawGraph extends ApplicationFrame {
//
//    /**
//     * A demonstration application showing an XY series containing a null value.
//     *
//     * @param yValues double[]. all data Point.
//     */
//    public ZipfLawGraph(double[] yValues) {
//
//        super("Zipf Law's");
//        final XYSeries series = new XYSeries("Zipf curve");
//
//        int index = 1;
//
//        for (double value : yValues) {
//
//            series.add(index, value);
//
//            index++;
//        }
//
//        final XYSeriesCollection data = new XYSeriesCollection(series);
//        final JFreeChart chart = ChartFactory.createXYLineChart(
//                "Terms frequency in corpus Plot",
//                "Terms",
//                "Total Occurrences",
//                data,
//                PlotOrientation.VERTICAL,
//                true,
//                true,
//                false
//        );
//
//        XYPlot xyPlot = chart.getXYPlot();
//        ValueAxis domainAxis = xyPlot.getDomainAxis();
//
//        domainAxis.setRange(0.0, 1000.0);
////        rangeAxis.setRange(0.0, 1.0);
//
//        final ChartPanel chartPanel = new ChartPanel(chart);
//        chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
//        setContentPane(chartPanel);
//
//        }
//
//// ****************************************************************************
//// * JFREECHART DEVELOPER GUIDE                                               *
//// * The JFreeChart Developer Guide, written by David Gilbert, is available   *
//// * to purchase from Object Refinery Limited:                                *
//// *                                                                          *
//// * http://www.object-refinery.com/jfreechart/guide.html                     *
//// *                                                                          *
//// * Sales are used to provide funding for the JFreeChart project - please    *
//// * support us so that we can continue developing free software.             *
//// ****************************************************************************
//
//    /**
//     * Starting point for the demonstration application.
//     *
//     * @param args  ignored.
//     */
//    public static void main(final String[] args) {
//
//        final ZipfLawGraph demo = new ZipfLawGraph(new double[1]);
//        demo.pack();
////            RefineryUtilities.centerFrameOnScreen(demo);
//        demo.setVisible(true);
//
//    }
//
//}
