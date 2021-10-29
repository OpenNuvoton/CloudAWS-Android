package com.nuvoton.cloudaws

import android.content.Context
import android.graphics.Color
import android.view.View
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import java.util.*
import kotlin.collections.ArrayList
import com.github.mikephil.charting.components.Legend
import android.app.Activity
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.DefaultValueFormatter
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import java.text.SimpleDateFormat


class MPChartManager(lc:LineChart,activity:Activity) {

    class Point() {
        public var name: String? = null
        public var value: Float? = null
    }

    private var _Activity: Activity? = null

    private var _Chartview: LineChart? = null
    private val _LineDataSetArray = ArrayList<LineDataSet>()
    private val _LastPonit = Point()

    /**
     * LineChart = findViewById<View>(R.id.MPChart) as LineChart
     */
    init {

        _Activity = activity
        _Chartview = lc

        val chartData = LineData()
        _Chartview!!.data = chartData
        _Chartview!!.getLegend().textSize = 16f
        _Chartview!!.getLegend().form = Legend.LegendForm.CIRCLE
        _Chartview!!.getLegend().setWordWrapEnabled(true);
        _Chartview!!.xAxis.isEnabled = false
//        _Chartview!!.xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
//        _Chartview!!.xAxis?.valueFormatter = MPChartCustomFormatter()
        _Chartview!!.getDescription().text = "Ver:1.0.0"
        _Chartview!!.getDescription().textSize = 16f
        _Chartview!!.getDescription().textColor = Color.RED

    }

    fun getLastValue(): Point {
        return _LastPonit
    }

    fun getLineLabelNameArray(): ArrayList<String> {

        val lineLabelNameArray = ArrayList<String>()
        for (lds in _LineDataSetArray) {
            lineLabelNameArray.add(lds.label)
        }

        return lineLabelNameArray
    }

    fun setDisplayLine(labelName: String) {

        _Activity!!.runOnUiThread {

            if (labelName == "ALL") {
                for (lds in _LineDataSetArray) {
                    lds.isVisible = true
                }

                return@runOnUiThread
            }

            for (lds in _LineDataSetArray) {
                lds.isVisible = lds.label.equals(labelName)
            }
        }
        _Chartview?.invalidate()
        _Chartview!!.notifyDataSetChanged()
    }

    fun addChartDataSet(name: String) {

        for (lds in _LineDataSetArray) {
            if (lds.label.equals(name)) {
                return
            }
        }

        val rnd = Random()
        val color: Int = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256))

        val lds = LineDataSet(arrayListOf<Entry>(), name)
        lds.setCircleColor(color)
        lds.color = color
        lds.valueTextColor = color
        lds.lineWidth = 3f
        lds.valueTextSize = 10f
        lds.valueFormatter = DefaultValueFormatter(2)
//        lds.setDefaultValueFormatter(new DefaultValueFormatter(digits = 1))

        _LineDataSetArray.add(lds)
        _Chartview?.data?.addDataSet(lds)
        _Chartview?.invalidate()
    }

    fun addEntry(name: String, value: Float) {

        if (_Chartview == null) return

        _LastPonit.name = name
        _LastPonit.value = value

        val data: LineData = _Chartview!!.getData()
        val entry = Entry(data.entryCount.toFloat(), value)

        val dls = data.getDataSetByLabel(name, false) //根據name找DataSet
        val index = data.getIndexOfDataSet(dls)//根據DataSet找Index（哪一條線）
        data.addEntry(entry, index)

        val moveToX = dls.entryCount.toFloat() - 1

        // 像ListView那样的通知数据更新
        _Chartview!!.notifyDataSetChanged()

        val isAllMode = true
        if (isAllMode == true) {
            // 当前统计图表中最多在x轴坐标线上显示的总量
            _Chartview!!.setVisibleXRangeMaximum(20f)
            // 将坐标移动到最新
            // 此代码将刷新图表的绘图
            _Chartview!!.moveViewToX((data.entryCount - 20).toFloat())

        } else {
            //恢復重第一個點開始的完整圖表
            _Chartview!!.setVisibleXRangeMaximum(data.entryCount.toFloat())
            _Chartview?.moveViewToX(moveToX)
            _Chartview?.zoom(0f, 1f, 0f, 0f)

        }
    }
}

