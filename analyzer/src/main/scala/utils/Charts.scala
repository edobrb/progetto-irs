package utils

import org.knowm.xchart.XYSeries.XYSeriesRenderStyle
import org.knowm.xchart.style.BoxStyler.BoxplotCalCulationMethod
import org.knowm.xchart.style.{BoxStyler, XYStyler}
import org.knowm.xchart.{BoxChart, BoxChartBuilder, XYChart, XYChartBuilder}

import java.awt.{Color, Font}

object Charts {

  def linePlot(title: String,
               xName: String,
               yName: String,
               series: Iterable[(String, Option[Color], Iterable[(Double, Double)])],
               applyCustomStyle: XYStyler => () = _ => (),
               applyCustomBuild: XYChartBuilder => () = _ => ()): XYChart = {
    val builder = new XYChartBuilder().xAxisTitle(xName).yAxisTitle(yName).title(title)
      .width(1600).height(900)
    applyCustomBuild(builder)
    val chart = builder.build()
    val styler = chart.getStyler
    styler.setLegendFont(new Font(Font.MONOSPACED, Font.PLAIN, 22))
    styler.setAxisTitleFont(new Font(Font.SERIF, Font.PLAIN, 22))
    styler.setChartTitleFont(new Font(Font.SERIF, Font.PLAIN, 30))
    styler.setAxisTickLabelsFont(new Font(Font.SERIF, Font.PLAIN, 22))
    applyCustomStyle(styler)
    series.foreach {
      case (name, color, s) =>
        val chartSeries = chart.addSeries(name, s.map(_._1).toArray, s.map(_._2).toArray)
        color.foreach(chartSeries.setMarkerColor)
    }
    chart
  }

  def scatterPlot(title: String,
                  xName: String,
                  yName: String,
                  series: Iterable[(String, Option[Color], Iterable[(Double, Double)])],
                  applyCustomStyle: XYStyler => () = _ => (),
                  applyCustomBuild: XYChartBuilder => () = _ => ()): XYChart = {
    linePlot(title, xName, yName, series, styler => {
      styler.setDefaultSeriesRenderStyle(XYSeriesRenderStyle.Scatter)
      applyCustomStyle(styler)
    }, applyCustomBuild)
  }

  def boxplot(title: String,
              xName: String,
              yName: String,
              series: Iterable[(String, Iterable[Double])],
              applyCustomStyle: BoxStyler => () = _ => (),
              applyCustomBuild: BoxChartBuilder => () = _ => ()): BoxChart = {
    val builder = new BoxChartBuilder().xAxisTitle(xName).yAxisTitle(yName)
      .title(title).width(1600).height(900)
    applyCustomBuild(builder)
    val chart = builder.build()
    val styler = chart.getStyler
    styler.setBoxplotCalCulationMethod(BoxplotCalCulationMethod.NP)
    styler.setToolTipsEnabled(true)
    styler.setPlotContentSize(0.98)
    styler.setLegendFont(new Font(Font.MONOSPACED, Font.PLAIN, 28))
    styler.setAxisTitleFont(new Font(Font.SERIF, Font.PLAIN, 22))
    styler.setChartTitleFont(new Font(Font.SERIF, Font.PLAIN, 30))
    styler.setAxisTickLabelsFont(new Font(Font.SERIF, Font.PLAIN, 22))
    styler.setXAxisLabelRotation(8)
    applyCustomStyle(styler)
    series.foreach {
      case (name, s) => chart.addSeries(name, s.toArray)
    }
    chart
  }
}
