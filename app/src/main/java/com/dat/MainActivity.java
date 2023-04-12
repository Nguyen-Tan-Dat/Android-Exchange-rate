package com.dat;

import android.icu.text.NumberFormat;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    private Spinner spinnerFrom;
    private Spinner spinnerTo;
    private ProgressBar progressBar;
    private TextView textViewFrom;
    private TextView textViewTo;
    private Button btConVert;
    private TextView textView;
    private EditText editTextInput;
    private Button btTrain;

    private ArrayList<Currency> listCurrency = new ArrayList<>();
    private ArrayAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        initView();
        initEvent();
        new LoadSpinner().execute();
    }

    private class LoadSpinner extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
            progressBar.startAnimation(AnimationUtils.loadAnimation(MainActivity.this, android.R.anim.fade_in));
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            progressBar.setVisibility(View.GONE);
            progressBar.startAnimation(AnimationUtils.loadAnimation(MainActivity.this, android.R.anim.fade_out));
            btConVert.setVisibility(View.VISIBLE);
            adapter = new ArrayAdapter(MainActivity.this, android.R.layout.simple_list_item_1, listCurrency);
            spinnerFrom.setAdapter(adapter);
            spinnerTo.setAdapter(adapter);

            btConVert.setVisibility(View.VISIBLE);
            textView.setVisibility(View.VISIBLE);
            editTextInput.setVisibility(View.VISIBLE);
            btTrain.setVisibility(View.VISIBLE);
            textViewFrom.setVisibility(View.VISIBLE);
            textViewTo.setVisibility(View.VISIBLE);
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                URL url = new URL("https://all.fxexchangerate.com/rss.xml");
                Log.d("", url.toURI().toString());
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                org.w3c.dom.Document doc = db.parse(url.openStream());
                doc.getDocumentElement().normalize();
                NodeList nodeList = (NodeList) doc.getElementsByTagName("item");
                listCurrency = new ArrayList<>();
                for (int i = 0; i < nodeList.getLength(); i++) {
                    Node node = nodeList.item(i);
                    org.w3c.dom.Element fstElmnt = (org.w3c.dom.Element) node;
                    NodeList nameList = fstElmnt.getElementsByTagName("title");
                    org.w3c.dom.Element nameElement = (org.w3c.dom.Element) nameList.item(0);
                    String[] data = nameElement.getChildNodes().item(0).getNodeValue().split("/");
                    if (data.length > 1) {
                        String text = data[1];
                        Currency temp = new Currency(text.substring(text.length() - 4, text.length() - 1), text.substring(0, text.length() - 6));
                        listCurrency.add(temp);
                    }
                }
                listCurrency.sort(Comparator.comparing(Currency::getId));
            } catch (IOException | URISyntaxException | ParserConfigurationException | SAXException e) {
                e.printStackTrace();
            }

            return null;
        }
    }

    private void initView() {
        progressBar = findViewById(R.id.progressBar);
        btConVert = findViewById(R.id.buttonOK);
        textView = findViewById(R.id.textViewResult);
        spinnerFrom = findViewById(R.id.spinnerFrom);
        spinnerTo = findViewById(R.id.spinnerTo);
        editTextInput = findViewById(R.id.textInput);
        btTrain = findViewById(R.id.buttonChange);
        textViewFrom = findViewById(R.id.textViewFrom);
        textViewTo = findViewById(R.id.textViewTo);
    }

    private void initEvent() {
        btConVert.setOnClickListener(this::btConVertAction);
        spinnerFrom.setOnItemSelectedListener(this);
        spinnerTo.setOnItemSelectedListener(this);
        btTrain.setOnClickListener(this::btTrainAction);
        textView.setOnClickListener(this::twOutputAction);
    }

    private void twOutputAction(View view) {
        editTextInput.setText(textView.getText());
    }

    private void btTrainAction(View view) {
        int from = spinnerFrom.getSelectedItemPosition();
        int to = spinnerTo.getSelectedItemPosition();
        spinnerFrom.setSelection(to);
        spinnerTo.setSelection(from);
    }

    private void btConVertAction(View view) {
        double value = trainInput(editTextInput.getText().toString());
        if (value == Double.MIN_VALUE) {
            textView.setText("Input cannot be processed");
        } else {
            Currency currencyFrom = listCurrency.get(spinnerFrom.getSelectedItemPosition());
            Currency currencyTo = listCurrency.get(spinnerTo.getSelectedItemPosition());
            currencyFrom.setValue(value);
            new RateExchange(currencyFrom, currencyTo).execute();
        }
    }

    private double trainInput(String value) {
        double temp;
        try {
            temp = Double.parseDouble(value);
        } catch (Exception e) {
            temp = Double.MIN_VALUE;
        }
        return temp;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    private class RateExchange extends AsyncTask<Void, Void, Void> {
        private final Currency currencyFrom;
        private final Currency currencyTo;

        public RateExchange(Currency currencyFrom, Currency currencyTo) {
            this.currencyFrom = currencyFrom;
            this.currencyTo = currencyTo;
        }

        private String rate = "0";

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            textView.setText(rate);
        }


        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                Document document = Jsoup.connect("https://" + currencyFrom.getId() + ".fxexchangerate.com/rss.xml").get();
                Elements elements = document.getElementsByTag("description");
                for (Element element : elements) {
                    String[] data = element.text().split(" = ");
                    if (data.length > 1 && data[1].contains(currencyTo.getName())) {
                        double r = currencyFrom.getValue() * trainInput(data[1].split(" ")[0]) ;
                        Locale usa = new Locale("en", "US");
                        NumberFormat formatter = NumberFormat.getCurrencyInstance(usa);
                        rate  = formatter.format(r);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
