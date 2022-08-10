package com.example.graph;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

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

    public Boolean rola = true;     // salva se o grafico esta rolando ou nao
    public Button btnCopiar;        // copia o array pra area de trasferecencia
    public Button btnEnviar;        // envia as constantes pro carrinho
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    public Switch swtRolar;         // botao para o grafico acompanha a linha
    public Button btnConf;          // botao para abrir as conf
    public Button btnX;             // fecha o cardConfig
    public Button btnLimpar;        // botao para limpar o grafico
    public XYPlot grafico;          // grafico
    public XYSeries linha;          // linha
    public Number[] valores = {0};  // valores do grafico
    public CardView cardConf;       // card das configurações
    public EditText edtKe;          // txt q tem os valores do Ke
    public EditText edtKd;          // txt q tem os valores do Kd
    public EditText edtKi;          // txt q tem os valores do Ki

    @SuppressLint("WrongViewCast")
    public void iniciaComponentes(){
        // inicia os componenetes como elemento do xml
        grafico = findViewById(R.id.grafico);
        btnConf = findViewById(R.id.btnConf);
        btnEnviar = findViewById(R.id.btnEnviarCons);
        cardConf = findViewById(R.id.cardConf);
        swtRolar = findViewById(R.id.swtRol);
        btnLimpar = findViewById(R.id.btnLimpar);
        btnX = findViewById(R.id.fechar);
        btnCopiar = findViewById(R.id.btnCopiar);
        edtKe = findViewById(R.id.txtKe);
        edtKd = findViewById(R.id.txtKd);
        edtKi = findViewById(R.id.txtKi);

        // define como nao nao rolage
        swtRolar.isChecked();

        // define como invisivel as configuraçoes
        cardConf.setVisibility(View.INVISIBLE);
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
        btnConf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(cardConf.getVisibility() != View.VISIBLE) {
                    cardConf.setVisibility(View.VISIBLE);
                }else{
                    cardConf.setVisibility(View.INVISIBLE);
                }
            }
        });

        // sitch
        swtRolar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(swtRolar.isActivated()){
                    swtRolar.setActivated(false);
                    rola = true;
                }else{
                    swtRolar.setActivated(true);
                    rola = false;
                }
            }
        });

        btnEnviar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // {ke,kd,ki}
                String texto = "{"+edtKe.getText()+","+edtKd.getText()+","+edtKi.getText()+"}";

                // enviar por bluetooth
            }
        });

        btnCopiar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // manipulador da area de trasferencia
                ClipboardManager clipboard = (ClipboardManager)
                        getSystemService(Context.CLIPBOARD_SERVICE);

                // adiciona o dadosGrafico em algo "copiavel"
                ClipData clip = ClipData.newPlainText("simple text", copiaDados());

                // adiciona oq foi copiado na area de trasferencia
                clipboard.setPrimaryClip(clip);

                Toast.makeText(MainActivity.this,
                        "Copiado para a área de trasferencia!", Toast.LENGTH_SHORT).show();

            }
        });

        btnLimpar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                valores = new Number[]{0};
            }
        });

        btnX.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cardConf.setVisibility(View.INVISIBLE);
            }
        });

        new th().start();
    }

    protected String copiaDados(){

        /*
        * Botei \n pq se colar no excel ele divide em celulas diferentes
        * dai fica mais facil pra montar uma planhilha com os graficos
        * */

        String dadosGrafico = Arrays.toString(valores);                // converte array pra string
        dadosGrafico = dadosGrafico.substring(1, dadosGrafico.length()-1);           // retira os []
        dadosGrafico = dadosGrafico.replaceAll(",", "\n");    // trasforma firgula em \n

        // salva as constantes
        String constatnes = "Ke: "+edtKe.getText()+" Kd: "+edtKd.getText()+" Ki: "+edtKi.getText();

        // retorna as constantes mais o grafico
        return(constatnes + "\n" + dadosGrafico);

    }

    protected void hideSystemBars() {
        //deixa app em full screen
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
          View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY // define q mesmo se encostar na tela continua hide
        | View.SYSTEM_UI_FLAG_FULLSCREEN // esconde status bar
        //| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // esconde statusbar
        );
    }

    protected void criaGrafico(){
        //----------------- caracteristicas do grafico -----------------//

        // define o dominio fixo de 1 a 9
        grafico.setDomainBoundaries(0, 9, BoundaryMode.FIXED);
        // define a altura fixo de -7 a 7
        grafico.setRangeBoundaries(-7, 7, BoundaryMode.FIXED);
        // esses 2 de cima são meio bugados pq n respeita direito seila

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
                                        SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "Dados recebidos");

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

        if(rola) { // deixa o grafico acompanhando a linha
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

            int[] valores = {1, -1, 3, 2, 5, 7, 3, -2, -7, 4, 5, 7, 6, 0, -1, -2, -3};
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
