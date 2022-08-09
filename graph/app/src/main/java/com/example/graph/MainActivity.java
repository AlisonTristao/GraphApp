package com.example.graph;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.CatmullRomInterpolator;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PanZoom;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.StepMode;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    public Boolean rola = false; // salva se o grafico esta rolando ou nao
    public Button btnRolar; // botao para o grafico acompanha a linha
    public Button btnBT; // botao para abrir o bt
    public XYPlot grafico; // grafico
    public XYSeries linha; // linha
    public Number[] valores = {0}; // valores do grafico

    @SuppressLint("WrongViewCast")
    public void iniciaComponentes(){
        // inicia os componenetes como elemento do xml
        grafico = findViewById(R.id.grafico);
        btnBT = findViewById(R.id.btnBT);
        btnRolar = findViewById(R.id.btnRolar);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // tela q vai abrir
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // esconde as bars
        hideSystemBars();

        //inicia os componentes
        iniciaComponentes();

        // cria o grafico
        criaGrafico();

        // plota o grafico
        plotaGraf(grafico);

        // -------------- botes ---------------//
        // bluetooth
        btnBT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        // rolagem
        btnRolar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rola = !rola; // recebe o contrario dela
            }
        });

        new th().start();
    }

    protected void hideSystemBars() {
        //deixa app em full screen
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
          View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY // define q mesmo se encostar na tela continua hide
        | View.SYSTEM_UI_FLAG_FULLSCREEN // esconde status bar
        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // esconde statusbar
        );
    }

    protected void criaGrafico(){
        //----------------- caracteristicas do grafico -----------------//

        // define o dominio fixo de 1 a 9
        grafico.setDomainBoundaries(0, 9, BoundaryMode.FIXED);
        // define a altura fixa de -7 a 7
        grafico.setRangeBoundaries(-7, 7, BoundaryMode.FIXED);

        grafico.setRangeStep(StepMode.SUBDIVIDE, 15);  // 15 linhas horizontais
        grafico.setDomainStep(StepMode.SUBDIVIDE, 10); // 10 linhas verticais

        // define como grafico rolavel para o lado (panoramico)
        PanZoom.attach(grafico, PanZoom.Pan.HORIZONTAL, PanZoom.Zoom.STRETCH_HORIZONTAL);
    }

    protected void appendLinha(int valor){
        // remove a linha antiga
        grafico.removeSeries(linha);

        // adiciona o valor recebido no array
        valores = append(valores, valor);

        // atualiza a linha com o valor do array
        linha = new SimpleXYSeries(Arrays.asList(valores),
                                        SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "Erro");

        // formata a aparecia da linha
        LineAndPointFormatter formatoLinha =
                new LineAndPointFormatter(Color.RED, Color.GREEN, null, null);

        // suaviza a linha
        formatoLinha.setInterpolationParams(
                new CatmullRomInterpolator.Params(10, CatmullRomInterpolator.Type.Centripetal));

        // adiciona a linha no grafico novamente
        grafico.addSeries(linha, formatoLinha);

        // redesenha as linhas
        grafico.redraw();

        if(!rola) { // deixa o grafico acompanhando a linha
            grafico.setDomainBoundaries(valores.length - 9, valores.length, BoundaryMode.FIXED);
        }
    }

    protected void plotaGraf(XYPlot grafico){
        // plota o grafico
        grafico.getGraph().getLineLabelStyle(XYGraphWidget.Edge.TOP).setFormat(new Format() {
            @Override
            public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
                int i = Math.round(((Number) obj).floatValue());
                return toAppendTo.append(1);
            }
            @Override
            public Object parseObject(String source, ParsePosition pos) {
                return null;
            }
        });
    }

    // soma mais um no array
    static <T> T[] append(T[] arr, T element) {
        final int N = arr.length;
        arr = Arrays.copyOf(arr, N + 1);
        arr[N] = element;
        return arr;
    }

    public class th extends Thread{// função q vai executar em segundo plano
        public void run() {

            int[] valores = {1, 2, 3, 4, 5, 6, 7, 6, 5, 4, 3, 2, 1, 0, -1, 2, -3};
            int a = 0;

            while (a < valores.length) {
                try {
                    sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                appendLinha(valores[a]);
                a++;

                if(a == valores.length){
                    a = 0;
                }

            }
        }
    }
}
