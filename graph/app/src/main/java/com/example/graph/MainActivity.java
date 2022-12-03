package com.example.graph;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.androidplot.util.PixelUtils;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.CatmullRomInterpolator;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PanZoom;
import com.androidplot.xy.PointLabelFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.StepMode;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Arrays;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    // botoes e cards
    public Boolean rola = true;     // salva se o grafico esta rolando ou nao
    public Button btnConectar;     // conexap bt
    public Button btnStop;        // copia o array pra area de trasferecencia
    public Button btnEnviar;        // envia as constantes pro carrinho
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    public Switch swtRolar;         // faz grafico acompanhar a linha
    public Button btnConf;          // abre as conf
    public Button btnX;             // fecha o cardConfig
    public Button btnLimpar;        // limpa o grafico
    public CardView cardConf;       // card das configurações
    public EditText edtKp;          // txt q tem os valores do Ke
    public EditText edtKd;          // txt q tem os valores do Kd
    public EditText edtKi;          // txt q tem os valores do Ki
    public EditText edtCte;         // tct q tem os valores da constante de curvas
    public EditText edtVel;         // txt q tem o valor da velocidade
    public EditText rangeImagem;    // aumenta o range da imagem do grafico

    // variaveis usadas no grafico
    public XYPlot grafico;          // grafico
    public XYSeries linha;          // linha
    public Number[] valores = {0};  // valores do grafico

    //variaveis usadas no bluetooth
    BluetoothAdapter adapBT = null;         // adaptador bluetooth
    BluetoothDevice devBT = null;           // dispositipo bluetooth
    BluetoothSocket socBT = null;           // entrada/canal bluetooth
    public boolean con = false;             // conectado ou nao
    static String endMac = null;            // endereço mac do dispositivo
    Handler manipulador = null;             // manipulador dos dados recebidos por bt
    // salva os dados recebidos por bt
    StringBuilder dadosRecebidosBT = new StringBuilder();

    /*maracutaia pra comunicação bt (todas os dispositivos devem ter esse UUID)
    cada dispositivo tem uma porta UUID diferente, então vai dar erro se tentar conectar com algo
    diferente (computador, tablet...)*/
    UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    // (cada request tem um int para sua identificação)
    public static final int REQ_BT = 1;     // requisição da ativação bt
    public static final int REQ_CON = 2;    // requisição para conexão bt
    private static MainActivity.conTh conTh;

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
        btnStop = findViewById(R.id.btnStop);
        edtKp = findViewById(R.id.txtKp);
        edtKd = findViewById(R.id.txtKd);
        edtKi = findViewById(R.id.txtKi);
        edtCte = findViewById(R.id.txtCte);
        btnConectar = findViewById(R.id.btnBlue);
        edtVel = findViewById(R.id.txtVelocidade);
        rangeImagem = findViewById(R.id.rangeImagem);

        // define como nao no swt de rolar o grafico
        swtRolar.isChecked();

        // define como invisivel as configuraçoes
        cardConf.setVisibility(View.INVISIBLE);
    }

    @SuppressLint({"MissingPermission", "HandlerLeak"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // tela q vai abrir
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // deixa status bar e nav bar black (n sei pintar pelo xml)
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);

        //inicia os componentes
        iniciaComponentes();

        // -------- inicia o grafico -------- //

        // cria o grafico
        criaGrafico();
        // plota o grafico
        plotaGraf(grafico);

        // -------- inicia o bluetooth -------- //

        adapBT = BluetoothAdapter.getDefaultAdapter();
        // verifica se o dispositivo possui bluetooth
        if (adapBT == null) {
            // dispositivo não suporta bluetooth
            Toast.makeText(this, "Seu dispositivo não possui Bluetooth!",
                    Toast.LENGTH_SHORT).show();
            // é preciso verifica pois se tentar ligar o bt sem ter bt crasha o app
        } else if (!adapBT.isEnabled()) { // verifica se o bluetooth está desligado
            // pede pra ligar o bluetooth
            // (aqui da um erro q n sei o pq, eu suprimi e funcionou ok)
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQ_BT);
        }

        //-----------------------------// botes //----------------------------//

        // abre a lista de dispositivos pareados ou desconecta
        btnConectar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // se não estiver conectado abre a lista de dispositivos
                if (con) {// se estiver conectado
                    try{// tenta desconectar
                        socBT.close();
                        Toast.makeText(MainActivity.this, "Desconectado!",
                                Toast.LENGTH_SHORT).show();
                        // define como desconectado
                        con = false;
                        btnConectar.setText("Bluetooth");
                    } catch (IOException er) {//mensagem de erro
                        Toast.makeText(MainActivity.this, "Erro ao desconectar!",
                                Toast.LENGTH_SHORT).show();
                    }
                } else {// senão abre uma lista com dispositivos pareados
                    Intent abreList = new Intent(MainActivity.this, Dispositivos.class);
                    startActivityForResult(abreList, REQ_CON);
                }
            }
        });

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

        // switch
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

        // enviar os dados
        btnEnviar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // {ke,kd,ki,velocidade}
                String texto =
                "{"+edtKp.getText()+"/"+ edtKi.getText()+"%"
                        +edtKd.getText()+"&"+ edtCte.getText()+"*"+edtVel.getText()+"}";

                // enviar por bluetooth
                if(con){
                    MainActivity.conTh.enviar(texto);
                }else{
                    Toast.makeText(MainActivity.this, "Desconectado!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // copia para area de trasferencia
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String texto =
                "{"+edtKp.getText()+"/"+ edtKi.getText()+"%"
                        +edtKd.getText()+"&"+ edtCte.getText()+"*0}";

                // enviar por bluetooth
                if(con){
                    MainActivity.conTh.enviar(texto);
                }else{
                    Toast.makeText(MainActivity.this, "Desconectado!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // limpa os dados do grafico
        btnLimpar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                valores = new Number[]{};
                appendLinha(0);
            }
        });

        btnX.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {cardConf.setVisibility(View.INVISIBLE);}});

        // aumenta o range da imagem do grafico
        rangeImagem.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                // verifica se n é valor nulo
                if(!String.valueOf(rangeImagem.getText()).equals("")) {
                    int range = Integer.parseInt(String.valueOf(rangeImagem.getText()));
                    grafico.setRangeBoundaries(-range, range, BoundaryMode.FIXED);
                }else{
                    grafico.setRangeBoundaries(-0, 0, BoundaryMode.FIXED);
                }

                grafico.redraw();
            }
        });

        //-----------------------------// recebe dados //----------------------------//

        // fica monitorando sozinho quando recebe mensagem
        manipulador = new Handler(){
            @Override
            public void handleMessage(@NonNull Message msg) {

                // se a mensagem ser "message_read" = 0
                if(msg.what == 0){
                    // transforma em string
                    String recebidos = (String) msg.obj;

                    // junta os dados enquanto esta recebendo algo
                    dadosRecebidosBT.append(recebidos);

                    // {dados} =  as chaves para saber se os dados vieram inteiros

                    if(dadosRecebidosBT.indexOf("}") > 0){
                        // verifica se o dado veio inteiro
                        if(dadosRecebidosBT.charAt(0) == '{'){

                            // tira as chaves
                            String dados = dadosRecebidosBT.substring(1, dadosRecebidosBT.indexOf("}"));

                            // converte pra int e add na linha
                            appendLinha(Float.parseFloat(dados));
                        }

                        // n botei mensagem de erro quando dado é inclompeto pq enchia o saco
                    }

                    // limpa a variavel
                    dadosRecebidosBT.delete(0, dadosRecebidosBT.length());
                }
            }
        };
    }

    protected void criaGrafico(){
        //----------------- caracteristicas do grafico -----------------//

        // define o dominio fixo de 1 a 9
        grafico.setDomainBoundaries(0, 9, BoundaryMode.FIXED);
        // define a altura fixo de 255 a 255
        grafico.setRangeBoundaries(-255, 255, BoundaryMode.FIXED);

        grafico.setRangeStep(StepMode.SUBDIVIDE, 15);  // 15 linhas horizontais
        grafico.setDomainStep(StepMode.SUBDIVIDE, 10); // 10 linhas verticais

        // define como grafico rolavel para o lado (panoramico)

        PanZoom.attach(grafico, PanZoom.Pan.HORIZONTAL, PanZoom.Zoom.STRETCH_HORIZONTAL);
    }

    protected void appendLinha(float valor){
        // remove a linha antiga
        grafico.removeSeries(linha);

        // adiciona o valor recebido no array
        valores = append(valores, valor);

        // atualiza a linha com o valor do array
        linha = new SimpleXYSeries(Arrays.asList(valores),
                                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "Dados recebidos");

        // formata a aparecia da linha
        LineAndPointFormatter formatoLinha =
                new LineAndPointFormatter(this, R.xml.estilo_da_linha);

        // suaviza a linha (deixa picos redondos)
        //formatoLinha.setInterpolationParams(
        //        new CatmullRomInterpolator.Params(10, CatmullRomInterpolator.Type.Centripetal));

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
        grafico.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).setFormat(new Format() {
            @Override
            public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
                int i = Math.round(((Number) obj).floatValue());
                return toAppendTo.append(i);
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

    @SuppressLint("MissingPermission")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {// verifica qual a requisição feita
            case REQ_BT:// caso for uma requisição para ligar o bt
                if (resultCode == Activity.RESULT_OK) {// se clicou ok
                } else {// se não deixou ligar o bt
                    Toast.makeText(this, "Erro ao ligar o Bluetooth!",
                            Toast.LENGTH_SHORT).show();
                }
                break;

            case REQ_CON:// caso seja requisição para conectar em um dispositivo
                if (resultCode == Activity.RESULT_OK) {// temos o endereço mac que retorna da lista
                    // salva o endereço mac q retornou
                    endMac = data.getExtras().getString(Dispositivos.endMac);

                    // buscamos o dispositivo bluetooth pelo seu endereço mac
                    devBT = adapBT.getRemoteDevice(endMac);

                    // criamos um canal de comunicação e começamos a enivar os dados
                    try {
                        // criamos um canal de comunicação com UUID
                        socBT = devBT.createInsecureRfcommSocketToServiceRecord(uuid);

                        // conecta com o dispostivo
                        socBT.connect();

                        // define os fluxos de entrada e saida de dados
                        conTh = new conTh(socBT);
                        conTh.start();

                        // mensagem
                        Toast.makeText(this, "Conectado com: " + endMac,
                                Toast.LENGTH_SHORT).show();

                        // define como conectado
                        con = true;
                        btnConectar.setText("Desconectar");
                    }catch (IOException er){// mensagem de erro
                        Toast.makeText(this, "Erro ao criar comunicação bluetooth!",
                                Toast.LENGTH_SHORT).show();

                        // define como desconectado
                        con = false;
                        btnConectar.setText("Bluetooth");
                    }
                }else{// nao conseguiu obeter o endereço mac
                    Toast.makeText(this, "Erro ao obeter endereço Mac!",
                            Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    // classe para enviar e receber dados pela porta/canal socket
    private class conTh extends Thread {
        private final InputStream mmInStream;// saida
        private final OutputStream mmOutStream;// entrada

        public conTh(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Obtem o fluxo de dados de entrada e saida
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Toast.makeText(MainActivity.this, "Erro no InputStream!",
                        Toast.LENGTH_SHORT).show();
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Toast.makeText(MainActivity.this, "Erro no OutputStream!",
                        Toast.LENGTH_SHORT).show();
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        // recebe os dados do bt
        // modificamos pq o metodo do google n detecta se a mensaagem veio quebrada
        public void run() {
            byte[] mmBuffer = new byte[1024];
            int numBytes; // bytes returned from read()

            while (true) {
                try {
                    // Read from the InputStream.
                    numBytes = mmInStream.read(mmBuffer);

                    // string do buffer recebido em butes
                    String dadosBT = new String(mmBuffer, 0, numBytes);

                    // isso aqui detecta a mensagem lida
                    manipulador.obtainMessage(0, numBytes, -1, dadosBT).sendToTarget();
                } catch (IOException e) {
                    // acho q crash se der erro
                    break;
                }
            }
        }


        // enviar dados
        public void enviar(String texto) {
            // converte String pra um vetor de bytes
            byte[] bytes = texto.getBytes();
            try{
                // fluxo de saida (envia os bytes)
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Toast.makeText(MainActivity.this, "Erro ao enviar dados!",
                        Toast.LENGTH_SHORT).show();
            }
        }


    }
}
