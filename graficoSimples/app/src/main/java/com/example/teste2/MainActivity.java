package com.example.teste2;

import androidx.appcompat.app.AppCompatActivity;
import android.app.Activity;
import android.graphics.*;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.androidplot.ui.HorizontalPositioning;
import com.androidplot.ui.VerticalPositioning;
import com.androidplot.util.PixelUtils;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYSeries;
import com.androidplot.xy.*;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    // grafico 
    private XYPlot plot;
    Number[] series1Numbers;
    XYSeries series1;
    LineAndPointFormatter series1Format;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // abre a tela
        super.onCreate(savedInstanceState);
        setContentView(R.layout.simple_xy_plot_example);

        Toast.makeText(this, "aqui", Toast.LENGTH_SHORT).show();

        // inicia o grafico como o elemento xml
        plot = (XYPlot) findViewById(R.id.plot);

        // cria o array de valores pra botar no xy do grafico
        final Number[] domainLabels = {10, 20, 30, 40, 50, 60, 70, 80, 90, 100};
        series1Numbers = new Number[]{10, 25, 20, 35, 40, 65, 50, 80, 75, 85};
        Number[] series2Numbers = {5, 2, 10, 5, 20, 10, 40, 20, 80, 40};

        // cria uma "serie" (linha) pra botar no grafico
        series1 = new SimpleXYSeries(
            Arrays.asList(series1Numbers), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "Series1");

        XYSeries series2 = new SimpleXYSeries(
                Arrays.asList(series2Numbers), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "Series2");

        // formata a aparecia da linha
        series1Format =
                        new LineAndPointFormatter(Color.RED, Color.GREEN, null, null);
        LineAndPointFormatter series2Format =
                        new LineAndPointFormatter(Color.BLUE, Color.YELLOW, null, null);

        // bota efeito de "dash" na linha
        //series1Format.getLinePaint().setPathEffect(new DashPathEffect(new float[] {

        //        PixelUtils.dpToPix(20),
        //        PixelUtils.dpToPix(15)}, 0));

        // suaviza a linha
        series1Format.setInterpolationParams(
        new CatmullRomInterpolator.Params(10, CatmullRomInterpolator.Type.Centripetal));

        // adiciona a linha no grafico
        plot.addSeries(series1, series1Format);
        plot.addSeries(series2, series2Format);

        // plota o grafico
        plot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.TOP).setFormat(new Format() {
            @Override
            public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
                int i = Math.round(((Number) obj).floatValue());
                return toAppendTo.append(domainLabels[i]);
            }
            @Override
            public Object parseObject(String source, ParsePosition pos) {
                return null;
            }
        });

        new th().start();

        hideSystemBars();
    }

    // esconde as barras de navegação e barra de status
    protected void hideSystemBars() {
        //deixa app em full screen
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY // define q mesmo se encostar na tela continua hide
                | View.SYSTEM_UI_FLAG_FULLSCREEN // esconde status bar
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // esconde statusbar
        );
    }

    public class th extends Thread{// função q vai executar em segundo plano
        public void run() {
            while(1<2){
                try {
                    sleep(400);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                plot.removeSeries(series1);

                plot.redraw();

                try {
                    sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                series1Format.setInterpolationParams(
                        new CatmullRomInterpolator.Params(10, CatmullRomInterpolator.Type.Centripetal));
                plot.addSeries(series1, series1Format);

                plot.redraw();
            }
        }
    }

}